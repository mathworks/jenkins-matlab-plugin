# Jenkins MATLAB Plugin

## Description
The Jenkins plugin for MATLAB&reg; enables you to easily run your MATLAB tests and generate test artifacts in formats such as JUnit, TAP, and Cobertura code coverage reports. 
## Build Step Configuration
To invoke this plugin, select "Run MATLAB Tests" from the Add build step list.

  ![image](https://user-images.githubusercontent.com/47204011/54334421-019c7380-464c-11e9-91de-6d1c90043b08.png)
  
  ![plugin_default_ui](https://user-images.githubusercontent.com/47204011/55470538-73e9ed80-5625-11e9-942a-69d0bab44319.JPG)

Use the plugin as part of the Jenkins build step to run MATLAB tests in two distinct modes:
* Automatic
* Custom
#### Configuring “Automatic” Option
This option finds tests written using the MATLAB unit testing framework and/or Simulink test and runs them. If the code is organized using projects, it will locate all test files in the project that have been classified as "Test". If the code does not leverage projects or uses a MATLAB version prior to R2019a, the plugin will discover all tests in the current Jenkins workspace including subfolders. The automatic test running feature enables you to generate different types of test artifacts. They could be used with other Jenkins plugins as part of a post-build action to publish the test results. To configure the Jenkins build for running MATLAB tests automatically, follow these steps.
* Enter the value returned by “matlabroot” in the field named “MATLAB root”.

  ![Automatic_01](https://user-images.githubusercontent.com/47204011/55470446-456c1280-5625-11e9-8ee6-c8db51c79524.JPG)

* Select the Test mode to run tests (Automatic is the default mode).
* Select the desired test artifacts. You can also choose not to generate any test artifact.

  ![Automatic_02](https://user-images.githubusercontent.com/47204011/55470458-49983000-5625-11e9-80ab-54d21b97f39c.JPG)

  The selected test artifact(s) will be stored in the “matlabTestArtifacts” folder of the Jenkins workspace.

  ![Workspace01](https://user-images.githubusercontent.com/47204011/55470859-1e621080-5626-11e9-98f2-044144272643.JPG)
  
  ![Test_artifacts](https://user-images.githubusercontent.com/47204011/55470863-21f59780-5626-11e9-9765-4d79a6fd4061.JPG)

* The Automatic test run mode generates a MATLAB script file named “runMatlabTests.m” in the Jenkins workspace. The plugin uses this file to run tests and generate test artifacts. You are encouraged to review the MATLAB script to understand the test workflow.

  ![Workspace01](https://user-images.githubusercontent.com/47204011/55470859-1e621080-5626-11e9-98f2-044144272643.JPG)


#### Configuring “Custom” Option
This option enables you to develop your custom MATLAB commands for running tests.
* Enter the value returned by “matlabroot”.
* From the "Test mode" dropdown list, select “Custom command” option.

  ![Custom_01](https://user-images.githubusercontent.com/47204011/55471036-7dc02080-5626-11e9-97ea-eb28ba389dc6.JPG)

* In "MATLAB command” text box, enter your MATLAB command(s). Separate multiple commands by commas or semicolons.

  ![Custom_02](https://user-images.githubusercontent.com/47204011/55471100-9fb9a300-5626-11e9-8fef-fb86072e4784.JPG)

  Note: If you require several MATLAB commands to execute your test session, consider writing a MATLAB script or function as part of your repository and executing the script or function instead. Test artifacts are not auto generated if you choose to run tests using your custom MATLAB commands. You can generate these and other artifacts by configuring a test runner in the script or function invoked by the command. The build will fail if the execution of any MATLAB command causes an error.

  ![Custom_03](https://user-images.githubusercontent.com/47204011/55471106-a1836680-5626-11e9-90a1-1c383f27d908.JPG)


## Contact Us
If you have any questions or suggestions, please feel free to contact MathWorks.

support@mathworks.com

## License
MIT © 2019 The MathWorks, Inc.


## Build Results


| Overall  | Linux  | Windows  | Mac  |
|---|---|---|---|
| [![Build Status](https://dev.azure.com/iat-ci/jenkins-matlab-plugin/_apis/build/status/mathworks.jenkins-matlab-plugin?branchName=master)](https://dev.azure.com/iat-ci/jenkins-matlab-plugin/_build/latest?definitionId=6&branchName=master) |[![Build Status](https://dev.azure.com/iat-ci/jenkins-matlab-plugin/_apis/build/status/mathworks.jenkins-matlab-plugin?branchName=master&jobName=Job&configuration=linux)](https://dev.azure.com/iat-ci/jenkins-matlab-plugin/_build/latest?definitionId=6&branchName=master) |[![Build Status](https://dev.azure.com/iat-ci/jenkins-matlab-plugin/_apis/build/status/mathworks.jenkins-matlab-plugin?branchName=master&jobName=Job&configuration=windows)](https://dev.azure.com/iat-ci/jenkins-matlab-plugin/_build/latest?definitionId=6&branchName=master) |[![Build Status](https://dev.azure.com/iat-ci/jenkins-matlab-plugin/_apis/build/status/mathworks.jenkins-matlab-plugin?branchName=master&jobName=Job&configuration=mac)](https://dev.azure.com/iat-ci/jenkins-matlab-plugin/_build/latest?definitionId=6&branchName=master) |
