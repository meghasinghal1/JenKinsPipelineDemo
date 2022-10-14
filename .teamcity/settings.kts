import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.buildSteps.DotnetMsBuildStep
import jetbrains.buildServer.configs.kotlin.buildSteps.dotnetMsBuild
import jetbrains.buildServer.configs.kotlin.buildSteps.powerShell
import jetbrains.buildServer.configs.kotlin.triggers.vcs

/*
The settings script is an entry point for defining a TeamCity
project hierarchy. The script should contain a single call to the
project() function with a Project instance or an init function as
an argument.

VcsRoots, BuildTypes, Templates, and subprojects can be
registered inside the project using the vcsRoot(), buildType(),
template(), and subProject() methods respectively.

To debug settings scripts in command-line, run the

    mvnDebug org.jetbrains.teamcity:teamcity-configs-maven-plugin:generate

command and attach your debugger to the port 8000.

To debug in IntelliJ Idea, open the 'Maven Projects' tool window (View
-> Tool Windows -> Maven Projects), find the generate task node
(Plugins -> teamcity-configs -> teamcity-configs:generate), the
'Debug' option is available in the context menu for the task.
*/

version = "2022.04"

project {

    buildType(Build)
}

object Build : BuildType({
    name = "Build"

    params {
        param("env.Offensive360SastApi_ProjectId", "")
        param("env.Offensive360SastApi_BaseUrl", "http://80.208.226.84:1800")
        password("env.Offensive360SastApi_AccessToken", "credentialsJSON:4634b0cb-2648-4747-ac02-42d326f93e2c")
        param("env.Offensive360SastUi_BaseUrl", "http://80.208.226.84")
        param("env.ADO_BreakBuildWhenVulnsFound", "True")
    }

    vcs {
        root(DslContext.settingsRoot)
    }

    steps {
        powerShell {
            name = "zipRepo"
            scriptMode = script {
                content = """
                    ${'$'}sourcePath = "%teamcity.build.checkoutDir%\\"
                    write-host "${'$'}sourcePath"
                    ${'$'}destinationPath = Split-Path -Path "${'$'}sourcePath"
                    ${'$'}destinationPath += "\%env.TEAMCITY_PROJECT_NAME%-%build.number%.zip"
                    Write-Host "${'$'}destinationPath"
                    
                    if(Test-Path -Path ${'$'}destinationPath -PathType Leaf)
                    {
                    	Remove-Item ${'$'}destinationPath
                    }
                    
                    Add-Type -Assembly 'System.IO.Compression.FileSystem'
                    ${'$'}zip = [System.IO.Compression.ZipFile]::Open(${'$'}destinationPath, 'create')
                    ${'$'}files = [IO.Directory]::GetFiles(${'$'}sourcePath, "*" , [IO.SearchOption]::AllDirectories)
                    foreach(${'$'}file in ${'$'}files)
                    {
                    	${'$'}relPath = ${'$'}file.Substring(${'$'}sourcePath.Length).Replace("\\", "/").Replace("\", "/")
                    	${'$'}a = [System.IO.Compression.ZipFileExtensions]::CreateEntryFromFile(${'$'}zip, ${'$'}file.Replace("\\", "/").Replace("\", "/"), ${'$'}relPath);
                    }
                    ${'$'}zip.Dispose()
                """.trimIndent()
            }
        }
        powerShell {
            name = "uploadZip"
            scriptMode = script {
                content = """
                    Write-Host "Received scanning request successfully.."
                    ${'$'}sourcePath = "%teamcity.build.checkoutDir%"
                    ${'$'}filePath = Split-Path -Path "${'$'}sourcePath"
                    ${'$'}filePath += "\%env.TEAMCITY_PROJECT_NAME%-%build.number%.zip"
                    Write-Host "${'$'}filePath"
                    
                    ${'$'}buildId = "%env.TEAMCITY_PROJECT_NAME%-%build.number%.zip"
                    ${'$'}projectId = ${'$'}null
                    
                    if("%env.Offensive360SastApi_ProjectId%" -ne "")
                    {
                    	${'$'}projectId = "%env.Offensive360SastApi_ProjectId%"
                    }
                    
                    ${'$'}projectName = "TeamCity_Project_${'$'}buildId"
                    ${'$'}boundary = [System.Guid]::NewGuid().ToString()
                    
                    Write-Host "Starting scanning for the project name [${'$'}projectName], accessToken [%env.Offensive360SastApi_AccessToken%], url [%env.Offensive360SastApi_BaseUrl%], buildId [${'$'}buildId], filePath [${'$'}filePath], boundary [${'$'}boundary], projectId [%env.Offensive360SastApi_ProjectId%], IsBreakBuildWhenVulnsFound [%env.ADO_BreakBuildWhenVulnsFound%]"
                    
                    ${'$'}fileBytes = [System.IO.File]::ReadAllBytes(${'$'}filePath)
                    ${'$'}fileContent = [System.Text.Encoding]::GetEncoding('iso-8859-1').GetString(${'$'}fileBytes)
                    
                    ${'$'}LF = "`r`n"
                    ${'$'}bodyLines = (
                        "--${'$'}boundary",
                        "Content-Disposition: form-data; name=`"projectOrRepoName`"${'$'}LF",
                        "${'$'}projectName",
                        "--${'$'}boundary",
                        "Content-Disposition: form-data; name=`"projectID`"${'$'}LF",
                        "${'$'}projectId",
                        "--${'$'}boundary",
                        "Content-Disposition: form-data; name=`"deleteProjectAndScanAfterScanning`"${'$'}LF",
                        "false",
                        "--${'$'}boundary",
                        "Content-Disposition: form-data; name=`"projectSource`"; filename=`"${'$'}projectName.zip`"",
                        "Content-Type: application/x-zip-compressed${'$'}LF",
                        ${'$'}fileContent,
                        "--${'$'}boundary--${'$'}LF"
                    ) -join ${'$'}LF
                    
                    ${'$'}apiResponse = Invoke-RestMethod -Method Post -Uri ("{0}/app/api/ExternalScan/single-file" -f "%env.Offensive360SastApi_BaseUrl%".TrimEnd('/')) -ContentType "multipart/form-data; boundary=`"${'$'}boundary`"" -Headers @{"Accept" = "application/json"; "Authorization" = "Bearer %env.Offensive360SastApi_AccessToken%"} -Body ${'$'}bodyLines
                    
                    write-host ("total vulnerabilities count = {0}" -f ${'$'}apiResponse.vulnerabilities.length)
                    
                    if (${'$'}apiResponse.vulnerabilities.length -gt 0 -and "%env.ADO_BreakBuildWhenVulnsFound%" -eq 'True') 
                    {
                        write-host "**********************************************************************************************************************"
                        write-host ("Offensive 360 vulnerability dashboard : {0}/Scan/showscan-{1}-{2}" -f "%env.Offensive360SastUi_BaseUrl%".TrimEnd('/'), ${'$'}apiResponse.projectId, ${'$'}apiResponse.id)
                        write-host "**********************************************************************************************************************"
                        throw [System.Exception] "Vulnerabilities found and breaking the build."
                    }
                    elseif (${'$'}apiResponse.vulnerabilities.length -gt 0 -and "%env.ADO_BreakBuildWhenVulnsFound%" -ne 'True') 
                    {
                    	Write-Warning 'Vulnerabilities found and since ADO_BreakBuildWhenVulnsFound is set to false so continuing to build it.'
                    }
                    else
                    {
                    	Write-Warning 'No vulnerabilities found and continuing to build it.'
                    }
                    
                    Write-Host "Finished SAST file scanning."
                    
                    if(Test-Path -Path ${'$'}filePath -PathType Leaf)
                    {
                    	Remove-Item ${'$'}filePath
                    }
                """.trimIndent()
            }
        }
        dotnetMsBuild {
            projects = "WebGoat.NET.sln"
            version = DotnetMsBuildStep.MSBuildVersion.V17
            args = "-restore -noLogo"
            sdk = "3.5"
        }
    }

    triggers {
        vcs {
        }
    }
})
