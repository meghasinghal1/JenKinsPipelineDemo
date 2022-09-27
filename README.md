# Offensive 360 for Azure DevOps Pipeline

Offensive 360 does deep source code analysis with one click. (We spent years building virtual compilers that understand the code, not only catching low hanging fruits vulnerabilities. We also claim that O360 can find security flaws that are even difficult for skilled application security testing consultants to find)

This section explains how to setup Offensive 360 scan step and its configuration as part of your CICD pipeline.

## Offensive 360 Scan Setup

1. Open the Microsoft Visual Studio Code application, navigate to the Extensions. 
![image](https://user-images.githubusercontent.com/13881466/179171511-3a12fea4-910c-431e-b929-88b44252d18e.png)


2. Search for **Offensive 360** and click on install
![image](https://user-images.githubusercontent.com/13881466/179386453-26f108b4-7869-4dac-9766-69cb8f27207f.png)


## Configuring Offensive 360 Settings
1. Click on `File menu => Preferences => Settings` to configure Offensive 360 settings if you are installing for first time or if you want to make any change to existing Offensive 360 settings.
![image](https://user-images.githubusercontent.com/13881466/179177844-bb73f6fe-6115-4840-a46c-9493f6bbb49f.png)

2. Then expend `Extensions` node and click on `Offensive 360 Settings`
![image](https://user-images.githubusercontent.com/13881466/179180063-ac05e749-ec93-4150-8ce9-c7093a30ccc8.png)

3. Enter Offensive 360 scan endpoint and access token information and close the Settins window
![image](https://user-images.githubusercontent.com/13881466/179180236-8c81eca0-b10e-43da-816f-b620700dde2e.png)


## How it works
1. Simply open any source file or project and right click on file or folder you will see respective context menu
![image](https://user-images.githubusercontent.com/13881466/179181906-37b252bf-2b5f-4ac1-863c-b814be0b8774.png)
![image](https://user-images.githubusercontent.com/13881466/179182052-4b8011f3-60d7-4515-9ebb-6a3643a08d9d.png)

> **Note** 
> When scanning the entire project, please select the "Scan Workspace" option for the best results instead of selecting "Scan Folder" on the root folder.

2. Click on respective context menu to scan a file or folder. You will see queued status on status bar that indicates your scan request got queued.
![image](https://user-images.githubusercontent.com/13881466/179186988-f968c6dc-c0f5-40b7-b04e-3de3f5ab7db3.png)

 3. After some time message on status bar will be updated to let you know whether your scan request is still in queued state or it turned into in-progress state. if it is still in queued, you will see queue position.
 ![image](https://user-images.githubusercontent.com/13881466/179187742-0e9b008c-2d8d-4c96-98b0-c5cbef2f7e8e.png)
 ![image](https://user-images.githubusercontent.com/13881466/183276079-823d9824-804f-453c-88b3-fa1ec7757921.png)

4. As soon as scanning is done, you will see vulnerabilities in IDE
![image](https://user-images.githubusercontent.com/13881466/179190467-f683aefa-0bbc-4939-a654-5f3ecd975d26.png)

5. By clicking on a vulnerability, you will be redirected to respective code file, line and column.
![image](https://user-images.githubusercontent.com/13881466/179195045-62fadc58-7bcb-44c3-a6ff-4f881185921f.png)

6. Mouse over on a vulnerability can help you in providing tooltip view to read the message easily.     
![image](https://user-images.githubusercontent.com/13881466/179195366-5ef41a06-8b06-4857-8e43-743659c842ac.png)
 
7. `Clear all Errors` on right click on a vulnerability will be appeared and help you in clearing all the errors from IDE
![image](https://user-images.githubusercontent.com/13881466/179200514-6947b5b6-cf94-417a-a52a-e2e60e532efd.png)
![image](https://user-images.githubusercontent.com/13881466/179200874-1d5106bb-290b-4b67-a1db-b06757308708.png)


8. `Get Help` on right click on a vulnerability will be appeared and help you in getting more details about a vulnerability
![image](https://user-images.githubusercontent.com/13881466/179200708-c796e7e8-db5d-4e64-b6b4-f6bab9747881.png)
![image](https://user-images.githubusercontent.com/13881466/179200804-21bc6707-6640-419d-8900-e6fec612eee3.png)

**Enjoy!!**
