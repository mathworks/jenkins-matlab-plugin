# jenkins-matlab-plugin

## Description
The Jenkins plugin for MATLAB enables you to run your MATLAB tests and generate test artifacts in various formats including Junit, Tap, and Cobertura code coverage reports. 
## Build Step Configuration 
Selecting “Run MATLAB Tests” from the “Add build step” dropdown list will invoke this plugin.

![image](https://user-images.githubusercontent.com/47204011/54334421-019c7380-464c-11e9-91de-6d1c90043b08.png)


Use the MATLAB test runner plugin as part of the Jenkins build step to run MATLAB tests in two distinct modes:
* Run Tests Automatically 
* Use Custom MATLAB Command 
#### Configuration Steps to “Run Tests Automatically”
This option runs all MATLAB tests present in the current Jenkins workspace including subfolders. The automatic test running feature enables you to generate three different types of test artifacts once the tests have run to completion. The generated test artifacts could be used as part of a post-build action to publish the test results. Follow the steps below to configure the Jenkins build for running MATLAB tests automatically.
* Enter the value returned by “matlabroot” in the field named “MATLAB Root Folder”.

  ![image](https://user-images.githubusercontent.com/47204011/54334487-35779900-464c-11e9-8957-e6770c4609b4.png)

* Select the test run mode (Run Tests Automatically is the default mode).

  ![image](https://user-images.githubusercontent.com/47204011/54334496-39a3b680-464c-11e9-8d45-8798e6316ed8.png)

* Select any of the desired test artifacts. You can also choose not to generate any test artifact.

  ![image](https://user-images.githubusercontent.com/47204011/54334505-3f999780-464c-11e9-9a06-090779910db8.png)
  
  The selected test artifact(s) will be stored in the “results” folder of the Jenkins workspace.
  
  ![image](https://user-images.githubusercontent.com/47204011/54334869-5ab8d700-464d-11e9-9a52-3f494b349d0e.png)
  
* The automatic test run generates a MATLAB script file named “runMatlabTests.m” in the Jenkins workspace. The plugin uses this file to run tests and generate test artifacts. You are encouraged to review the MATLAB script to understand the test workflow.
 
  ![image](https://user-images.githubusercontent.com/47204011/54334812-20e7d080-464d-11e9-9d68-8e281d5a2801.png)


#### Configuration Steps to “Use Custom MATLAB Command”
This option enables you to develop your custom MATLAB commands for running tests. 
* Enter the value returned by “matlabroot”.  

  ![image](https://user-images.githubusercontent.com/47204011/54334561-6f489f80-464c-11e9-93a2-67e036b02830.png)
  
* From the dropdown list, select “Use Custom MATLAB command” as your test run mode.

  ![image](https://user-images.githubusercontent.com/47204011/54334568-72439000-464c-11e9-8e4e-5a9e1c66bb99.png)

* In “Enter Custom MATLAB Command,” enter your MATLAB command(s). Separate the  commands by semicolon. 

  ![image](https://user-images.githubusercontent.com/47204011/54334576-74a5ea00-464c-11e9-82fe-788f8b92cdcd.png)
  
  Note: If you require several MATLAB commands to execute your build, consider writing a MATLAB script and executing the script file       instead. Also, no test artifact can be automatically generated if you choose to run tests using your custom MATLAB commands.

  ![image](https://user-images.githubusercontent.com/47204011/54334581-77a0da80-464c-11e9-962d-1357f2f9df2c.png)
  
  
## Matrix Build Configuration 
Jenkins MATLAB plugin supports configuring the current job for matrix builds (i.e., multi-configuration projects). If you need Matrix build to run the same set of tests using different MATLAB versions installed on the system, follow these instructions.
* Create a multi-configuration project job.
  
  ![image](https://user-images.githubusercontent.com/47204011/54334963-93f14700-464d-11e9-8c88-a98565eba6fe.png)

* Add user-defined axis in the configuration page. The axis name can take any value but the axis value only accepts the MATLAB release folder name. This is the last section of the MATLAB root folder which starts with an R, e.g., R2015a.
  
  ![image](https://user-images.githubusercontent.com/47204011/54334603-838c9c80-464c-11e9-841f-4585d631b3ac.png)

* In the plugin, replace the last section of the MATLAB root folder with the user-defined axis name prefixed with '$' symbol as shown     below.

  ![image](https://user-images.githubusercontent.com/47204011/54335058-d155d480-464d-11e9-8af6-14285b3a8d33.png)

* Select other plugin options and save the job.

## Known Issues in the Current Version 
* With the “Use Custom MATLAB Command” option, the build will fail if the execution of any MATLAB command causes an error. 
* Multi-configuration project is supported only if MATLAB instances are in the same system as the current Jenkins instance. 
* In case of a multi-configuration project, the plugin UI displays error messages even if you replace the MATLAB root path with the axis name including a valid matlabroot path.
* The current version does not support running the job in slave systems as configured on Jenkins.


## Wiki Page 
<TBD>

## Contact Us 
If you have any questions or suggestions, please feel free to contact us.

Nikhil Bhoski: nikhil.bhoski@mathworks.com,
Mark Cafaro: mark.cafaro@mathworks.com,
Abhishek Kumar: akumar@mathworks.com,
Andy Campbell: andy.campbell@mathworks.com.


## License 
MIT © 1994-2019 The MathWorks, Inc.




| Overall  | Linux  | Windows  | Mac  | 
|---|---|---|---|
| [![Build Status](https://dev.azure.com/acampbel/acampbel/_apis/build/status/mathworks.jenkins-matlab-plugin?branchName=master)](https://dev.azure.com/acampbel/acampbel/_build/latest?definitionId=1&branchName=master)  | [![Build Status](https://dev.azure.com/acampbel/acampbel/_apis/build/status/mathworks.jenkins-matlab-plugin?branchName=master&jobName=Job&configuration=linux)](https://dev.azure.com/acampbel/acampbel/_build/latest?definitionId=1&branchName=master)  | [![Build Status](https://dev.azure.com/acampbel/acampbel/_apis/build/status/mathworks.jenkins-matlab-plugin?branchName=master&jobName=Job&configuration=windows)](https://dev.azure.com/acampbel/acampbel/_build/latest?definitionId=1&branchName=master)  | [![Build Status](https://dev.azure.com/acampbel/acampbel/_apis/build/status/mathworks.jenkins-matlab-plugin?branchName=master&jobName=Job&configuration=mac)](https://dev.azure.com/acampbel/acampbel/_build/latest?definitionId=1&branchName=master)  | 


