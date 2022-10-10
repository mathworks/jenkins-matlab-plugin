# Run MATLAB Tests on Jenkins Server
This example shows how to run a suite of MATLAB&reg; unit tests with Jenkins&trade;. The example demonstrates how to:

* Configure a freestyle project to access MATLAB tests hosted in a remote repository.
* Add a build step to the project to run the tests and generate test and coverage artifacts.
* Build the project and examine the test results and the generated artifacts.

The freestyle project runs the tests in the Times Table App MATLAB project (which requires R2019a or later). You can create a working copy of the project files and open the project in MATLAB by running this statement in the Command Window.

```
matlab.project.example.timesTable
```

For more information about the Times Table App example project, see [Explore an Example Project](https://www.mathworks.com/help/matlab/matlab_prog/explore-an-example-project.html).

## Prerequisites
To follow the steps in this example:

* MATLAB and the plugin for MATLAB must be installed on your Jenkins server. For information on how to install a plugin in Jenkins, see [Managing Plugins](https://jenkins.io/doc/book/managing/plugins/).
* The Times Table App project must be under source control. For example, you can create a new repository for the project using your GitHub&reg; account. For more information, see [Use Source Control with Projects](https://www.mathworks.com/help/matlab/matlab_prog/use-source-control-with-projects.html).
* The [Cobertura](https://plugins.jenkins.io/cobertura) and [JUnit](https://plugins.jenkins.io/junit) plugins must be installed. These plugins are required to publish the artifacts using post-build actions. 

## Create a Freestyle Project to Run MATLAB Tests
Create a new project and configure it by following these steps:
1. In your Jenkins interface, select **New Item** on the left. A new page opens where you can choose the type of your project. Enter a project name, and then click **Freestyle project**. To confirm your choices, click **OK**.

![create_project](https://user-images.githubusercontent.com/48831250/193948023-65f038c2-a81e-416b-8dea-0cf91ae5f105.png)

2. On the project configuration page, in the **Source Code Management** section, specify the repository that hosts your tests.

![source_control](https://user-images.githubusercontent.com/48831250/94478391-37a73700-01a1-11eb-9f89-a5a71413baf0.png)

3. In the **Build Environment** section, select **Use MATLAB version** and specify the MATLAB version you want to use in the build. If your desired MATLAB version is not listed under **Use MATLAB version**, enter the full path to its root folder in the **MATLAB root** box. 

![build_environment](https://user-images.githubusercontent.com/48831250/193948480-564ac249-0ef2-407b-b18c-ee52b5129ac0.png)

4. In the **Build** section, select **Add build step > Run MATLAB Tests**. Then, specify the artifacts to be generated in the project workspace. In this example, the plugin generates Cobertura code coverage and JUnit-style test results reports. Furthermore, to generate the coverage report, the plugin uses only the code in the `source` folder located in the root of the repository. For more information about the build steps provided by the plugin, see [Plugin Configuration Guide](../CONFIGDOC.md).

![run_,matlab_tests](https://user-images.githubusercontent.com/48831250/193948891-aa09960a-04ba-4a13-9eea-e4bebab9371d.png)

5. In the **Post-build Actions** section, add two post-build actions to publish the Cobertura code coverage and JUnit-style test results reports. For each artifact, provide the path to the report.

![post_build](https://user-images.githubusercontent.com/48831250/193949176-aced5dfb-f7b9-4978-8726-7d01bae4bc97.png)

6. Click **Save** to save the project configuration settings. You can access and modify your settings at a later stage by selecting **Configure** in the project interface, which displays the project name at the top-left of the page.

## Run Tests and Inspect Artifacts
To build your freestyle project, click **Build Now** in the project interface. Jenkins triggers a build, assigns it a number under **Build History**, and runs the build. If the build is successful, a blue circle icon appears next to the build number. If the build fails, Jenkins adds a red circle icon. In this example, the build succeeds because all the tests in the Times Table App project pass.

Navigate to the project workspace by clicking the **Workspace** icon in the project interface. The generated artifacts are in the `matlabTestArtifacts` folder of the workspace.

![workspace](https://user-images.githubusercontent.com/48831250/193951807-a09ef1f8-cf7e-49c8-8615-af5fbd97acd0.png)

Click the **Status** icon in the project interface. You can access the published artifacts by opening the **Latest Test Result** and **Coverage Report** links. For example, open the **Latest Test Result** link to view the published JUnit-style test results. In the new page, open the link in the **All Tests** table. The table expands and lists information for each of the test classes within the Times Table App project.  

![test_results](https://user-images.githubusercontent.com/48831250/194086287-35f7c677-4c18-4316-b676-f2491ffa20a7.png)

## See Also
* [Plugin Configuration Guide](../CONFIGDOC.md)<br/>
* [Explore an Example Project (MATLAB)](https://www.mathworks.com/help/matlab/matlab_prog/explore-an-example-project.html)
