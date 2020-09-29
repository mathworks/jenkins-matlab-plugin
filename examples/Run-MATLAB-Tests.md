# Run MATLAB Tests on Jenkins Server

This example shows how to run a suite of MATLAB&reg; unit tests with Jenkins&trade;. The example demonstrates how to:

* Configure Jenkins to run a freestyle project including MATLAB tests.
* Interface Jenkins with a remote repository that contains the tests.
* Build the freestyle project and examine the generated test artifacts.

The freestyle project runs the tests in the Times Table App MATLAB project (which requires R2019a or later). You can create a working copy of the project files and open the project in MATLAB by running this statement in the Command Window: 

```
matlab.project.example.timesTable
```

For more information about the Times Table App example project, see [Explore an Example Project](https://www.mathworks.com/help/matlab/matlab_prog/explore-an-example-project.html).

## Prerequisites
Running the tests in this example requires you to interface Jenkins with MATLAB as well as a remote repository.

* To run MATLAB tests and generate test artifacts, you must install the MATLAB plugin. For information on how to install a plugin in Jenkins, see [Managing Plugins](https://jenkins.io/doc/book/managing/plugins/).
* Jenkins runs the tests in a branch of the remote repository that is specified by a URL. To follow the build steps in this example, the Times Table App project must be under Git&trade; source control. For example, you can create a new repository for the project using your GitHub&reg; account. For more information, see [Use Source Control with Projects](https://www.mathworks.com/help/matlab/matlab_prog/use-source-control-with-projects.html).
* To publish the test artifacts using post-build actions, relevant plugins must be installed on Jenkins. For example, the [Cobertura plugin](https://plugins.jenkins.io/cobertura) integrates Cobertura coverage reports to Jenkins, and [JUnit plugin](https://plugins.jenkins.io/junit) publishes JUnit-format test results. 

## Configure Jenkins Project to Run MATLAB Tests
Create a new project and configure it by following these steps:
1. In your Jenkins interface, select **New Item** on the left. A new page opens where you can choose the type of your project. Enter a project name, then click **Freestyle project**. To confirm your choices, click **OK**.

![create_project](https://user-images.githubusercontent.com/48831250/94477040-50aee880-019f-11eb-9484-1d4ecf60ed92.png)

2. In the configuration window of your Jenkins project, navigate to the **Source Code Management** section and click **Git**. This option enables Jenkins to interface with a remote repository.

![git](https://user-images.githubusercontent.com/48831250/94477376-cb780380-019f-11eb-8297-1c3a4874fe0e.png)


3. Navigate to the main page of the GitHub repository that hosts your tests. Click **Code** and copy the web URL to your clipboard.

![clipboard](https://user-images.githubusercontent.com/48831250/94478137-dd0ddb00-01a0-11eb-9e55-d8004863c5e7.png)


4. In the Jenkins interface, paste the URL into the **Repository URL** box of the **Source Code Management** section. You also can specify the branch to build in the **Branch Specifier** box.

![source_control](https://user-images.githubusercontent.com/48831250/94478391-37a73700-01a1-11eb-9f89-a5a71413baf0.png)


5. In the **Build Environment** section of Jenkins, select **Use MATLAB Version**. Then, enter the full path to the MATLAB root folder in the **MATLAB root** box. Jenkins uses MATLAB at the specified location to run the tests. 

![build_environment](https://user-images.githubusercontent.com/48831250/94478737-ad130780-01a1-11eb-89d8-1ef43c34fab0.png)

6. In the **Build** section of Jenkins, select **Add build step > Run MATLAB Tests**. Then, select your desired test artifacts to be generated in the project workspace. The plugin in this example is configured to generate Cobertura code coverage and JUnit test result reports. Furthermore, the coverage report is generated only for the code in the `source` folder located in the root of your repository. For more information on how to configure the plugin, see [Plugin Configuration Guide](../CONFIGDOC.md).

![run_matlab_tests](https://user-images.githubusercontent.com/48831250/94479048-2f9bc700-01a2-11eb-9ff6-4ab1df99d1b9.png)

7. In the **Post-build Actions** section of Jenkins, add two post-build actions to publish the Cobertura code coverage and JUnit test result reports. For each report, provide the path to the report file.

![post_build](https://user-images.githubusercontent.com/48831250/94479293-96b97b80-01a2-11eb-97ff-44a321bce0e9.png)


8. Click **Save** to save the project configuration settings. You can access and modify your settings at a later stage by selecting **Configure** in the project interface.

## Run Tests and Inspect Test Artifacts
To build your Jenkins project and run the tests specified in the repository, click **Build Now** in the project interface, which displays the project name at the top-left of the page. Jenkins triggers a build, assigns it a number under **Build History**, and runs the build. If the build is successful, a blue circle icon appears next to the build number. If the build fails, Jenkins adds a red circle icon. In this example, the build passes because all of the tests specified in the Times Table App project pass.

![build_1](https://user-images.githubusercontent.com/48831250/94481160-4db6f680-01a5-11eb-83eb-75027a009321.png)

Navigate to the project workspace by clicking the **Workspace** icon in the project interface. In this example, the generated test artifacts are in the `matlabTestArtifacts` folder of the workspace.

![workspace](https://user-images.githubusercontent.com/48831250/94481319-88209380-01a5-11eb-8681-440ea84b59d9.png)

Access the published Cobertura code coverage report by opening the **Coverage Report** link in the project interface.

![cobertura_report](https://user-images.githubusercontent.com/48831250/94481556-e2b9ef80-01a5-11eb-86f2-1679ae63f407.png)

To view the published JUnit test results, open the **Latest Test Result** link in the project interface. In the new page, open the link in the **All Tests** table. The table expands and lists information for each of the test classes within the Times Table App project.  

![junit_report](https://user-images.githubusercontent.com/48831250/94481735-33c9e380-01a6-11eb-9106-d96cfd7676ae.png)

## See Also
[MathWorks Blogs: Developer Zone – Continuous Integration](https://blogs.mathworks.com/developer/category/continuous-integration/)<br/>
[matlab.unittest.plugins Package](https://www.mathworks.com/help/matlab/ref/matlab.unittest.plugins-package.html)<br/>
[Explore an Example Project (MATLAB)](https://www.mathworks.com/help/matlab/matlab_prog/explore-an-example-project.html)<br/>
[Use Source Control with Projects (MATLAB)](https://www.mathworks.com/help/matlab/matlab_prog/use-source-control-with-projects.html)
