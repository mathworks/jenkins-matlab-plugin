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
* Jenkins runs the tests in a branch of the remote repository that is specified by a URL. To follow the build steps in this example, the *Times Table App* project must be under Git&trade; source control. For example, you can create a new repository for the project using your GitHub&reg; account. For more information, see [Use Source Control with Projects](https://www.mathworks.com/help/matlab/matlab_prog/use-source-control-with-projects.html).
* To publish the test artifacts using post-build actions, relevant plugins must be installed on Jenkins. For example, the [Cobertura plugin](https://plugins.jenkins.io/cobertura) integrates Cobertura coverage reports to Jenkins, and [JUnit plugin](https://plugins.jenkins.io/junit) enables JUnit-format test results to be published. 

## Configure Jenkins Project to Run MATLAB Tests
Create a new project and configure it by following these steps:
1. In your Jenkins interface, select **New Item** on the left. A new page opens where you can choose the type of your project. Enter a project name, then click **Freestyle project**. To confirm your choices, click **OK**.

![create_project](https://user-images.githubusercontent.com/48831250/71735513-a7f47800-2e1c-11ea-878b-db53c059c4c7.png)

2. In the configuration window of your Jenkins project, navigate to the **Source Code Management** section and click **Git**. This option enables Jenkins to interface with a remote repository.

![git](https://user-images.githubusercontent.com/48831250/71736215-809eaa80-2e1e-11ea-9ff5-6eea39622d3e.png)

3. Navigate to the main page of the GitHub repository that hosts your tests. Click **Clone or download** and copy the web URL to your clipboard.

![clipboard](https://user-images.githubusercontent.com/48831250/71736069-1980f600-2e1e-11ea-9672-1af6c958d77d.png)

4. In the Jenkins interface, paste the URL into the **Repository URL** field of the **Source Code Management** section. (You also can specify the branch to build in the **Branch Specifier** field.)

![source_control](https://user-images.githubusercontent.com/48831250/71735877-965fa000-2e1d-11ea-95c4-8b9259308e75.png)

5. In the **Build** section of Jenkins, click **Add build step > Run MATLAB Tests**. A new window opens that enables you to configure the Jenkins plugin for MATLAB. Paste the full path to the MATLAB root folder into the **MATLAB root** field; choose the default Automatic test mode from the **Test mode** drop-down list; and select your desired test artifacts to be generated in the project workspace. (The plugin in this example is configured to generate Cobertura code coverage and JUnit test result reports.) For more information on how to configure the Jenkins plugin for MATLAB, see [Jenkins MATLAB Plugin](https://github.com/jenkinsci/matlab-plugin).

![configure_plugin](https://user-images.githubusercontent.com/48831250/71839614-94017e00-3089-11ea-8964-f4557a54749c.png)

6. In the **Post-build Actions** section of Jenkins, add two post-build actions to publish the Cobertura code coverage and JUnit test result reports that are generated. For each report, provide the path to the report file. Jenkins stores the artifacts in the *matlabTestArtifacts* folder of the workspace. 

![post-build](https://user-images.githubusercontent.com/48831250/71736813-0c650680-2e20-11ea-8fa5-dee1bb6ddda0.png)

7. Click **Save** to save the project configuration settings. You can access and modify your settings at a later stage by selecting **Configure** in the project interface.

## Run Tests and Inspect Test Artifacts
To build your Jenkins project and run the tests specified in the repository, click **Build Now** in the project interface, which displays the project name at the top-left of the page. Jenkins triggers a build, assigns it a number under **Build History**, and runs the build. If the build is successful, a blue circle icon appears next to the build number. If the build fails, Jenkins adds a red circle icon. In this example, the build passes because all of the tests specified in the *Times Table App* project pass.

![build_1](https://user-images.githubusercontent.com/48831250/71737052-9b721e80-2e20-11ea-9e1d-ed82e3f57484.png)

Navigate to the project workspace by clicking the **Workspace** icon in the project interface. You can view the generated test artifacts in the *matlabTestArtifacts* folder of the workspace.

![workspace](https://user-images.githubusercontent.com/48831250/71737151-e68c3180-2e20-11ea-80d5-368deb6d3e1e.png)

Access the published Cobertura code coverage report by opening the *Coverage Report* link in the project interface.

![cobertura_report](https://user-images.githubusercontent.com/48831250/71737280-3c60d980-2e21-11ea-8891-aff1b794243d.png)

To view the published JUnit test results, open the *Latest Test Result* link in the project interface. In the new page, open the link in the *All Tests* table. The table expands and lists information for each of the test classes within the *Times Table App* project.  

![junit_report](https://user-images.githubusercontent.com/48831250/71737382-7e8a1b00-2e21-11ea-8133-fe5b2f198bd9.png)





## See Also
[MathWorks Blogs: Developer Zone – Continuous Integration](https://blogs.mathworks.com/developer/category/continuous-integration/)<br/>
[matlab.unittest.plugins Package](https://www.mathworks.com/help/matlab/ref/matlab.unittest.plugins-package.html)<br/>
[Explore an Example Project (MATLAB)](https://www.mathworks.com/help/matlab/matlab_prog/explore-an-example-project.html)<br/>
[Use Source Control with Projects (MATLAB)](https://www.mathworks.com/help/matlab/matlab_prog/use-source-control-with-projects.html)
