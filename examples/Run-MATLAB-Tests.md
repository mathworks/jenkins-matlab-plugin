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

* To run MATLAB tests and generate test artifacts, you must install the Jenkins plugin for MATLAB. For information on how to install a plugin in Jenkins, see [Managing Plugins](https://jenkins.io/doc/book/managing/plugins/).
* Jenkins runs the tests in a branch of the remote repository that is specified by a URL. To follow the build steps in this example, the Times Table App project must be under Git&trade; source control. For example, you can create a new repository for the project using your GitHub&reg; account. For more information, see [Use Source Control with Projects](https://www.mathworks.com/help/matlab/matlab_prog/use-source-control-with-projects.html).
* To publish the test artifacts using post-build actions, relevant plugins must be installed on Jenkins. For example, the [Cobertura plugin](https://plugins.jenkins.io/cobertura) integrates Cobertura coverage reports to Jenkins, and [JUnit plugin](https://plugins.jenkins.io/junit) publishes JUnit-format test results. 

## Configure Jenkins Project to Run MATLAB Tests
Create a new project and configure it by following these steps:
1. In your Jenkins interface, select **New Item** on the left. A new page opens where you can choose the type of your project. Enter a project name, then click **Freestyle project**. To confirm your choices, click **OK**.

![create_project](https://user-images.githubusercontent.com/48831250/71735513-a7f47800-2e1c-11ea-878b-db53c059c4c7.png)

2. In the configuration window of your Jenkins project, navigate to the **Source Code Management** section and click **Git**. This option enables Jenkins to interface with a remote repository.

![git](https://user-images.githubusercontent.com/48831250/71736215-809eaa80-2e1e-11ea-9ff5-6eea39622d3e.png)

3. Navigate to the main page of the GitHub repository that hosts your tests. Click **Clone or download** and copy the web URL to your clipboard.

![clipboard](https://user-images.githubusercontent.com/48831250/71736069-1980f600-2e1e-11ea-9672-1af6c958d77d.png)

4. In the Jenkins interface, paste the URL into the **Repository URL** box of the **Source Code Management** section. (You also can specify the branch to build in the **Branch Specifier** box.)

![source_control](https://user-images.githubusercontent.com/48831250/71735877-965fa000-2e1d-11ea-95c4-8b9259308e75.png)

5. In the **Build Environment** section of Jenkins, select **Use MATLAB Version**. Then, enter the full path to the MATLAB root folder in the **MATLAB root** box. Jenkins uses MATLAB at the specified location to run the tests. 

![build_environment](https://user-images.githubusercontent.com/48831250/76796506-f53c8080-67a1-11ea-860f-0cca3748c723.png)

6. In the **Build** section of Jenkins, select **Add build step > Run MATLAB Tests**. Then, select your desired test artifacts to be generated in the project workspace. The plugin in this example is configured to generate Cobertura code coverage and JUnit test result reports. For more information on how to configure the Jenkins plugin for MATLAB, see [Plugin Configuration Guide](../CONFIGDOC.md).

![build_step](https://user-images.githubusercontent.com/48831250/76796528-02f20600-67a2-11ea-9e40-9f10239db1f9.png)

7. In the **Post-build Actions** section of Jenkins, add two post-build actions to publish the Cobertura code coverage and JUnit test result reports. For each report, provide the path to the report file. Jenkins stores the artifacts in the **matlabTestArtifacts** folder of the workspace. 

![post_build](https://user-images.githubusercontent.com/48831250/76796543-0f765e80-67a2-11ea-98f6-8180ff85d4a0.png)

8. Click **Save** to save the project configuration settings. You can access and modify your settings at a later stage by selecting **Configure** in the project interface.

## Run Tests and Inspect Test Artifacts
To build your Jenkins project and run the tests specified in the repository, click **Build Now** in the project interface, which displays the project name at the top-left of the page. Jenkins triggers a build, assigns it a number under **Build History**, and runs the build. If the build is successful, a blue circle icon appears next to the build number. If the build fails, Jenkins adds a red circle icon. In this example, the build passes because all of the tests specified in the Times Table App project pass.

![build_1](https://user-images.githubusercontent.com/48831250/76796848-b0fdb000-67a2-11ea-8cec-753cf1eb27b2.png)

Navigate to the project workspace by clicking the **Workspace** icon in the project interface. You can view the generated test artifacts in the **matlabTestArtifacts** folder of the workspace.

![workspace](https://user-images.githubusercontent.com/48831250/76797316-a0016e80-67a3-11ea-9166-e95b5a4ac97d.png)

Access the published Cobertura code coverage report by opening the **Coverage Report** link in the project interface.

![cobertura_report](https://user-images.githubusercontent.com/48831250/76797272-85c79080-67a3-11ea-8a93-c9f92c66de5c.png)

To view the published JUnit test results, open the **Latest Test Result** link in the project interface. In the new page, open the link in the **All Tests** table. The table expands and lists information for each of the test classes within the Times Table App project.  

![junit_report](https://user-images.githubusercontent.com/48831250/76797445-e1921980-67a3-11ea-8ed4-157f7fd8bf77.png)

## See Also
[MathWorks Blogs: Developer Zone – Continuous Integration](https://blogs.mathworks.com/developer/category/continuous-integration/)<br/>
[matlab.unittest.plugins Package](https://www.mathworks.com/help/matlab/ref/matlab.unittest.plugins-package.html)<br/>
[Explore an Example Project (MATLAB)](https://www.mathworks.com/help/matlab/matlab_prog/explore-an-example-project.html)<br/>
[Use Source Control with Projects (MATLAB)](https://www.mathworks.com/help/matlab/matlab_prog/use-source-control-with-projects.html)
