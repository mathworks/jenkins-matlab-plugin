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
* The [Cobertura](https://plugins.jenkins.io/cobertura) and [JUnit](https://plugins.jenkins.io/junit) plugins must be installed. These plugins are required to publish the artifacts using post-build actions. 

## Create a Freestyle Project to Run MATLAB Tests
Create a new project and configure it by following these steps:
1. In your Jenkins interface, select **New Item** on the left. A new page opens where you can choose the type of your project. Enter a project name, and then click **Freestyle project**. To confirm your choices, click **OK**.

![create_project](https://github.com/mathworks/jenkins-matlab-plugin/assets/48831250/8aa314b3-60fc-4534-bd49-223617ca0542)

2. On the project configuration page, in the **Source Code Management** section, specify the repository that hosts your tests.

![source_control](https://github.com/mathworks/jenkins-matlab-plugin/assets/48831250/5befa3c5-6924-4abb-bc34-25ff3328ee47)

3. In the **Build Environment** section, select **Use MATLAB version** and specify the MATLAB version you want to use in the build. If your preferred MATLAB version is not listed under **Use MATLAB version**, enter the full path to its root folder in the **MATLAB root** box. 

![build_environment](https://github.com/mathworks/jenkins-matlab-plugin/assets/48831250/c77220c0-a521-41ad-b0e4-76a6f0afce28)

4. In the **Build Steps** section, select **Add build step > Run MATLAB Tests**. Then, specify the artifacts to generate in the project workspace. In this example, the plugin generates test results in JUnit-style XML format and code coverage results in Cobertura XML format. Furthermore, to generate the coverage results, the plugin uses only the code in the `source` folder located in the root of the repository. For more information about the build steps provided by the plugin, see [Plugin Configuration Guide](../CONFIGDOC.md).

![run_matlab_tests](https://github.com/mathworks/jenkins-matlab-plugin/assets/48831250/2ef326b7-9b39-4068-83b9-011cebd52506)

5. In the **Post-build Actions** section, add two post-build actions to publish the JUnit-style test results and the Cobertura code coverage results. For each artifact, provide the path to the report.

![post_build](https://github.com/mathworks/jenkins-matlab-plugin/assets/48831250/d584a290-de93-4a9f-8061-00bc2a435c12)

6. Click **Save** to save the project configuration settings. You can access and modify your settings at a later stage by selecting **Configure** in the project interface, which displays the project name at the upper-left corner of the page.

## Run Tests and Inspect Artifacts
To build your freestyle project, select **Build Now** in the project interface. Jenkins triggers a build, assigns it a number under **Build History**, and runs the build. In this example, the build succeeds because all the tests in the Times Table App project pass.

Navigate to the project workspace by selecting **Workspace** in the project interface. The generated artifacts are in the `matlabTestArtifacts` folder of the workspace.

![workspace](https://github.com/mathworks/jenkins-matlab-plugin/assets/48831250/5195fb71-6f4f-4261-82c0-501ab953a079)

Select **Status** in the project interface. You can access the published artifacts by clicking the **Latest Test Result** and **Coverage Report** links. For example, click the **Latest Test Result** link to view the published JUnit-style test results. On the test results page, click the **(root)** link in the **All Tests** table. The table expands and lists information for each of the test classes within the Times Table App project.  

![test_results](https://github.com/mathworks/jenkins-matlab-plugin/assets/48831250/2dc5142d-b844-436c-8e9e-8f7b90c7b69e)

## See Also
* [Plugin Configuration Guide](../CONFIGDOC.md)<br/>
* [Explore an Example Project (MATLAB)](https://www.mathworks.com/help/matlab/matlab_prog/explore-an-example-project.html)
