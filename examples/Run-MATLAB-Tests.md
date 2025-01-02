# Run MATLAB Tests on Jenkins Server
This example shows how to run a suite of MATLAB&reg; unit tests with Jenkins&trade;. The example demonstrates how to:

* Configure a freestyle project to access MATLAB tests hosted in a remote repository.
* Add a build step to the project to run the tests and generate test and coverage artifacts.
* Build the project and examine the test results and the generated artifacts.

The freestyle project runs the tests in the Times Table App MATLAB project (which requires R2019a or later). You can create a working copy of the project files and open the project in MATLAB by running a statement in the Command Window. The statement to run depends on your MATLAB release:

R2023a and Earlier                 | Starting in R2023b
-----------------------------------| ------------------------------------------------
`matlab.project.example.timesTable`| `openExample("matlab/TimesTableProjectExample")`

For more information about the Times Table App project, see [Explore an Example Project](https://www.mathworks.com/help/matlab/matlab_prog/explore-an-example-project.html).

## Prerequisites
To follow the steps in this example:

* MATLAB and the plugin for MATLAB must be installed on your Jenkins server. For information on how to install a plugin in Jenkins, see [Managing Plugins](https://jenkins.io/doc/book/managing/plugins/).
* The Times Table App project must be under source control. For example, you can create a new repository for the project using your GitHub&reg; account. For more information, see [Use Source Control with Projects](https://www.mathworks.com/help/matlab/matlab_prog/use-source-control-with-projects.html).
* The [coverage](https://plugins.jenkins.io/coverage/) and [JUnit](https://plugins.jenkins.io/junit) plugins must be installed. These plugins are required to publish the artifacts using post-build actions. 

## Create a Freestyle Project to Run MATLAB Tests
Create a new project and configure it by following these steps:
1. In your Jenkins interface, select **New Item** on the left. A new page opens where you can choose the type of your project. Enter a project name, and then click **Freestyle project**. To confirm your choices, click **OK**.

![create_project](https://github.com/user-attachments/assets/cedd846b-4460-43d7-9278-253c7ee7260e)

2. On the project configuration page, in the **Source Code Management** section, specify the repository that hosts your tests.

![source_control](https://github.com/user-attachments/assets/3b888ed9-b521-4c3c-a932-ad5f9de563c3)

3. In the **Environment** section, select **Use MATLAB version** and specify the MATLAB version you want to use in the build. If your preferred MATLAB version is not listed under **Use MATLAB version**, enter the full path to its root folder in the **MATLAB root** box. 

![environment](https://github.com/user-attachments/assets/00598e0c-468d-465b-b334-5c7ed750ee3f)

4. In the **Build Steps** section, select **Add build step > Run MATLAB Tests**. Then, specify the artifacts to generate in the project workspace. In this example, the plugin generates test results in JUnit-style XML format and code coverage results in Cobertura XML format. Furthermore, to generate the coverage results, the plugin uses only the code in the `source` folder located in the root of the repository. For more information about the build steps provided by the plugin, see [Plugin Configuration Guide](../CONFIGDOC.md).

![run_matlab_tests](https://github.com/user-attachments/assets/b6b7b811-d998-4fb9-bbf8-5de624bb5bd6)

5. In the **Post-build Actions** section, add the **Publish JUnit test result report** post-build action to publish the test results in JUnit-style XML format. Specify the path to the test report in the **Test report XMLs** box.

![post_build_junit](https://github.com/user-attachments/assets/82b4c99a-59c3-41e4-946d-555fb9315f35)

6. In the **Post-build Actions** section, add the **Record code coverage results** post-build action to publish the code coverage results in Cobertura XML format. Select `Cobertura Coverage Reports` from the **Coverage Parser** list and specify the path to the coverage report in the **Report File Pattern** box.

![post_build_cobertura](https://github.com/user-attachments/assets/5af14bb9-f12e-4942-a3ad-957eec4a057b)

7. Click **Save** to save the project configuration settings. You can access and modify your settings at a later stage by selecting **Configure** in the project interface, which displays the project name at the upper-left corner of the page.

## Run Tests and Inspect Artifacts
To build your freestyle project, select **Build Now** in the project interface. Jenkins triggers a build, assigns it a number under **Builds**, and runs the build. In this example, the build succeeds because all the tests in the Times Table App project pass.

Navigate to the project workspace by selecting **Workspace** in the project interface. The generated artifacts are in the `matlabTestArtifacts` folder of the workspace.

![workspace](https://github.com/user-attachments/assets/1c1ff1f8-99b7-475f-8278-180ab0185833)

Select **Status** in the project interface. You can access the published artifacts by clicking the **Latest Test Result** and **Coverage Report** links. For example, click the **Latest Test Result** link to view the published JUnit-style test results. On the test results page, click the **(root)** link in the **All Tests** table. The table expands and lists information for each of the test classes within the Times Table App project.  

![test_results](https://github.com/user-attachments/assets/51cc73aa-cf8b-455a-b210-7ecfbb772a72)

## See Also
* [Plugin Configuration Guide](../CONFIGDOC.md)<br/>
* [Explore an Example Project (MATLAB)](https://www.mathworks.com/help/matlab/matlab_prog/explore-an-example-project.html)
