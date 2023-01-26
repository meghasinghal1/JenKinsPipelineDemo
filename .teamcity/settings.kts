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
        password("env.Offensive360SastApi_AccessToken", "credentialsJSON:4634b0cb-2648-4747-ac02-42d326f93e2c")
        param("env.Offensive360SastApi_BaseUrl", "http://80.208.226.84:1800")
        param("env.Offensive360SastUi_BaseUrl", "http://80.208.226.84")
        param("env.ADO_BreakBuildWhenVulnsFound", "True")
        param("env.Offensive360SastApi_AllowDependencyScan", "True")
        param("env.Offensive360SastApi_AllowMalwareScan", "True")
        param("env.Offensive360SastApi_AllowLicenseScan", "True")
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
                    
                    ${'$'}selfUrl = "%teamcity.serverUrl%/buildConfiguration/%system.teamcity.buildType.id%/%teamcity.build.id%?buildTypeTab=overview&hideProblemsFromDependencies=false&hideTestsFromDependencies=false&buildTab=log&focusLine=0&logView=flowAware"  
                    Write-Host "${'$'}selfUrl"
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
                    
                    Write-Host "Starting scanning for the project name [${'$'}projectName], accessToken [%env.Offensive360SastApi_AccessToken%], url [%env.Offensive360SastApi_BaseUrl%], buildId [${'$'}buildId], filePath [${'$'}filePath], boundary [${'$'}boundary], projectId [%env.Offensive360SastApi_ProjectId%], IsBreakBuildWhenVulnsFound [%env.ADO_BreakBuildWhenVulnsFound%], Offensive360SastApi_AllowDependencyScan [%env.Offensive360SastApi_AllowDependencyScan%], Offensive360SastApi_AllowMalwareScan [%env.Offensive360SastApi_AllowMalwareScan%], Offensive360SastApi_AllowLicenseScan [%env.Offensive360SastApi_AllowLicenseScan%]"
                    
                    ${'$'}fileBytes = [System.IO.File]::ReadAllBytes(${'$'}filePath)
                    ${'$'}fileContent = [System.Text.Encoding]::GetEncoding('iso-8859-1').GetString(${'$'}fileBytes)
                    
                    ${'$'}LF = "`r`n"
                    ${'$'}bodyLines = (
                        "--${'$'}boundary",
                        "Content-Disposition: form-data; name=`"name`"${'$'}LF",
                        "${'$'}projectName",
                        "--${'$'}boundary",
                        "Content-Disposition: form-data; name=`"projectId`"${'$'}LF",
                        "${'$'}projectId",
                        "--${'$'}boundary",
                        "Content-Disposition: form-data; name=`"keepInvisibleAndDeletePostScan`"${'$'}LF",
                        "false",
                        "--${'$'}boundary",
                        "Content-Disposition: form-data; name=`"allowDependencyScan`"${'$'}LF",
                        "%env.Offensive360SastApi_AllowDependencyScan%",
                         "--${'$'}boundary",
                        "Content-Disposition: form-data; name=`"allowMalwareScan`"${'$'}LF",
                        "%env.Offensive360SastApi_AllowMalwareScan%",
                         "--${'$'}boundary",
                        "Content-Disposition: form-data; name=`"allowLicenseScan`"${'$'}LF",
                        "%env.Offensive360SastApi_AllowLicenseScan%",
                        "--${'$'}boundary",
                        "Content-Disposition: form-data; name=`"externalScanSourceType`"${'$'}LF",
                        "TeamCity",
                        "--${'$'}boundary",
                        "Content-Disposition: form-data; name=`"fileSource`"; filename=`"${'$'}projectName.zip`"",
                        "Content-Type: application/x-zip-compressed${'$'}LF",
                        ${'$'}fileContent,
                        "--${'$'}boundary--${'$'}LF"
                    ) -join ${'$'}LF
                    
                    ${'$'}apiResponse = Invoke-RestMethod -Method Post -Uri ("{0}/app/api/externalScan" -f "%env.Offensive360SastApi_BaseUrl%".TrimEnd('/')) -ContentType "multipart/form-data; boundary=`"${'$'}boundary`"" -Headers @{"Accept" = "application/json"; "Authorization" = "Bearer %env.Offensive360SastApi_AccessToken%"} -Body ${'$'}bodyLines
                    
                    write-host ("total vulnerabilities count = {0}" -f ${'$'}apiResponse.vulnerabilities.length)
                    write-host ("Total Vulnerabilities Count : {0}" -f ${'$'}apiResponse.vulnerabilities.length)
                    write-host ("Total Malwares Count : {0}" -f ${'$'}apiResponse.malwares.length)
                    write-host ("Total Licenses Count : {0}" -f ${'$'}apiResponse.licenses.length)
                    write-host ("Total Dependency Vulnerabilities Count : {0}" -f ${'$'}apiResponse.dependencyVulnerabilities.length)
                    
                    if ((${'$'}apiResponse.vulnerabilities.length -gt 0 -or ${'$'}apiResponse.malwares.length -gt 0 -or ${'$'}apiResponse.licenses.length -gt 0 -or ${'$'}apiResponse.dependencyVulnerabilities.length -gt 0) -and "%env.ADO_BreakBuildWhenVulnsFound%" -eq 'True') 
                    {
                        write-host "**********************************************************************************************************************"
                        write-host ("Offensive 360 vulnerability dashboard : {0}/scan/{1}" -f "%env.Offensive360SastUi_BaseUrl%".TrimEnd('/'), ${'$'}apiResponse.projectId)
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
