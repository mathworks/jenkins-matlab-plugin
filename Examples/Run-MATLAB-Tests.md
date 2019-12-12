# Run MATLAB Tests on Jenkins Server

This example shows you how to run a suite of MATLAB&reg; unit tests with Jenkins&trade;. In particular, the example enables you to:

* Configure Jenkins to run a freestyle project including MATLAB tests.
* Interface Jenkins with a remote repository that contains the tests.
* Build the freestyle project and examine the test outcomes.

The project will run the tests specified by a test class file named *TestRand.m*, which is stored in the **Examples** folder. The class tests various aspects of random number generation as described in [Create Advanced Parameterized Test](https://www.mathworks.com/help/matlab/matlab_prog/create-advanced-parameterized-test.html).

## Prerequisites
To run MATLAB tests stored in a remote repository, Jenkins must properly interface with MATLAB as well as the repository.
* To run MATLAB tests, you must install the MATLAB Jenkins plugin. For information on how to install a plugin in Jenkins, see [Managing Plugins](https://jenkins.io/doc/book/managing/plugins/).
* Since the tests are stored in a remote GitHub repository, you must install a command-line Git™ client. For more information, see [Getting Started - Installing Git](https://git-scm.com/book/en/v2/Getting-Started-Installing-Git).

## Configure Jenkins to Run MATLAB Tests
To configure the Jenkins build for running MATLAB tests, follow these steps:
1. In your Jenkins interface, select **New Item** on the left. A new page opens where you can choose the type of your project. Enter a project name, then click **Freestyle project**. To confirm the project name and creation, click **OK**.
2. In the configuration window of your Jenkins project, navigate to the **Source Code Management** section and click **Git**. This option enables Jenkins to interface with a remote GitHub repository.
3. Navigate to the main page of this GitHub Repository in your system browser. Click **Clone or download** and copy the web URL to your clipboard.
4. In the Jenkins interface, paste the URL into the **Repository URL** field of the **Source Code Management** section. (You also can specify the branch to build in the **Branch Specifier** field.)
5. In the **Build** section of Jenkins, click **Add build step > Run MATLAB Tests**. A new window opens that enables you to configure the MATLAB Jenkins plugin. Paste the full path to the **MATLAB root** folder into the MATLAB root field; choose the default Automatic test mode from the **Test mode** drop-down list; and select your desired test artifacts to be generated in the project wrokspace. For more information on how to configure the plugin, see [Jenkins MATLAB Plugin](https://github.com/jenkinsci/matlab-plugin).
6. Click **Save** to save the project configuration settings. You can access and modify your settings at a later stage by selecting **Configure** in the project interface.

## Run Tests in Freestyle Project
To build your Jenkins project and run the tests specified in this GitHub repository, click **Build now** in the project interface, which displays the project name at the top-left of the page. Jenkins triggers a build, assigns it a number under **Build History**, and runs the build. If the build is successful, a blue circle icon appears next to the build number. If the build fails, Jenkins adds a red circle icon. In this example, the build passes because all of the tests defined by the *TestRand* class pass.

![jenkins_workspace](https://user-images.githubusercontent.com/48831250/70753800-9e875c80-1d03-11ea-9b4d-41c9bd0c005e.png)

Navigate to the project workspace by clicking the **Workspace** icon in the project interface. You can view the generated test artifacts in the **matlabTestArtifacts** folder of the workspace.


## See Also
[MathWorks Blogs: Developer Zone](https://blogs.mathworks.com/developer/category/continuous-integration/)<br/>
[matlab.unittest.plugins Package](https://www.mathworks.com/help/matlab/ref/matlab.unittest.plugins-package.html)<br/>
[Source Control Integration](https://www.mathworks.com/help/matlab/source-control.html)