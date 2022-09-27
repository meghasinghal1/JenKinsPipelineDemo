# Offensive 360 for Azure DevOps Pipeline

Offensive 360 does deep source code analysis with one click. (We spent years building virtual compilers that understand the code, not only catching low hanging fruits vulnerabilities. We also claim that O360 can find security flaws that are even difficult for skilled application security testing consultants to find)

This section explains how to setup Offensive 360 scan step and its configuration as part of your ADO CICD pipeline.

## Offensive 360 CICD Setup

1. Add ArchiveFile agent task as 1st CI step (before building) to compress your repo and create a zip file. 
```
steps:
- task: ArchiveFiles@2
  displayName: 'Archive $(Build.SourcesDirectory)'
  inputs:
    rootFolderOrFile: '$(Build.SourcesDirectory)'
    includeRootFolder: false
```

You can refer below screenshot to know right placement of the archive file step.
![image](https://user-images.githubusercontent.com/13881466/192559405-9e9e7b02-a7fb-4d40-817e-b6cf81c1a7c0.png)

2. Add PowerShell agent task as 2nd CI step to send the zipped file to Offensive 360 SAST API for scannng. 
```
steps:
- powershell: |
   Write-Host "Received scanning request successfully.."
   
   $buildId = $(Build.BuildId)
   $filePath = "$(Build.ArtifactStagingDirectory)/$(Build.BuildId).zip"
   $projectId = $null
   
   if("$(Offensive360SastApi_ProjectId)" -ne "")
   {
       $projectId = "$(Offensive360SastApi_ProjectId)"
   }
   
   $projectName = "ADO_Project_$buildId"
   $boundary = [System.Guid]::NewGuid().ToString()
   
   Write-Host "Starting scanning for the project name [$projectName], accessToken [$(Offensive360SastApi_AccessToken)], url [$(Offensive360SastApi_Url)], buildId [$buildId], filePath [$filePath], boundary [$boundary], projectId [$(Offensive360SastApi_ProjectId)], DeleteScanOutput [$(OFFENSIVE_DeleteScanOutput)]"
   
   $fileBytes = [System.IO.File]::ReadAllBytes("$filePath")
   $fileContent = [System.Text.Encoding]::GetEncoding('iso-8859-1').GetString($fileBytes)
   
   $LF = "`r`n"
   $bodyLines = (
       "--$boundary",
       "Content-Disposition: form-data; name=`"projectOrRepoName`"$LF",
       "$projectName",
       "--$boundary",
       "Content-Disposition: form-data; name=`"projectID`"$LF",
       "$projectId",
       "--$boundary",
       "Content-Disposition: form-data; name=`"deleteProjectAndScanAfterScanning`"$LF",
       "$(OFFENSIVE_DeleteScanOutput)",
       "--$boundary",
       "Content-Disposition: form-data; name=`"projectSource`"; filename=`"$projectName.zip`"",
       "Content-Type: application/x-zip-compressed$LF",
       $fileContent,
       "--$boundary--$LF"
   ) -join $LF
   
   $apiResponse = Invoke-RestMethod -Method Post -Uri $(Offensive360SastApi_Url) -ContentType "multipart/form-data; boundary=`"$boundary`"" -Headers @{"Accept" = "application/json"; "Authorization" = "Bearer $(Offensive360SastApi_AccessToken)"} -Body $bodyLines
   
   write-host ("total vulnerabilities count = ($apiResponse.vulnerabilities.length)")
   
   if ($apiResponse.vulnerabilities.length -gt 0 -and "$(ADO_BreakBuildWhenVulnsFound)" -eq 'True') 
   {
       throw [System.Exception] "Vulnerabilities found and breaking the build."
   }
   elseif ($apiResponse.vulnerabilities.length -gt 0 -and "$(ADO_BreakBuildWhenVulnsFound)" -ne 'True') 
   {
       Write-Warning 'Vulnerabilities found and since ADO_BreakBuildWhenVulnsFound is set to false so continuing to build it.'
   }
   else
   {
       Write-Warning 'No vulnerabilities found and continuing to build it.'
   }
   
   Write-Host "Finished SAST file scanning."
  displayName: 'PowerShell Script'
```

You can refer below screenshot to know right placement of the PowerShell step.
![image](https://user-images.githubusercontent.com/13881466/192562571-07c0746c-53a7-4f6f-8278-b7fd529389e5.png)

3. Add build or other agent tasks as next CI step(s) as needed to build your project and do other things. 
```
steps:
- task: VSBuild@1
  displayName: 'Build solution **\*.sln'
```

You can refer below screenshot to know right placement of the archive file step.
![image](https://user-images.githubusercontent.com/13881466/192563485-f6fb475f-8745-41b8-a115-77459a189534.png)


## Configuring Offensive 360 Settings
1. **Offensive360SastApi_Url (Mandatory)** : Offensive 360 SAST API scan url
```
Sample - http://<domainName>/app/api/ExternalScan/single-file
```
2. **Offensive360SastApi_AccessToken (Mandatory)** : Offensive 360 SAST API access token
```
Sample - eyJhbGci...
```
3. **Offensive360SastApi_ProjectId (Optional)** : This is Project Id to pass on to SAST API in case you want to scan your commit under existing project else a null value will be assigned.
```
Sample - 6331fc545360adc39ef5e080
```
4. **OFFENSIVE_DeleteScanOutput (Optional)** : This is Project Name to pass on to SAST API in case you want to scan your commit with your desired name else a random project name will be populated. This is how a random name will be populated GitLab_${CI_PROJECT_TITLE}Project${CI_JOB_ID}
```
Sample - True/False
```
5. **ADO_BreakBuildWhenVulnsFound (Optional)** : To break the build upon vulnerability found and value for this param is true else it will continue to build pipeline. default value is false.
```
Sample - True/False
```

## How to create/update variables in ADO 
1. Click on `Pipeline from left menu => Your pipeline => Edit => Variables` to configure Offensive 360 settings if you are setting up for first time or if you want to make any change to existing Offensive 360 settings.
![image](https://user-images.githubusercontent.com/13881466/192563774-38d3234e-5693-44e9-926b-213191fcec45.png)

2. Then click on `+` icon to add new pipeline variable
![image](https://user-images.githubusercontent.com/13881466/192569113-4920a78c-fce9-427d-8805-baf6405904d1.png)

3.Enter variable name and its value and click Ok
![image](https://user-images.githubusercontent.com/13881466/192569302-10270b64-594b-4508-b475-f9456e4fb3c7.png)

4. Click on existing variable to update value of it
![image](https://user-images.githubusercontent.com/13881466/192569594-94c90786-912b-4d25-9169-f83112f81a21.png)


## How it works
Your CI pipeline will stop execution from scanning step as soon as it found any vulnerability and remaining steps like build etc will get skipped.
![image](https://user-images.githubusercontent.com/13881466/192570312-fa880475-5242-4d95-bd96-11ac8184147c.png)

**Enjoy!!**
