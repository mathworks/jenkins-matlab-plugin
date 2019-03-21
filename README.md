# jenkins-matlab-plugin

## Description
The Jenkins plugin for MATLAB&copy; enables you to easily run your MATLAB tests and generate test artifacts in formats such as JUnit, TAP, and Cobertura code coverage reports. 
## Build Step Configuration
Selecting “Run MATLAB Tests” from the “Add build step” dropdown list will invoke this plugin.

![image](https://user-images.githubusercontent.com/47204011/54334421-019c7380-464c-11e9-91de-6d1c90043b08.png)


Use the plugin as part of the Jenkins build step to run MATLAB tests in two distinct modes:
* Running tests automatically
* Running tests using a custom command
#### Configuring “Run tests automatically”
This option finds tests using the MATLAB unit test framework and Simulink Test present and runs them. If the code is organized using projects, it will locate all test files in the project that have been classified as "Test". If the code does not leverage projects, or uses a MATLAB version prior to R2019a, the plugin will discover all tests in the current Jenkins workspace including subfolders. The automatic test running feature enables you to generate different types of test artifacts. The generated test artifacts could be used in conjunction with other Jenkins plugins as part of a post-build action to publish the test results. Follow the steps below to configure the Jenkins build for running MATLAB tests automatically.
* Enter the value returned by “matlabroot” in the field named “MATLAB Root Folder”.

  ![image](https://user-images.githubusercontent.com/47204011/54334487-35779900-464c-11e9-8957-e6770c4609b4.png)

* Select the method to run tests (Run tests automatically is the default mode).

  ![image](https://user-images.githubusercontent.com/47204011/54334496-39a3b680-464c-11e9-8d45-8798e6316ed8.png)

* Select any of the desired test artifacts. You can also choose not to generate any test artifact.

  ![image](https://user-images.githubusercontent.com/47204011/54334505-3f999780-464c-11e9-9a06-090779910db8.png)

  The selected test artifact(s) will be stored in the “matlab-results” folder of the Jenkins workspace.

  ![image](https://user-images.githubusercontent.com/47204011/54334869-5ab8d700-464d-11e9-9a52-3f494b349d0e.png)

* The automatic test run generates a MATLAB script file named “runMatlabTests.m” in the Jenkins workspace. The plugin uses this file to run tests and generate test artifacts. You are encouraged to review the MATLAB script to understand the test workflow.

  ![image](https://user-images.githubusercontent.com/47204011/54334812-20e7d080-464d-11e9-9d68-8e281d5a2801.png)


#### Configuration Steps to “Use Custom MATLAB Command”
This option enables you to develop your custom MATLAB commands for running tests.
* Enter the value returned by “matlabroot”.  

  ![image](https://user-images.githubusercontent.com/47204011/54334561-6f489f80-464c-11e9-93a2-67e036b02830.png)

* From the "Run tests" dropdown list, select “using custom command”.

  ![image](https://user-images.githubusercontent.com/47204011/54334568-72439000-464c-11e9-8e4e-5a9e1c66bb99.png)

* In "Custom MATLAB Command” enter your MATLAB command(s). Separate multiple commands by commas or semicolons.

  ![image](https://user-images.githubusercontent.com/47204011/54334576-74a5ea00-464c-11e9-82fe-788f8b92cdcd.png)

  Note: If you require several MATLAB commands to execute your test session, consider writing a MATLAB script or function as part of your repository and executing the script or function instead. No test artifacts are automatically generated if you choose to run tests using your custom MATLAB commands. You can generate these and other artifacts by configuring a test runner in the script or function invoked by the command. The build will fail if the execution of any MATLAB command causes an error.

  ![image](https://user-images.githubusercontent.com/47204011/54334581-77a0da80-464c-11e9-962d-1357f2f9df2c.png)

## Wiki Page
<TBD>

## Contact Us
If you have any questions or suggestions, please feel free to contact MathWorks.

support@mathworks.com

## License
MIT © 2019 The MathWorks, Inc.


## Build Results


| Overall  | Linux  | Windows  | Mac  |
|---|---|---|---|
| [![Build Status](https://dev.azure.com/iat-ci/jenkins-matlab-plugin/_apis/build/status/mathworks.jenkins-matlab-plugin?branchName=master)](https://dev.azure.com/iat-ci/jenkins-matlab-plugin/_build/latest?definitionId=6&branchName=master) |[![Build Status](https://dev.azure.com/iat-ci/jenkins-matlab-plugin/_apis/build/status/mathworks.jenkins-matlab-plugin?branchName=master&jobName=Job&configuration=linux)](https://dev.azure.com/iat-ci/jenkins-matlab-plugin/_build/latest?definitionId=6&branchName=master) |[![Build Status](https://dev.azure.com/iat-ci/jenkins-matlab-plugin/_apis/build/status/mathworks.jenkins-matlab-plugin?branchName=master&jobName=Job&configuration=windows)](https://dev.azure.com/iat-ci/jenkins-matlab-plugin/_build/latest?definitionId=6&branchName=master) |[![Build Status](https://dev.azure.com/iat-ci/jenkins-matlab-plugin/_apis/build/status/mathworks.jenkins-matlab-plugin?branchName=master&jobName=Job&configuration=mac)](https://dev.azure.com/iat-ci/jenkins-matlab-plugin/_build/latest?definitionId=6&branchName=master) |
