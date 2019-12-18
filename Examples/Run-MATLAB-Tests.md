# Run MATLAB Tests on Jenkins Server

This example shows you how to run a suite of MATLAB&reg; unit tests with Jenkins&trade;. The example enables you to:

* Configure Jenkins to run a freestyle project including MATLAB tests.
* Interface Jenkins with a remote repository that contains the tests.
* Build the freestyle project and examine the generated test artifacts.

The project will run the tests in the *Times Table App* MATLAB project (requires R2019a or later). You can create a working copy of the project files and open the project in MATLAB by running the following statement in the Command Window. 

```
matlab.project.example.timesTable
```

For more information about the *Times Table App* example project, see [Explore an Example Project](https://www.mathworks.com/help/matlab/matlab_prog/explore-an-example-project.html).

## Prerequisites
Running the tests in this example requires you to interface Jenkins with MATLAB as well as a remote repository.

* To run MATLAB tests and generate test artifacts, you must install the Jenkins plugin for MATLAB. For information on how to install a plugin in Jenkins, see [Managing Plugins](https://jenkins.io/doc/book/managing/plugins/).
* Jenkins runs the tests in a branch of the remote repository that is specified by a URL. To follow the build configuration steps, you must upload the *Times Table App* example project to a remote GitHub&reg; repository.
* To publish the test artifacts using post-build actions, relevant plugins must be installed on Jenkins. For example, the *Cobertura Plugin* integrates Cobertura coverage reports to Jenkins, and *JUnit plugin* enables JUnit-format test results to be published. 

## Configure Jenkins to Run MATLAB Tests
Configure the Jenkins build by following these steps:
1. In your Jenkins interface, select **New Item** on the left. A new page opens where you can choose the type of your project. Enter a project name, then click **Freestyle project**. To confirm your choices, click **OK**.
2. In the configuration window of your Jenkins project, navigate to the **Source Code Management** section and click **Git**. This option enables Jenkins to interface with a remote repository.
3. Navigate to the main page of the GitHub repository that hosts your tests. Click **Clone or download** and copy the web URL to your clipboard.
4. In the Jenkins interface, paste the URL into the **Repository URL** field of the **Source Code Management** section. (You also can specify the branch to build in the **Branch Specifier** field.)
5. In the **Build** section of Jenkins, click **Add build step > Run MATLAB Tests**. A new window opens that enables you to configure the Jenkins plugin for MATLAB. Paste the full path to the MATLAB root folder into the **MATLAB root** field; choose the default Automatic test mode from the **Test mode** drop-down list; and select your desired test artifacts to be generated in the project wrokspace. (The build in this example is configured to generate Cobertura code coverage and JUnit test result reports.) For more information on how to configure the Jenkins plugin for MATLAB, see [Jenkins MATLAB Plugin](https://github.com/jenkinsci/matlab-plugin).
6. In the **Post-build Actions** section of Jenkins, add two post-build actions to publish the Cobertura code coverage and JUnit test result reports that the build generates. For each report, provide the path to the report file. Jenkins stores the artifacts in the *matlabTestArtifacts* folder of the workspace. 

![post-build](https://user-images.githubusercontent.com/48831250/71104139-534cce00-2189-11ea-9f59-1f4d9eee99f8.png)

7. Click **Save** to save the project configuration settings. You can access and modify your settings at a later stage by selecting **Configure** in the project interface.

## Run Tests in Freestyle Project
To build your Jenkins project and run the tests specified in the repository, click **Build now** in the project interface, which displays the project name at the top-left of the page. Jenkins triggers a build, assigns it a number under **Build History**, and runs the build. If the build is successful, a blue circle icon appears next to the build number. If the build fails, Jenkins adds a red circle icon. In this example, the build passes because all of the tests specified in the *Times Table App* project pass.

![build_1](https://user-images.githubusercontent.com/48831250/71103438-221fce00-2188-11ea-8d17-3793b3964d04.png)

Navigate to the project workspace by clicking the **Workspace** icon in the project interface. You can view the generated test artifacts in the *matlabTestArtifacts* folder of the workspace.

![workspace](https://user-images.githubusercontent.com/48831250/71103834-ce61b480-2188-11ea-84a8-b2bfbea9b106.png)




![cobertura_report](https://user-images.githubusercontent.com/48831250/71103566-598e7a80-2188-11ea-9bc5-857420ca1fce.png)
![junit_report](https://user-images.githubusercontent.com/48831250/71103594-63b07900-2188-11ea-9fa4-253cba446de7.png)





## See Also
[MathWorks Blogs: Developer Zone](https://blogs.mathworks.com/developer/category/continuous-integration/)<br/>
[matlab.unittest.plugins Package](https://www.mathworks.com/help/matlab/ref/matlab.unittest.plugins-package.html)<br/>
[Explore an Example Project](https://www.mathworks.com/help/matlab/matlab_prog/explore-an-example-project.html)<br/>
[Source Control Integration](https://www.mathworks.com/help/matlab/source-control.html)
