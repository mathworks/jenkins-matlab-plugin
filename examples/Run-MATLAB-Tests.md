# Run MATLAB Tests on Jenkins Server

This example shows how to run a suite of MATLAB&reg; unit tests with Jenkins&trade;. The example demonstrates how to:

* Configure Jenkins to run a freestyle project including MATLAB tests.
* Interface Jenkins with a remote repository that contains the tests.
* Build the freestyle project and examine the generated artifacts.

The freestyle project runs the tests in the Times Table App MATLAB project (which requires R2019a or later). You can create a working copy of the project files and open the project in MATLAB by running this statement in the Command Window.

```
matlab.project.example.timesTable
```

For more information about the Times Table App example project, see [Explore an Example Project](https://www.mathworks.com/help/matlab/matlab_prog/explore-an-example-project.html).

## Prerequisites
Running the tests in this example requires you to interface Jenkins with MATLAB as well as a remote repository.

* To run MATLAB tests and generate artifacts, you must install the plugin for MATLAB. For information on how to install a plugin in Jenkins, see [Managing Plugins](https://jenkins.io/doc/book/managing/plugins/).
* Jenkins runs the tests in a branch of the remote repository that is specified by a URL. To follow the build steps in this example, the Times Table App project must be under Git&trade; source control. For example, you can create a new repository for the project using your GitHub&reg; account. For more information, see [Use Source Control with Projects](https://www.mathworks.com/help/matlab/matlab_prog/use-source-control-with-projects.html).
* To publish the artifacts using post-build actions, relevant plugins must be installed on Jenkins. For example, the [Cobertura plugin](https://plugins.jenkins.io/cobertura) integrates Cobertura coverage reports to Jenkins, and [JUnit plugin](https://plugins.jenkins.io/junit) publishes JUnit-format test results. 

## Configure Jenkins Project to Run MATLAB Tests
Create a new project and configure it by following these steps:
1. In your Jenkins interface, select **New Item** on the left. A new page opens where you can choose the type of your project. Enter a project name, then click **Freestyle project**. To confirm your choices, click **OK**.

![create_project](https://user-images.githubusercontent.com/48831250/105080784-520e6480-5a5f-11eb-9218-4d43013e2850.png)

2. In the configuration window of your Jenkins project, navigate to the **Source Code Management** section and click **Git**. This option enables Jenkins to interface with a remote repository.

3. Navigate to the main page of the GitHub repository that hosts your tests. Click **Code** and copy the web URL to your clipboard.

![clipboard](https://user-images.githubusercontent.com/48831250/94478137-dd0ddb00-01a0-11eb-9e55-d8004863c5e7.png)


4. In the project configuration window, paste the URL into the **Repository URL** box of the **Source Code Management** section. You also can specify the branch to build in the **Branch Specifier** box.

![source_control](https://user-images.githubusercontent.com/48831250/94478391-37a73700-01a1-11eb-9f89-a5a71413baf0.png)


5. In the **Build Environment** section, select **Use MATLAB version** and specify the MATLAB version you want to use in the build. If your desired MATLAB version is not listed under **Use MATLAB version**, enter the full path to its root folder in the **MATLAB root** box. 

![build_environment](https://user-images.githubusercontent.com/48831250/105091260-943ea280-5a6d-11eb-8d11-48747df7ec32.png)

6. In the **Build** section, select **Add build step > Run MATLAB Tests**. Then, select your desired artifacts to be generated in the project workspace. The plugin in this example is configured to generate Cobertura code coverage and JUnit test result reports. Furthermore, the coverage report is generated only for the code in the `source` folder located in the root of your repository. For more information on how to configure the plugin, see [Plugin Configuration Guide](../CONFIGDOC.md).

![run_matlab_tests](https://user-images.githubusercontent.com/48831250/105909903-2149a480-5ff6-11eb-81f1-c3b44e9b17d1.png)

7. In the **Post-build Actions** section, add two post-build actions to publish the Cobertura code coverage and JUnit test result reports. For each report, provide the path to the report file.

![post_build](https://user-images.githubusercontent.com/48831250/105082096-14aad680-5a61-11eb-9868-68d018199f9d.png)

8. Click **Save** to save the project configuration settings. You can access and modify your settings at a later stage by selecting **Configure** in the project interface.

## Run Tests and Inspect Artifacts
To build your freestyle project, click **Build Now** in the project interface, which displays the project name at the top-left of the page. Jenkins triggers a build, assigns it a number under **Build History**, and runs the build. If the build is successful, a blue circle icon appears next to the build number. If the build fails, Jenkins adds a red circle icon. In this example, the build passes because all of the tests in the Times Table App project pass.

![build_1](https://user-images.githubusercontent.com/48831250/105084788-dca59280-5a64-11eb-858d-664a5727a947.png)

Navigate to the project workspace by clicking the **Workspace** icon in the project interface. In this example, the generated artifacts are in the `matlabTestArtifacts` folder of the workspace.

![workspace](https://user-images.githubusercontent.com/48831250/105085419-b46a6380-5a65-11eb-8d46-747dd23291bf.png)

Access the published Cobertura code coverage report by opening the **Coverage Report** link in the project interface.

![cobertura_report](https://user-images.githubusercontent.com/48831250/105085331-9ac91c00-5a65-11eb-9628-efbf70520489.png)

To view the published JUnit test results, open the **Latest Test Result** link in the project interface. In the new page, open the link in the **All Tests** table. The table expands and lists information for each of the test classes within the Times Table App project.  

![junit_report](https://user-images.githubusercontent.com/48831250/105088211-956dd080-5a69-11eb-931c-aef201eb9dbe.png)

## See Also
[MathWorks Blogs: Developer Zone – Continuous Integration](https://blogs.mathworks.com/developer/category/continuous-integration/)<br/>
[matlab.unittest.plugins Package](https://www.mathworks.com/help/matlab/ref/matlab.unittest.plugins-package.html)<br/>
[Explore an Example Project (MATLAB)](https://www.mathworks.com/help/matlab/matlab_prog/explore-an-example-project.html)<br/>
[Use Source Control with Projects (MATLAB)](https://www.mathworks.com/help/matlab/matlab_prog/use-source-control-with-projects.html)
