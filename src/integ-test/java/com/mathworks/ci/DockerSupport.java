package com.mathworks.ci;

import hudson.FilePath;
import hudson.model.WorkspaceBrowser;
import hudson.matrix.*;
import hudson.model.AbstractBuild;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import org.jenkinsci.plugins.workflow.actions.WorkspaceAction;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.cps.nodes.StepStartNode;
import org.jenkinsci.plugins.workflow.flow.FlowExecution;
import org.jenkinsci.plugins.workflow.graph.FlowGraphWalker;
import org.jenkinsci.plugins.workflow.graph.FlowNode;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.*;
import org.jvnet.hudson.test.ExtractResourceSCM;
import org.jvnet.hudson.test.JenkinsRule;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import jenkins.model.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class DockerSupport extends Thread{
    volatile private FreeStyleProject project;
    volatile MatrixProject matrixProject;
    volatile WorkflowJob pipelineProject;
    volatile private UseMatlabVersionBuildWrapper buildWrapper;
    volatile FreeStyleBuild build;
    volatile MatrixBuild matrixBuild;
    volatile WorkflowRun pipelineBuild;


    @Rule
    public JenkinsRule jenkins = new JenkinsRule();


    @Before
    public void testSetup() throws IOException {
        project = jenkins.createFreeStyleProject();
        buildWrapper = new UseMatlabVersionBuildWrapper();
    }

    @After
    public void testTearDown() {
        project = null;
        buildWrapper = null;
        pipelineProject = null;
    }

    private String getMatlabroot() throws URISyntaxException {
        String  MATLAB_ROOT;

        if (System.getProperty("os.name").startsWith("Win")) {
            MATLAB_ROOT = TestData.getPropValues("matlab.windows.installed.path");
            // Prints the root folder of MATLAB
            System.out.println(MATLAB_ROOT);
        }
        else if (System.getProperty("os.name").startsWith("Linux")){
            MATLAB_ROOT = TestData.getPropValues("matlab.linux.installed.path");
            // Prints the root folder of MATLAB
            System.out.println(MATLAB_ROOT);
        }
        else {
            MATLAB_ROOT = TestData.getPropValues("matlab.mac.installed.path");
            // Prints the root folder of MATLAB
            System.out.println(MATLAB_ROOT);
        }
        return MATLAB_ROOT;
    }

    /*
     * Utility function which returns the build of the project
     */
    private WorkflowRun getPipelineBuild(String script) throws Exception{
        pipelineProject.setDefinition(new CpsFlowDefinition(script,true));
        return pipelineProject.scheduleBuild2(0).get();
    }

    private void verifySourceFilesFolderStructure(FilePath projectWorkspace) throws Exception {
        // assert it has only folder .matlab
//        assertEquals(projectWorkspace.list().size(), 1);
        assertEquals(projectWorkspace.listDirectories().get(0).getName(), ".matlab");
        // Assert it has only one folder inside it
        assertEquals(projectWorkspace.listDirectories().get(0).list().size(), 1);

        // assert inside .matlab folder there is only one folder of name size 8 and with a.lpha numeric charecters
        FilePath randomBuildFolderPath = projectWorkspace.listDirectories().get(0).listDirectories().get(0);
        assertEquals(randomBuildFolderPath.getName().length(), 8);
        assertTrue(StringUtils.isAlphanumeric(randomBuildFolderPath.getName()));
    }

    private boolean verifyFolderHasFile(FilePath sourceFilePath, String fileName) throws Exception {
        List<FilePath> sourceFiles = sourceFilePath.list();
        for(int i=0;i<sourceFiles.size();i++){
            if(sourceFiles.get(i).getName().contains(fileName)){
                return true;
            }
        }
        return false;
    }


    private void verifyCommandScriptSourceFolder(FilePath projectWorkspace) throws Exception {
        FilePath sourceFilePath = projectWorkspace.listDirectories().get(0).listDirectories().get(0);


        assertTrue(verifyFolderHasFile(sourceFilePath, "command_"));
        assertTrue(verifyFolderHasFile(sourceFilePath, "run_matlab_command"));
    }

    private void verifyTestsScriptFolder(FilePath projectWorkspace) throws Exception {
        FilePath sourceFilePath = projectWorkspace.listDirectories().get(0).listDirectories().get(0);
        assertTrue(verifyFolderHasFile(sourceFilePath, "runner_"));
        assertTrue(verifyFolderHasFile(sourceFilePath, "genscript"));
        assertTrue(verifyFolderHasFile(sourceFilePath, "run_matlab_command"));
    }

    private  void verifyCommandScriptInvokledPath(FilePath commandScriptFilePath, AbstractBuild<?,?> build) throws Exception {
        jenkins.assertLogContains(".matlab", build);
        jenkins.assertLogNotContains("tmp", build);
        jenkins.assertLogContains(commandScriptFilePath.getParent().getName(), build);
        // Removing .m extesion
        String commandScriptName = commandScriptFilePath.getName();
        commandScriptName = commandScriptName.substring(0, commandScriptName.length() - 2);
        jenkins.assertLogContains(commandScriptName, build);
        // Removing 'command_'
        assertTrue(StringUtils.isAlphanumeric(commandScriptName.substring(8)));
    }

    private  void verifyCommandScriptInvokledPath(FilePath commandScriptFilePath, WorkflowRun build) throws Exception {
        jenkins.assertLogContains(".matlab", build);
        jenkins.assertLogNotContains("tmp", build);
        jenkins.assertLogContains(commandScriptFilePath.getParent().getName(), build);
        // Removing .m extesion
        String commandScriptName = commandScriptFilePath.getName();
        commandScriptName = commandScriptName.substring(0, commandScriptName.length() - 2);
        jenkins.assertLogContains(commandScriptName, build);
        // Removing 'command_'
        assertTrue(StringUtils.isAlphanumeric(commandScriptName.substring(8)));
    }

    private  void verifyTestScriptInvokledPath(FilePath runnerScriptFilePath, AbstractBuild<?,?> build) throws Exception {
        jenkins.assertLogContains(".matlab", build);
        jenkins.assertLogNotContains("tmp", build);
        jenkins.assertLogContains(runnerScriptFilePath.getParent().getName(), build);
        String runnerScriptName = runnerScriptFilePath.getName();
        runnerScriptName = runnerScriptName.substring(0, runnerScriptName.length() - 2);
        jenkins.assertLogContains(runnerScriptName, build);
    }
    private  void verifyTestScriptInvokledPath(FilePath runnerScriptFilePath, WorkflowRun build) throws Exception {
        jenkins.assertLogContains(".matlab", build);
        jenkins.assertLogNotContains("tmp", build);
        jenkins.assertLogContains(runnerScriptFilePath.getParent().getName(), build);
        String runnerScriptName = runnerScriptFilePath.getName();
        runnerScriptName = runnerScriptName.substring(0, runnerScriptName.length() - 2);
        jenkins.assertLogContains(runnerScriptName, build);
    }

    private void verifyDotMATLABFolderIsEmptyAfterBuild(FilePath projectWorkspace) throws Exception {
        assertEquals(projectWorkspace.listDirectories().get(0).getName(), ".matlab");
        // Assert it has only one folder inside it
        assertEquals(projectWorkspace.listDirectories().get(0).list().size(), 0);
    }

    FilePath getCommandScriptFilePath(FilePath projectWorkspace) throws IOException, InterruptedException {
        return projectWorkspace.list().get(0).list().get(0).list().get(0);
    }

    FilePath getrunnerScriptFilePath(FilePath projectWorkspace) throws IOException, InterruptedException {
        List<FilePath> sourceFiles = projectWorkspace.listDirectories().get(0).listDirectories().get(0).list();
        return sourceFiles.get(sourceFiles.size()-1);
    }

    /*
     * Test to verify if Build FAILS when matlab command fails
     */

    @Test
    public void verifyFreeStyleCommandSourceFiles() throws Exception {
        FilePath commandScriptFilePath;

        String matlabRoot = getMatlabroot();
        buildWrapper.setMatlabBuildWrapperContent(new MatlabBuildWrapperContent(Message.getValue("matlab.custom.location"), matlabRoot));
//        this.buildWrapper.setMatlabRootFolder(matlabRoot);
        project.getBuildWrappersList().add(buildWrapper);
        RunMatlabCommandBuilder tester =
                new RunMatlabCommandBuilder();
        tester.setMatlabCommand("pwd");
        project.getBuildersList().add(tester);
        Thread thread = new Thread("New Thread") {
            public void run(){
                try {
                    build = project.scheduleBuild2(0).get();

                    jenkins.assertBuildStatus(Result.SUCCESS, build);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        thread.start();
        Thread.sleep(200);
        thread.suspend();

        FilePath projectWorkspace = project.getWorkspace();
        verifySourceFilesFolderStructure(projectWorkspace);
        verifyCommandScriptSourceFolder(projectWorkspace);
        commandScriptFilePath = getCommandScriptFilePath(projectWorkspace);

        // Add check for alpha numeric names

//        thread.suspend();
        thread.resume();
        thread.join();
        verifyCommandScriptInvokledPath(commandScriptFilePath, build);
        verifyDotMATLABFolderIsEmptyAfterBuild(projectWorkspace);
        String build_log = jenkins.getLog(build);


        String build_log1 = jenkins.getLog(build);
    }

    @Test
    public void verifyFreeStyleTestsSourceFiles() throws Exception {
        FilePath runnerScriptFilePath;
        this.buildWrapper.setMatlabBuildWrapperContent(new MatlabBuildWrapperContent(Message.getValue("matlab.custom.location"), getMatlabroot()));
        project.getBuildWrappersList().add(this.buildWrapper);
        project.setScm(new ExtractResourceSCM(getClass().getResource("FilterTestData.zip")));

        RunMatlabTestsBuilder testingBuilder = new RunMatlabTestsBuilder();
        // Adding list of source folder
        List<SourceFolderPaths> list=new ArrayList<SourceFolderPaths>();
        list.add(new SourceFolderPaths("src"));
        testingBuilder.setSourceFolder(new SourceFolder(list));

        project.getBuildersList().add(testingBuilder);

        Thread thread = new Thread("Build Thread") {
            public void run(){
                try {
                    build = project.scheduleBuild2(0).get();

                    jenkins.assertBuildStatus(Result.SUCCESS, build);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        thread.start();
        Thread.sleep(1000);
        thread.suspend();

        FilePath projectWorkspace = project.getWorkspace();
        verifySourceFilesFolderStructure(projectWorkspace);
        verifyTestsScriptFolder(projectWorkspace);
        runnerScriptFilePath = getrunnerScriptFilePath(projectWorkspace);


        thread.resume();
        thread.join();

        verifyTestScriptInvokledPath(runnerScriptFilePath, build);
        verifyDotMATLABFolderIsEmptyAfterBuild(projectWorkspace);
    }
//
//    @Test
//    public void
    @Test
    public void verifyMatrixCommandSourceFiles() throws Exception {
        matrixProject = jenkins.createProject(MatrixProject.class);
        Axis axes = new Axis("VERSION", "R2020b", "R2020a");
        matrixProject.setAxes(new AxisList(axes));
        String matlabRoot = getMatlabroot();
        this.buildWrapper.setMatlabBuildWrapperContent(new MatlabBuildWrapperContent(Message.getValue("matlab.custom.location"), matlabRoot.replace("R2020b", "$VERSION")));
    //        this.buildWrapper.setMatlabRootFolder(matlabRoot.replace(TestData.getPropValues("matlab.version"), "$VERSION"));
        matrixProject.getBuildWrappersList().add(this.buildWrapper);
        RunMatlabCommandBuilder tester = new RunMatlabCommandBuilder();

        tester.setMatlabCommand("pwd,version");
        matrixProject.getBuildersList().add(tester);
        Thread thread = new Thread("New Thread") {
            public void run(){
                try {
                    matrixBuild = matrixProject.scheduleBuild2(0).get();
                    jenkins.assertBuildStatus(Result.SUCCESS, matrixBuild);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        thread.start();
        Thread.sleep(500);
        thread.suspend();


        FilePath projectWorkspace = matrixProject.getWorkspace();
        // verify folder structure and source files path for first run
        FilePath firstRunWorkspace = projectWorkspace.listDirectories().get(0).listDirectories().get(0);
        verifySourceFilesFolderStructure(firstRunWorkspace);
        verifyCommandScriptSourceFolder(firstRunWorkspace);

        // verify folder structure and source files path for second run
        FilePath secondRunWorkspace = projectWorkspace.listDirectories().get(0).listDirectories().get(1);
        verifySourceFilesFolderStructure(secondRunWorkspace);
        verifyCommandScriptSourceFolder(secondRunWorkspace);

        // Command script path for first run
        FilePath firstRunCommandScriptFilePath = getCommandScriptFilePath(firstRunWorkspace);

        // Command script path for second run
        FilePath secondRunCommandScriptFilePath = getCommandScriptFilePath(secondRunWorkspace);

        // Completing the build
        thread.resume();
        thread.join();

        List<MatrixRun> runs = matrixBuild.getRuns();
        // verify source files invoked path for first run
        verifyCommandScriptInvokledPath(firstRunCommandScriptFilePath, runs.get(0));
        verifyDotMATLABFolderIsEmptyAfterBuild(firstRunWorkspace);
        // verify source files invoked path for second run
        verifyCommandScriptInvokledPath(secondRunCommandScriptFilePath, runs.get(1));
        verifyDotMATLABFolderIsEmptyAfterBuild(secondRunWorkspace);


        String build_log = jenkins.getLog(matrixBuild);

        jenkins.assertLogContains("R2020b completed", matrixBuild);
        jenkins.assertLogContains("R2020a completed", matrixBuild);

        jenkins.assertBuildStatus(Result.SUCCESS, matrixBuild);
    }

    @Test
    public void verifyMatrixTestsSourceFiles() throws Exception {
        matrixProject = jenkins.createProject(MatrixProject.class);
        Axis axes = new Axis("MATLAB_VERSION", "R2020b", "R2020a");
        matrixProject.setAxes(new AxisList(axes));
        String matlabRoot = getMatlabroot();
        this.buildWrapper.setMatlabBuildWrapperContent(new MatlabBuildWrapperContent(Message.getValue("matlab.custom.location"), matlabRoot.replace("R2020b", "$MATLAB_VERSION")));
        //        this.buildWrapper.setMatlabRootFolder(matlabRoot.replace(TestData.getPropValues("matlab.version"), "$VERSION"));
        matrixProject.getBuildWrappersList().add(this.buildWrapper);
        matrixProject.setScm(new ExtractResourceSCM(getClass().getResource("FilterTestData.zip")));

        RunMatlabTestsBuilder testingBuilder = new RunMatlabTestsBuilder();
        // Adding list of source folder
        List<SourceFolderPaths> list=new ArrayList<SourceFolderPaths>();
        list.add(new SourceFolderPaths("src"));
        testingBuilder.setSourceFolder(new SourceFolder(list));

        matrixProject.getBuildersList().add(testingBuilder);
        Thread thread = new Thread("New Thread") {
            public void run(){
                try {
                    matrixBuild = matrixProject.scheduleBuild2(0).get();
                    jenkins.assertBuildStatus(Result.SUCCESS, matrixBuild);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        thread.start();
        Thread.sleep(2000);
        thread.suspend();


        FilePath projectWorkspace = matrixProject.getWorkspace();
        // verify folder structure and source files path for first run
        FilePath firstRunWorkspace = projectWorkspace.listDirectories().get(0).listDirectories().get(0);
        verifySourceFilesFolderStructure(firstRunWorkspace);
        verifyTestsScriptFolder(firstRunWorkspace);

        // verify folder structure and source files path for second run
        FilePath secondRunWorkspace = projectWorkspace.listDirectories().get(0).listDirectories().get(1);
        verifySourceFilesFolderStructure(secondRunWorkspace);
        verifyTestsScriptFolder(secondRunWorkspace);

        // Command script path for first run
        FilePath firstRunTestsScriptFilePath = getrunnerScriptFilePath(firstRunWorkspace);

        // Command script path for second run
        FilePath secondRunTestsScriptFilePath = getrunnerScriptFilePath(secondRunWorkspace);

        // Completing the build
        thread.resume();
        thread.join();

        List<MatrixRun> runs = matrixBuild.getRuns();
        // verify source files invoked path for first run
        verifyTestScriptInvokledPath(firstRunTestsScriptFilePath, runs.get(0));
        verifyDotMATLABFolderIsEmptyAfterBuild(firstRunWorkspace);
        // verify source files invoked path for second run
        verifyTestScriptInvokledPath(secondRunTestsScriptFilePath, runs.get(1));
        verifyDotMATLABFolderIsEmptyAfterBuild(secondRunWorkspace);

        jenkins.assertLogContains("R2020b completed", matrixBuild);
        jenkins.assertLogContains("R2020a completed", matrixBuild);

        jenkins.assertBuildStatus(Result.SUCCESS, matrixBuild);
    }

    @Test
    public void verifyDockerSupport() throws Exception {
        Assume.assumeTrue
                (System.getProperty("os.name").toLowerCase().startsWith("linux"));
        pipelineProject = jenkins.createProject(WorkflowJob.class);
        URL zipFile = getClass().getResource("FilterTestData.zip");
        String script = "pipeline {\n" +
                "    agent {\n" +
                "        docker {\n" +
                "            image 'mathworks/matlab'\n" +
                "            args \"--entrypoint='' -v /home/vkayithi/license:/opt/matlab/R2021a/licenses \"\n" +
                "        }\n" +
                "    }\n" +
                "    stages {\n" +
                "        stage('Test') {\n" +
                "            steps {\n" +
                "                unzip '" + zipFile.getPath() + "'" + "\n" +
                "                runMATLABCommand 'ver;pwd'\n" +
                "                runMATLABTests(sourceFolder: ['src'], testResultsPDF:'test-results/results.pdf')\n" +
                "                //sh 'pwd'\n" +
                "            }\n" +
                "        }\n" +
                "    }\n" +
                "}";
        WorkflowRun build = getPipelineBuild(script);
        jenkins.assertBuildStatus(Result.SUCCESS,build);
    }

    @Test
    public void verifyPipelineCommandSourceFiles() throws Exception {
//        WorkspaceBrowser w =
        FilePath commandScriptFilePath;
        pipelineProject = jenkins.createProject(WorkflowJob.class);
        URL zipFile = getClass().getResource("FilterTestData.zip");
        String script = "pipeline {\n" +
                "    agent any\n" +
                MatlabRootSetup.getEnvironmentDSL() + "\n" +
                "    stages {\n" +
                "        stage('Test') {\n" +
                "            steps {\n" +
                "                runMATLABCommand 'pwd'\n" +
                "            }\n" +
                "        }\n" +
                "    }\n" +
                "}";


        Thread thread = new Thread("New Thread") {
            public void run(){
                try {
                    pipelineBuild = getPipelineBuild(script);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        thread.start();
        Thread.sleep(12000);
        thread.suspend();

        FilePath projectWorkspace = jenkins.jenkins.getWorkspaceFor(pipelineProject);
        verifySourceFilesFolderStructure(projectWorkspace);
        verifyCommandScriptSourceFolder(projectWorkspace);
        commandScriptFilePath = getCommandScriptFilePath(projectWorkspace);

        thread.resume();
        thread.join();
        verifyCommandScriptInvokledPath(commandScriptFilePath, pipelineBuild);
        verifyDotMATLABFolderIsEmptyAfterBuild(projectWorkspace);
    }

    @Test
    public void verifyPipelineTestsSourceFiles() throws Exception {
        FilePath runnerScriptFilePath;
        pipelineProject = jenkins.createProject(WorkflowJob.class);
        URL zipFile = getClass().getResource("FilterTestData.zip");
        String script = "pipeline {\n" +
                "    agent any\n" +
                MatlabRootSetup.getEnvironmentDSL() + "\n" +
                " stages {\n" +
                "      stage('Test') {\n" +
                    "            steps {\n" +
                    "                unzip '" + zipFile.getPath() + "'" + "\n" +
                    "                runMATLABTests(sourceFolder: ['src'], testResultsPDF:'test-results/results.pdf')\n" +
                    "                //sh 'pwd'\n" +
                    "            }\n" +
                    "        }\n" +
                    "    }\n" +
                "}";


        Thread thread = new Thread("New Thread") {
            public void run(){
                try {
                    pipelineBuild = getPipelineBuild(script);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        thread.start();
        Thread.sleep(15000);
        thread.suspend();

        FilePath projectWorkspace = jenkins.jenkins.getWorkspaceFor(pipelineProject);
        verifySourceFilesFolderStructure(projectWorkspace);
        verifyTestsScriptFolder(projectWorkspace);
        runnerScriptFilePath = getrunnerScriptFilePath(projectWorkspace);

        thread.resume();
        thread.join();
        verifyTestScriptInvokledPath(runnerScriptFilePath, pipelineBuild);
        verifyDotMATLABFolderIsEmptyAfterBuild(projectWorkspace);
    }
}