import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.buildSteps.DotnetMsBuildStep
import jetbrains.buildServer.configs.kotlin.buildSteps.dotnetMsBuild
import jetbrains.buildServer.configs.kotlin.buildSteps.maven
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

    vcs {
        root(DslContext.settingsRoot)
    }

    steps {
        maven {
            enabled = false
            goals = "clean test"
            pomLocation = ".teamcity/pom.xml"
            runnerArgs = "-Dmaven.test.failure.ignore=true"
        }
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
        dotnetMsBuild {
            enabled = false
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
