When you define an automated pipeline of tasks in Jenkins&trade;, whether in the web UI or with a [`Jenkinsfile`](https://www.jenkins.io/doc/book/pipeline/jenkinsfile/), you can use the plugin to run your MATLAB&reg; code or Simulink&reg; models. This guide demonstrates how to configure the plugin and use it in freestyle, multi-configuration, and Pipeline projects.

> :information_source: **Note:** To run MATLAB code or Simulink models, Jenkins requires a valid MATLAB or Simulink license. If you have installed Jenkins as a Windows&reg; service application, you need to provide a MathWorks&reg; username and password in the **Jenkins Properties** window (accessible from within the Windows Task Manager).

-  [Configure Plugin in Web UI](#configure-plugin-in-web-ui)
      -  [Use MATLAB in Build](#use-matlab-in-build)
      -  [Specify Build Steps](#specify-build-steps)
         - [Run MATLAB Command](#run-matlab-command)
         - [Run MATLAB Tests](#run-matlab-tests)
-  [Set Up Freestyle Project](#set-up-freestyle-project)
-  [Set Up Multi-Configuration Project](#set-up-multi-configuration-project)
      -  [Add MATLAB Axis](#add-matlab-axis)
      -  [Add User-Defined Axis](#add-user-defined-axis)
-  [Set Up Pipeline Project](#set-up-pipeline-project)
   -  [Add MATLAB to System Path](#add-matlab-to-system-path)
   -  [Use the runMATLABCommand Step](#use-the-runmatlabcommand-step)
   -  [Use the runMATLABTests Step](#use-the-runmatlabtests-step) 
   -  [Use MATLAB in Matrix Build](#use-matlab-in-matrix-build)
-  [Register MATLAB as Jenkins Tool](#register-matlab-as-jenkins-tool)
   -  [Use MATLAB as a Tool in Pipeline Project](#use-matlab-as-a-tool-in-pipeline-project)

## Configure Plugin in Web UI
You can use the web UI provided by Jenkins to configure the plugin in freestyle and multi-configuration projects. To run MATLAB or Simulink in a Pipeline project, see [Set Up Pipeline Project](#set-up-pipeline-project).

### Use MATLAB in Build
Once you install the plugin, **Use MATLAB version** appears in the **Build Environment** section of the project configuration window. Select **Use MATLAB version** to specify the MATLAB version you want to use in the build. You can select one of the MATLAB versions that have been registered as Jenkins tools, or you can select **Custom** if you want to specify a different version. For more information about registering a MATLAB version as a tool, see [Register MATLAB as Jenkins Tool](#register-matlab-as-jenkins-tool).

In this example, the list includes two registered tools as well as the option for specifying a custom installation. If you select **Custom**, a **MATLAB root** box appears in the UI. You must enter the full path to your desired MATLAB root folder in this box.

![use_matlab_version_tool](https://user-images.githubusercontent.com/48831250/105066626-fb982a80-5a4c-11eb-8d9a-d044c16fdb53.png)

When you specify a MATLAB version in the **Build Environment** section, the plugin prepends its `bin` folder to the system PATH environment variable of the build agent and invokes it to perform the build. If the build agent already has your desired MATLAB on the path, then you are not required to select **Use MATLAB version**. The plugin always uses the topmost MATLAB version on the system path. The build fails if the operating system cannot find MATLAB on the path.

You can use the [`matlabroot`](https://www.mathworks.com/help/matlab/ref/matlabroot.html) function to return the full path to your desired MATLAB root folder. The path depends on the platform, MATLAB version, and installation location. This table shows examples of the root folder path on different platforms. 

| Platform     | Path to MATLAB Root Folder      |
|--------------|---------------------------------|
| Windows      | C:\Program Files\MATLAB\R2021a  |
| Linux&reg;   | /usr/local/MATLAB/R2021a        |
| macOS        | /Applications/MATLAB_R2021a.app |

### Specify Build Steps
When you set up the **Build** section of the project configuration window, the plugin provides you with the **Run MATLAB Command** and **Run MATLAB Tests** build steps.

If you use a source code management (SCM) system such as Git&trade;, then your project should include the appropriate SCM configuration to check out the code before it can invoke the plugin. If you do not use any SCM systems to manage your code, then an additional build step might be required to ensure that the code is available in the project workspace before the build starts.

#### Run MATLAB Command
The **Run MATLAB Command** build step lets you run MATLAB scripts, functions, and statements. You can use this build step to customize your test run or execute any MATLAB commands.

Specify the MATLAB script, function, or statement you want to execute in the **Command** box. If you specify more than one script, function, or statement, use a comma or semicolon to separate them. If you want to run a script or function, do not specify the file extension.

For example, run a script named `myscript.m` in the root of your repository.

![run_matlab_command](https://user-images.githubusercontent.com/48831250/94472499-d8ddbf80-0198-11eb-8f25-ead3c4039f55.png)

MATLAB exits with exit code 0 if the specified script, function, or statement executes successfully without error. Otherwise, MATLAB terminates with a nonzero exit code, which causes the build to fail. To ensure that the build fails in certain conditions, use the [`assert`](https://www.mathworks.com/help/matlab/ref/assert.html) or [`error`](https://www.mathworks.com/help/matlab/ref/error.html) functions.

When you use this step, all the required files must be on the MATLAB search path. If your script or function is not in the root of your repository, you can use the [`addpath`](https://www.mathworks.com/help/matlab/ref/addpath.html), [`cd`](https://www.mathworks.com/help/matlab/ref/cd.html), or [`run`](https://www.mathworks.com/help/matlab/ref/run.html) functions to ensure that it is on the path when invoked. For example, to run `myscript.m` in a folder named `myfolder` and located in the root of the repository, you can specify the contents of the **Command** box like this:

`addpath('myfolder'), myscript`

#### Run MATLAB Tests
This build step lets you run MATLAB and Simulink tests and generate artifacts such as JUnit-style test results and Cobertura coverage reports. By default, the plugin includes any test files in your [MATLAB project](https://www.mathworks.com/help/matlab/projects.html) that have a `Test` label. If your build does not use a MATLAB project, or if it uses a MATLAB release before R2019a, then the plugin includes all tests in the root of your repository or in any of its subfolders.
 
With the **Run MATLAB Tests** build step, you can customize your test run using the existing options in the step configuration interface. For example, you can add folders to the MATLAB search path, control which tests to run, and generate various code and coverage artifacts. If you do not select any of the check boxes, all the tests in your project run, and any test failure causes the build to fail.
 
![run_matlab_tests](https://user-images.githubusercontent.com/48831250/105909610-c2842b00-5ff5-11eb-9b6a-9530ae7289ff.png)
 
Select **Source folder** if you want to specify the location of a folder containing source code, relative to the project root folder. The plugin adds the specified folder and its subfolders to the top of the MATLAB search path. If you specify a source folder and then generate a coverage report, the plugin uses only the source code in the specified folder and its subfolders to generate the report. You can specify more than one folder by clicking **Add folder**.

![run_matlab_tests_source](https://user-images.githubusercontent.com/48831250/104773323-e5375980-5742-11eb-84ed-e71a23a2dc55.png)

By default, the **Run MATLAB Tests** step creates a test suite from all the tests in your project. To create a filtered test suite, select **By folder name**, **By tag**, or both:

* Select **By folder name** if you want to specify the location of a folder containing test files, relative to the project root folder. The plugin creates a test suite using only the tests in the specified folder and its subfolders. You can specify more than one folder by clicking **Add folder**.

* Select **By tag** if you want to select test suite elements using a specified test tag.

![run_matlab_tests_filter](https://user-images.githubusercontent.com/48831250/105909635-cfa11a00-5ff5-11eb-8642-7fc037dbedf5.png)

Select check boxes in the **Generate Test Artifacts** and **Generate Coverage Artifacts** sections if you want to generate test and coverage artifacts. To publish the test results, you can use these artifacts with other Jenkins plugins. By default, the plugin assigns a name to each selected artifact and stores it in the `matlabTestArtifacts` folder of the project workspace. You can override the default artifact name and location by specifying a path relative to the project root folder in the **File path** box. The plugin does not create the `matlabTestArtifacts` folder if the name of the folder does not appear in any of the displayed **File path** boxes.

![run_matlab_tests_artifacts](https://user-images.githubusercontent.com/48831250/104773381-f7b19300-5742-11eb-9dad-392c5ca8777d.png)

The **Run MATLAB Tests** build step produces a MATLAB script file and uses it to run the tests and generate the artifacts. The plugin writes the contents of this file to the build log. You can review the build log in **Console Output** to understand the testing workflow.
 
Artifacts to generate with the plugin are subject to these restrictions: 
* Producing a PDF test report on macOS platforms is supported in MATLAB R2020b and later.
* Exporting Simulink Test&trade; Manager results requires a Simulink Test license and is supported in MATLAB R2019a and later.
* Producing a Cobertura model coverage report requires a Simulink Coverage&trade; license and is supported in MATLAB R2018b and later.

## Set Up Freestyle Project
To configure the plugin for a freestyle project, specify the MATLAB version to use as well as the required build steps.

To specify the MATLAB version, select **Use MATLAB version** in the **Build Environment** section of the project configuration window. Then, specify the MATLAB version that Jenkins should use in the build. You can skip this step if MATLAB has already been added to the path on the build agent.

![build_environment](https://user-images.githubusercontent.com/48831250/105088650-2cd32380-5a6a-11eb-94ee-533f7f7fdf78.png)

To run MATLAB code and Simulink models, specify the appropriate build steps in the **Build** section:
* If you add the [**Run MATLAB Command**](#run-matlab-command) build step, specify your MATLAB script, function, or statement in the **Command** box. 
* If you add the [**Run MATLAB Tests**](#run-matlab-tests) build step, specify the source code, the test suite filters, and the artifacts to be generated in the project workspace.
   
## Set Up Multi-Configuration Project
The plugin supports [multi-configuration (matrix) projects](https://plugins.jenkins.io/matrix-project/). Multi-configuration projects are useful when builds include similar steps. For example:
* The same test suite is run on different platforms (such as Windows, Linux, and macOS).
* The same test suite is run against different versions of MATLAB.

To configure the plugin for a multi-configuration project, specify the MATLAB versions to use as well as the required build steps. You can add axes in the **Configuration Matrix** section to specify the duplicating build steps. 

There are two ways to specify multiple MATLAB versions in a multi-configuration project: using the **MATLAB** axis or using a user-defined axis.

![add_axis](https://user-images.githubusercontent.com/48831250/105097497-b0930d00-5a76-11eb-9ccf-585f2d6e0bcc.png)

### Add MATLAB Axis
If your Jenkins instance includes MATLAB versions registered as tools, then **MATLAB** appears as an option when you click **Add axis** in the **Configuration Matrix** section. By adding the **MATLAB** axis, you can select MATLAB versions and add them as axis values to your matrix configuration. The list includes all MATLAB versions that have been registered as Jenkins tools. In this example, there are two MATLAB versions registered as tools. In each build iteration, the plugin prepends one of the selected versions to the PATH environment variable and invokes it to run the build. 

![matlab_axis](https://user-images.githubusercontent.com/48831250/106194057-554ed200-617c-11eb-9fa5-7d74a9a8a510.png)

For more information about registering a MATLAB version as a tool, see [Register MATLAB as Jenkins Tool](#register-matlab-as-jenkins-tool).

> :information_source: **Note:** When you add the **MATLAB** axis, do not select **Use MATLAB version**. Any values you specify by **Use MATLAB version** take precedence over the values specified by the **MATLAB** axis.

### Add User-Defined Axis
If you do not specify the **MATLAB** axis, add a user-defined axis in the **Configuration Matrix** section to specify the MATLAB versions in the build. Enter the name of the axis in the **Name** box and its values in the **Values** box. Separate the values with a space. For instance, specify two MATLAB versions to run the same set of tests.

![user_defined_axis](https://user-images.githubusercontent.com/48831250/105099544-d968d180-5a79-11eb-9fb6-a9bbf262c09d.png)

When you add a user-defined axis to specify MATLAB versions, you must also specify where they are installed. To do this, select **Use MATLAB version** in the **Build Environment** section and then construct a root folder path using the axis name. In this example, `$VERSION` in the **MATLAB root** box is replaced by one axis value per build iteration. 

![build_environment_matrix](https://user-images.githubusercontent.com/48831250/105099364-90b11880-5a79-11eb-86de-c026a1dd2a1a.png)

A multi-configuration project creates a separate workspace for each user-defined axis value. If you specify the full paths to where different MATLAB versions are installed as axis values, the plugin fails to create separate workspaces and fails the build.

> :information_source: **Note:** Both `$VAR` and `${VAR}` are valid formats for accessing the values of the axis `VAR`. On macOS platforms, the `${VAR}` format is recommended.

You can add several axes in the **Configuration Matrix** section. For example, add the **MATLAB** axis to specify MATLAB versions and the user-defined `TEST_TAG` axis to specify the test tags for a group of tests.

![axis_matlab_testtag](https://user-images.githubusercontent.com/48831250/106194098-6566b180-617c-11eb-8501-64a192378580.png)

Once you have specified the axes, add the required build steps in the **Build** section:

* If you add the [**Run MATLAB Command**](#run-matlab-command) build step, specify your MATLAB script, function, or statement in the **Command** box. You can use the user-defined axes to specify the contents of the **Command** box. For example:
   
```
results = runtests(pwd,'Tag','$TEST_TAG'); assertSuccess(results);
```

* If you add the [**Run MATLAB Tests**](#run-matlab-tests) build step, specify the source code, the test suite filters, and the artifacts to be generated in the project workspace.

## Set Up Pipeline Project
When you define your Pipeline with a `Jenkinsfile`, the plugin provides you with a step to run MATLAB scripts, functions, and statements. The plugin also provides a step to run MATLAB and Simulink tests. These steps are common to both Declarative and Scripted Pipelines.

To configure the plugin for a Pipeline project:
1) Define your Pipeline in a `Jenkinsfile` in the root of your repository.
2) In the **Pipeline** section of the project configuration window, select **Pipeline script from SCM** from the **Definition** list. 
3) Select your source control system from the **SCM** list.
4) Paste your repository URL into the **Repository URL** box.

You also can define your Pipeline directly in the project configuration window. If you select **Pipeline script** from the **Definition** list, you can author your Pipeline code in the **Script** box. When you define your Pipeline this way, it must include an additional stage to check out your code from source control.

### Add MATLAB to System Path
When the plugin executes MATLAB related steps in your Pipeline, it uses the topmost MATLAB version on the system path. If the PATH environment variable of the build agent does not include any MATLAB versions, you must update the variable with the MATLAB root folder that should be used for the build.

To update the system PATH environment variable using Declarative Pipeline syntax, use an `environment` block in your `Jenkinsfile`. For example, prepend MATLAB R2021a to the system PATH environment variable and use it to run your command.

```groovy
// Declarative Pipeline
pipeline {
   agent any
   environment {
       PATH = "C:\\Program Files\\MATLAB\\R2021a\\bin;${PATH}"   // Windows agent
    // PATH = "/usr/local/MATLAB/R2021a/bin:${PATH}"   // Linux agent
    // PATH = "/Applications/MATLAB_R2021a.app/bin:${PATH}"   // macOS agent    
   }
    stages{
        stage('Run MATLAB Command') {
            steps
            {
               runMATLABCommand "disp('Hello World!')"
            }       
        }                
    } 
}
``` 
If you define your Pipeline using Scripted Pipeline syntax, set the PATH environment variable in the `node` block. For example: 

```groovy
// Scripted Pipeline
node {
    env.PATH = "C:\\Program Files\\MATLAB\\R2021a\\bin;${env.PATH}"   //Windows agent
    // env.PATH = "/usr/local/MATLAB/R2021a/bin:${env.PATH}"   //Linux agent
    // env.PATH = "/Applications/MATLAB_R2021a.app/bin:${env.PATH}"   //macOS agent
    runMATLABCommand "disp('Hello World!')"
}
``` 

### Use the runMATLABCommand Step
Use the `runMATLABCommand` step in your Pipeline to run MATLAB scripts, functions, and statements. You can use this step to customize your test run or execute any MATLAB commands.

You must provide `runMATLABCommand` with a string that specifies the script, function, or statement you want to execute. If you specify more than one script, function, or statement, use a comma or semicolon to separate them. If you want to run a script or function, do not specify the file extension.

**Example:** `runMATLABCommand 'myscript'`<br/>
**Example:** `runMATLABCommand 'results = runtests, assertSuccess(results);'` 

For example, in your `Jenkinsfile`, define a Declarative Pipeline to run a script named `myscript.m`.

```groovy
// Declarative Pipeline
pipeline {
    agent any
    stages{
        stage('Run MATLAB Command') {
            steps
            {
                runMATLABCommand 'myscript'
            }       
        }                
    } 
}
``` 

You also can use `runMATLABCommand` in a Scripted Pipeline.

```groovy
// Scripted Pipeline
node {
    runMATLABCommand 'myscript'  
}
``` 

MATLAB exits with exit code 0 if the specified script, function, or statement executes successfully without error. Otherwise, MATLAB terminates with a nonzero exit code, which causes the current stage to fail. If you properly react to the resulting MATLAB execution exception, the remaining stages of your Pipeline can still run, and your build can succeed. Otherwise, Jenkins terminates the build in the current stage and marks it as a failure.

When you use the `runMATLABCommand` step, all the required files must be on the MATLAB search path. If your script or function is not in the root of your repository, you can use the [`addpath`](https://www.mathworks.com/help/matlab/ref/addpath.html), [`cd`](https://www.mathworks.com/help/matlab/ref/cd.html), or [`run`](https://www.mathworks.com/help/matlab/ref/run.html) functions to ensure that it is on the path when invoked. For example, to run `myscript.m` in a folder named `myfolder` and located in the root of the repository, you can specify the `runMATLABCommand` step like this: 

`runMATLABCommand 'addpath('myfolder'), myscript'` 

### Use the runMATLABTests Step
Use the `runMATLABTests` step in your Pipeline to run MATLAB and Simulink tests and generate various code and coverage artifacts. By default, the plugin includes any test files in your [MATLAB project](https://www.mathworks.com/help/matlab/projects.html) that have a `Test` label. If your Pipeline does not use a MATLAB project, or if it uses a MATLAB release before R2019a, then the plugin includes all tests in the root of your repository or in any of its subfolders.

For example, in your `Jenkinsfile`, define a Declarative Pipeline to run the tests in your project.


```groovy
// Declarative Pipeline
pipeline {
    agent any
    stages{
        stage('Run MATLAB Tests') {
            steps
            {
                runMATLABTests()
            }       
        }                
    } 
}
``` 

Use the `runMATLABTests` step in a Scripted Pipeline to run the tests in your project.

```groovy
// Scripted Pipeline
node {
    runMATLABTests()  
}
``` 

MATLAB exits with exit code 0 if the test suite runs successfully without any test failures. Otherwise, MATLAB terminates with a nonzero exit code, which causes the current stage to fail. If you properly react to the resulting MATLAB execution exception, the remaining stages of your Pipeline can still run, and your build can succeed. Otherwise, Jenkins terminates the build in the current stage and marks it as a failure.

The `runMATLABTests` step lets you customize your test run using optional inputs. For example, you can add folders to the MATLAB search path, control which tests to run, and generate various artifacts.

Input                     | Description    
------------------------- | ---------------
`sourceFolder`            | (Optional) Location of the folder containing source code, relative to the project root folder. The specified folder and its subfolders are added to the top of the MATLAB search path. If you specify `sourceFolder` and then generate a coverage report, the plugin uses only the source code in the specified folder and its subfolders to generate the report.<br/>**Example:** `['source']`<br/>**Example:** `['source/folderA', 'source/folderB']`
`selectByFolder`          | (Optional) Location of the folder used to select test suite elements, relative to the project root folder. To create a test suite, the plugin uses only the tests in the specified folder and its subfolders.<br/>**Example:** `['test']`<br/>**Example:** `['test/folderA', 'test/folderB']`
`selectByTag`             | (Optional) Test tag used to select test suite elements. To create a test suite, the plugin uses only the test elements with the specified tag.<br/>**Example:** `'FeatureA'`
`testResultsPDF`          | (Optional) Path to write test results report in PDF format. On macOS platforms, this input is supported in MATLAB R2020b and later.<br/>**Example:** `'test-results/results.pdf'`      
`testResultsTAP`          | (Optional) Path to write test results report in TAP format.<br/>**Example:** `'test-results/results.tap'`
`testResultsJUnit`        | (Optional) Path to write test results report in JUnit XML format.<br/>**Example:** `'test-results/results.xml'`
`testResultsSimulinkTest` | (Optional) Path to export Simulink Test Manager results in MLDATX format. This input requires a Simulink Test license and is supported in MATLAB R2019a and later.<br/>**Example:** `'test-results/results.mldatx'`
`codeCoverageCobertura`   | (Optional) Path to write code coverage report in Cobertura XML format.<br/>**Example:** `'code-coverage/coverage.xml'`
`modelCoverageCobertura`  | (Optional) Path to write model coverage report in Cobertura XML format. This input requires a Simulink Coverage license and is supported in MATLAB R2018b and later.<br/>**Example:** `'model-coverage/coverage.xml'`

For instance, define a Declarative Pipeline to run the tests in your MATLAB project, and then generate a JUnit-style test results report and a Cobertura code coverage report at specified locations on the build agent. Generate the coverage report for only the code in the `source` folder in the root of your repository. 


```groovy
// Declarative Pipeline
pipeline {
    agent any
    stages{
        stage('Run MATLAB Tests') {
            steps
            {
                runMATLABTests(testResultsJUnit: 'test-results/results.xml',
                               codeCoverageCobertura: 'code-coverage/coverage.xml',
                               sourceFolder: ['source'])
            }       
        }                
    } 
}
``` 

Define a Scripted Pipeline to run your tests and generate artifacts.

```groovy
// Scripted Pipeline
node {
    runMATLABTests(testResultsJUnit: 'test-results/results.xml',
                   codeCoverageCobertura: 'code-coverage/coverage.xml',
                   sourceFolder: ['source']) 
}
``` 

## Use MATLAB in Matrix Build
Like multi-configuration projects, you can use MATLAB as part of a [matrix](https://www.jenkins.io/doc/book/pipeline/syntax/#declarative-matrix) build in Pipeline projects. For example, you can define a Pipeline to run your test suite on different platforms or against different versions of MATLAB.

This example shows how to define a Declarative Pipeline to run your MATLAB code and generate artifacts using MATLAB R2019a, R2020b, and R2021a. The Pipeline has a `matrix` block to define the possible name-value combinations that should run in parallel. 

```groovy
// Declarative Pipeline
pipeline {
    agent any
    stages {
        stage('BuildAndTest') {
            matrix {
                agent any
                environment {
                    PATH = "C:\\Program Files\\MATLAB\\${MATLAB_VERSION}\\bin;${PATH}"   // Windows agent
                }
                axes {
                    axis {
                        name 'MATLAB_VERSION'
                        values 'R2019a', 'R2020b', 'R2021a'
                    }
                }
                stages {
                    stage('Run MATLAB commands') {
                        steps {
                            runMATLABCommand 'ver'
                            runMATLABCommand 'pwd'
                        }
                    }
                    stage('Run MATLAB tests'){
                        steps {
                            runMATLABTests(testResultsJUnit: 'test-results/results.xml',
                                           codeCoverageCobertura: 'code-coverage/coverage.xml')
                        }  
                    }
                }
            } 
        }
    }
}
``` 

## Register MATLAB as Jenkins Tool
If it runs MATLAB code and Simulink models as part of your automated pipeline of tasks, Jenkins invokes MATLAB as an external program. When you configure your project, you can explicitly specify the MATLAB version that Jenkins should invoke by providing the path to the desired MATLAB root folder. For example, you can use an `environment` block in your `Jenkinsfile` to specify a MATLAB root folder for your Pipeline project.

Instead of specifying the path to the MATLAB root folder on a per-project basis, you can register a MATLAB version as a Jenkins tool, which makes it available to any project you configure in Jenkins. To register a MATLAB version as a tool, specify its name and location on the build agent. Once you have registered a MATLAB version as a tool, you no longer need to specify its root folder path within a project. Jenkins only needs the tool name to access the MATLAB version.

To register a MATLAB version as a Jenkins tool:

1) In your Jenkins interface, select **Manage Jenkins > Global Tool Configuration**. The **Global Tool Configuration** page opens where you can register different tools with Jenkins.
2) In the **MATLAB** section of the **Global Tool Configuration** page, click **Add MATLAB**. The section expands and lets you assign a name to your desired MATLAB version and specify its installation location.
3) Specify the name you want to assign to the MATLAB version in the **Name** box, and enter the full path to its root folder in the **MATLAB root** box. To register the MATLAB version as a tool, do not select **Install automatically**. 
4) To confirm your choices, click **Save** at the bottom of the page.

For example, register MATLAB R2020b as a Jenkins tool on your Windows local agent.

![matlab_tool](https://user-images.githubusercontent.com/48831250/98566654-0714eb80-227d-11eb-90b8-4875ab32bf66.png)

If your Jenkins instance includes remote agents, you can register MATLAB as a tool on the remote agents using the tool name that you have specified on the local agent. For example, if you have registered MATLAB R2020b as a tool on your local agent, you can register the same MATLAB version installed on a remote agent as a tool on that agent. To register a MATLAB version as a Jenkins tool on a remote agent: 

1) Navigate to the **Node Properties** interface of the agent. You can access this interface by selecting **Manage Jenkins > Manage Nodes and Clouds**, following the link corresponding to the agent, and then selecting **Configure** on the left.
2) Select **Tool Locations**. Then, select the tool name from the **Name** list. The list contains the names assigned to the registered MATLAB versions on the local agent.  
3) In the **Home** box, enter the full path to the MATLAB root folder on the remote agent.
4) Click **Save** to confirm your choices.

For example, on a Linux remote agent, register MATLAB R2020b as a tool.

![tool_remote](https://user-images.githubusercontent.com/48831250/98685471-67676400-2335-11eb-9db5-bd027bc053f6.PNG)

### Use MATLAB as a Tool in Pipeline Project
To invoke MATLAB as a Jenkins tool using Declarative Pipeline syntax, use a `tools` block in your `Jenkinsfile`. To specify the tool in the block, use the `matlab` keyword followed by the name assigned to the tool on the **Global Tool Configuration** page. For example, run `myscript.m` using the MATLAB version that has been registered as a tool named R2021a.  

```groovy
// Declarative Pipeline
pipeline {
   agent any
   tools {
       matlab 'R2021a'
   }
    stages{
        stage('Run MATLAB Command') {
            steps
            {
               runMATLABCommand 'myscript'
            }       
        }                
    } 
}

```

If you define your Pipeline using Scripted Pipeline syntax, use the `tool` keyword followed by the name of the tool to retrieve the path to the MATLAB root folder. Then, prepend the MATLAB `bin` folder to the PATH environment variable.

```groovy
// Scripted Pipeline
node {
    def matlabver
    stage('Run MATLAB Command') {
        matlabver = tool 'R2021a'
        if (isUnix()){
            env.PATH = "${matlabver}/bin:${env.PATH}"   // Linux or macOS agent
        }else{
            env.PATH = "${matlabver}\\bin;${env.PATH}"   // Windows agent
        }     
        runMATLABCommand 'myscript'
    }
}
```
You also can invoke MATLAB as a Jenkins tool when you perform a matrix build in your Pipeline project. This example shows how to use three MATLAB versions (specified in an `axis` block using their tool names) to run a set of MATLAB commands and tests. 

```groovy
// Declarative Pipeline
pipeline {
    agent any
    stages {
        stage('BuildAndTest') {
            matrix {
                agent any
                axes {
                    axis {
                        name 'MATLAB_VERSION'
                        values 'R2019a', 'R2020b', 'R2021a'
                    }
                }
                tools{
                    matlab "${MATLAB_VERSION}"
                }
                stages {
                    stage('Run MATLAB commands') {
                        steps {
                            runMATLABCommand 'ver'
                            runMATLABCommand 'pwd'
                        }
                    }
                    stage('Run MATLAB Tests') {
                    steps
                        {
                            runMATLABTests(testResultsJUnit: 'test-results/results.xml',
                                           codeCoverageCobertura: 'code-coverage/coverage.xml')
                        }
                    }
                }
            }
        }
    }
}
```

## See Also
* [Run MATLAB Tests on Jenkins Server](examples/Run-MATLAB-Tests.md)<br/>
* [Continuous Integration with MATLAB and Simulink](https://www.mathworks.com/solutions/continuous-integration.html)
