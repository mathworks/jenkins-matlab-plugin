package com.mathworks.ci;

import com.sun.akuma.CLibrary;
import hudson.FilePath;
import hudson.model.WorkspaceBrowser;
import hudson.matrix.*;
import hudson.model.AbstractBuild;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import org.apache.commons.lang.ObjectUtils;
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
import java.util.*;

import org.apache.commons.lang3.StringUtils;
import jenkins.model.*;

import static org.junit.Assert.*;

public class DockerSupportIT extends Thread{
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

    /*
     * Utility function which returns the build of the project
     */
    private WorkflowRun getPipelineBuild(String script) throws Exception{
        pipelineProject.setDefinition(new CpsFlowDefinition(script,true));
        return pipelineProject.scheduleBuild2(0).get();
    }

    /*
     * Utility function to check if a directory has a given folder
     */
    private boolean verifyFilePathContainsFolder(FilePath projectWorkspace, String folderName) throws Exception {
        List<FilePath> allFolders = projectWorkspace.listDirectories();
        for(int i=0;i<allFolders.size();i++){
            if(allFolders.get(i).getName().equalsIgnoreCase(folderName)){
                return true;
            }
        }
        return false;
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

    /*
     * Utility function to get path of a file from folder
     */
    private FilePath getFilePathOfFileFromWorkspace(FilePath projectWorkspace, String fileName) throws Exception{
        FilePath filePathOfFile = null;
        List<FilePath> allFolders = projectWorkspace.list();
        for(int i=0;i<allFolders.size();i++){
            if(allFolders.get(i).getName().contains(fileName)){
                filePathOfFile = allFolders.get(i);
                break;
            }
        }
        assertNotNull(filePathOfFile);
        return filePathOfFile;
    }

    private void verifySourceFilesFolderStructure(FilePath projectWorkspace) throws Exception {
        // Project workspace should have a folder .matlab
        verifyFolderHasFile(projectWorkspace, ".matlab");
        // Get the path of .matlab folder
        FilePath dotMATLABPath = getFilePathOfFileFromWorkspace(projectWorkspace, ".matlab");

        // .matlab folder should have only one folder
        assertEquals(dotMATLABPath.list().size(), 1);
        // Folder where all the MATLAB scripts from genscript are stored
        FilePath MATLABScriptsFolderPath = dotMATLABPath.listDirectories().get(0);

        // assert inside .matlab folder there is only one folder of name size 8 and with alpha numeric charecters
        assertEquals(MATLABScriptsFolderPath.getName().length(), 8);
        assertTrue(StringUtils.isAlphanumeric(MATLABScriptsFolderPath.getName()));
    }
    private void verifyCommandScriptSourceFolder(FilePath projectWorkspace) throws Exception {
        verifySourceFilesFolderStructure(projectWorkspace);
        FilePath MATLABScriptsFolderPath = getFilePathOfFileFromWorkspace(projectWorkspace, ".matlab").listDirectories().get(0);

        assertTrue(verifyFolderHasFile(MATLABScriptsFolderPath, "command_"));
        assertTrue(verifyFolderHasFile(MATLABScriptsFolderPath, "run_matlab_command"));
    }

    private void verifyTestsScriptFolder(FilePath projectWorkspace) throws Exception {
        verifySourceFilesFolderStructure(projectWorkspace);
        FilePath MATLABScriptsFolderPath = getFilePathOfFileFromWorkspace(projectWorkspace, ".matlab").listDirectories().get(0);

        assertTrue(verifyFolderHasFile(MATLABScriptsFolderPath, "runner_"));
        assertTrue(verifyFolderHasFile(MATLABScriptsFolderPath, "genscript"));
        assertTrue(verifyFolderHasFile(MATLABScriptsFolderPath, "run_matlab_command"));
    }

    String getFileNameWithoutExt(FilePath scriptFilePath){
        String fileFullName = scriptFilePath.getName();
        return fileFullName.substring(0, fileFullName.length()-2);
    }

    /*
     * Utility function to verify the command/tests script file is invoked from inside ".matlab" folder
     */
    private  void verifyScriptInvokledPath(FilePath scriptFilePath, AbstractBuild<?,?> build) throws Exception {
        jenkins.assertLogContains(".matlab", build);
        jenkins.assertLogContains(scriptFilePath.getParent().getName(), build);
        String scriptFileName = getFileNameWithoutExt(scriptFilePath);
        jenkins.assertLogContains(scriptFileName, build);
        // Removing 'command_/runner_'
//        assertTrue(StringUtils.isAlphanumeric(scriptName.substring(8)));
    }

    private  void verifyScriptInvokledPath(FilePath scriptFilePath, WorkflowRun build) throws Exception {
        jenkins.assertLogContains(".matlab", build);
        jenkins.assertLogContains(scriptFilePath.getParent().getName(), build);
        String scriptFileName = getFileNameWithoutExt(scriptFilePath);
        jenkins.assertLogContains(scriptFileName, build);
        // Removing 'command_/runner_'
//        assertTrue(StringUtils.isAlphanumeric(scriptName.substring(8)));
    }

    MatrixRun getMatrixRunFromMatrixBuild(MatrixBuild matrixBuild, String axesName, String runName){
        Map<String, String> vals = new HashMap<String, String>();
        vals.put(axesName, runName);
        Combination c = new Combination(vals);
        return matrixBuild.getRun(c);
    }

//    private  void verifyTestScriptInvokledPath(FilePath runnerScriptFilePath, AbstractBuild<?,?> build) throws Exception {
//        jenkins.assertLogContains(".matlab", build);
//        jenkins.assertLogContains(runnerScriptFilePath.getParent().getName(), build);
//        String runnerScriptName = runnerScriptFilePath.getName();
//        runnerScriptName = runnerScriptName.substring(0, runnerScriptName.length() - 2);
//        jenkins.assertLogContains(runnerScriptName, build);
//    }
//    private  void verifyTestScriptInvokledPath(FilePath runnerScriptFilePath, WorkflowRun build) throws Exception {
//        jenkins.assertLogContains(".matlab", build);
//        jenkins.assertLogContains(runnerScriptFilePath.getParent().getName(), build);
//        String runnerScriptName = runnerScriptFilePath.getName();
//        runnerScriptName = runnerScriptName.substring(0, runnerScriptName.length() - 2);
//        jenkins.assertLogContains(runnerScriptName, build);
//    }
    /*
     * CHeck after the build, the ".matlab" folder is still present and is empty
     */
    private void verifyDotMATLABFolderIsEmptyAfterBuild(FilePath projectWorkspace) throws Exception {
        assertTrue(verifyFilePathContainsFolder(projectWorkspace, ".matlab"));
        // Assert it has only one folder inside it
        assertEquals(projectWorkspace.listDirectories().get(0).list().size(), 0);
    }

    FilePath getMATLABCommandScriptFilePath(FilePath projectWorkspace) throws Exception {
        FilePath dotMATLABPath = getFilePathOfFileFromWorkspace(projectWorkspace, ".matlab");
        FilePath MATLABscriptFilePath = getFilePathOfFileFromWorkspace(dotMATLABPath.listDirectories().get(0), "command_");
        return MATLABscriptFilePath;
    }

    FilePath getMATLABrunnerScriptFilePath(FilePath projectWorkspace) throws Exception {
        FilePath dotMATLABPath = getFilePathOfFileFromWorkspace(projectWorkspace, ".matlab");
        FilePath MATLABscriptFilePath = getFilePathOfFileFromWorkspace(dotMATLABPath.listDirectories().get(0), "runner_");
        return MATLABscriptFilePath;
    }

    boolean verifyMATLABScriptsFolderIsCreated(FilePath projectWorkspace) throws Exception {
        File dotMATLABFolder = new File(projectWorkspace.getRemote() + File.separator + ".matlab");

        // Check .matlab folder is generated
        if(!dotMATLABFolder.exists()){
            return false;
        }
        FilePath dotMATLABFolderPath = getFilePathOfFileFromWorkspace(projectWorkspace, ".matlab");
        // Check a folder is created to store the MATLAB scripts
        if(Objects.requireNonNull(dotMATLABFolder.list()).length == 0){
            return false;
        }
        // Check required MATLAB scripts are created in the folder
        FilePath MATLABScriptsFolderPath = dotMATLABFolderPath.listDirectories().get(0);
        if(MATLABScriptsFolderPath.list().size() == 0){
            return false;
        }
        return  true;
    }

    private boolean checkMatrixRunWorkspacesAreCreated(FilePath projectWorkspace) throws IOException, InterruptedException {
        if(projectWorkspace.listDirectories().size() == 0){
            return false;
        }
        if(projectWorkspace.listDirectories().get(0).listDirectories().size() == 0){
            return false;
        }
        return true;
    }

    /*
     * Test to verify if Build FAILS when matlab command fails
     */

    @Test
    public void verifyFreeStyleCommandSourceFiles() throws Exception {
        buildWrapper.setMatlabBuildWrapperContent(new MatlabBuildWrapperContent(Message.getValue("matlab.custom.location"), MatlabRootSetup.getMatlabRoot()));
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
        FilePath projectWorkspace = jenkins.jenkins.getWorkspaceFor(project);
        assert projectWorkspace != null;
        while(!verifyMATLABScriptsFolderIsCreated(projectWorkspace)){
            Thread.sleep(100);
        }

        thread.suspend();

        verifyCommandScriptSourceFolder(projectWorkspace);
        FilePath commnadMATLABScriptFilePath = getMATLABCommandScriptFilePath(projectWorkspace);

        thread.resume();
        thread.join();

        verifyScriptInvokledPath(commnadMATLABScriptFilePath, build);
        verifyDotMATLABFolderIsEmptyAfterBuild(projectWorkspace);
    }

    @Test
    public void verifyFreeStyleTestsSourceFiles() throws Exception {
        FilePath runnerScriptFilePath;
        this.buildWrapper.setMatlabBuildWrapperContent(new MatlabBuildWrapperContent(Message.getValue("matlab.custom.location"),MatlabRootSetup.getMatlabRoot()));
        project.getBuildWrappersList().add(this.buildWrapper);
        project.setScm(new ExtractResourceSCM(MatlabRootSetup.getRunMATLABTestsData()));

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
        FilePath projectWorkspace =jenkins.jenkins.getWorkspaceFor(project);

        thread.start();
        while(!verifyMATLABScriptsFolderIsCreated(projectWorkspace)){
            Thread.sleep(1000);
        }
        thread.suspend();

        verifyTestsScriptFolder(projectWorkspace);
        FilePath runnerMATLABScriptName = getMATLABrunnerScriptFilePath(projectWorkspace);

        thread.resume();
        thread.join();

        verifyScriptInvokledPath(runnerMATLABScriptName, build);
        verifyDotMATLABFolderIsEmptyAfterBuild(projectWorkspace);
    }
//
    @Test
    public void verifyMatrixCommandSourceFiles() throws Exception {
        matrixProject = jenkins.createProject(MatrixProject.class);
        Axis axes = new Axis("MATLAB_VERSION", "R2020b", "R2020a");
        matrixProject.setAxes(new AxisList(axes));
        String matlabRoot = MatlabRootSetup.getMatlabRoot();
        this.buildWrapper.setMatlabBuildWrapperContent(new MatlabBuildWrapperContent(Message.getValue("matlab.custom.location"), matlabRoot.replace("R2020b", "$MATLAB_VERSION")));
    //        this.buildWrapper.setMatlabRootFolder(matlabRoot.replace(TestData.getPropValues("matlab.version"), "$VERSION"));
        matrixProject.getBuildWrappersList().add(this.buildWrapper);
        RunMatlabCommandBuilder tester = new RunMatlabCommandBuilder();

        tester.setMatlabCommand("pwd,version");
        matrixProject.getBuildersList().add(tester);
        Thread thread = new Thread("New Thread") {
            public void run(){
                try {
                    matrixBuild = matrixProject.scheduleBuild2(0).get();
//                    jenkins.assertBuildStatus(Result.SUCCESS, matrixBuild);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        thread.start();
        FilePath projectWorkspace =jenkins.jenkins.getWorkspaceFor(matrixProject);
        while(!checkMatrixRunWorkspacesAreCreated(projectWorkspace)){
            Thread.sleep(1000);
        }
        projectWorkspace = getFilePathOfFileFromWorkspace(projectWorkspace, "MATLAB_VERSION");
        FilePath firstRunWorkspace = getFilePathOfFileFromWorkspace(projectWorkspace, "R2020b");
        FilePath secondRunWorkspace = getFilePathOfFileFromWorkspace(projectWorkspace, "R2020a");

        while(!verifyMATLABScriptsFolderIsCreated(firstRunWorkspace) && !verifyMATLABScriptsFolderIsCreated(secondRunWorkspace)){
            Thread.sleep(1000);
        }
        thread.suspend();


        // verify folder structure and source files path for first run
        verifyCommandScriptSourceFolder(firstRunWorkspace);

        // verify folder structure and source files path for second run
        verifyCommandScriptSourceFolder(secondRunWorkspace);

        // Command script path for first run
        FilePath firstRunCommandScriptFilePath = getMATLABCommandScriptFilePath(firstRunWorkspace);

        // Command script path for second run
        FilePath secondRunCommandScriptFilePath = getMATLABCommandScriptFilePath(secondRunWorkspace);

        // Completing the build
        thread.resume();
        thread.join();

        // verify source files invoked path for first run
        verifyScriptInvokledPath(firstRunCommandScriptFilePath, getMatrixRunFromMatrixBuild(matrixBuild,"MATLAB_VERSION", "R2020b"));
        verifyDotMATLABFolderIsEmptyAfterBuild(firstRunWorkspace);

        // verify source files invoked path for second run
        verifyScriptInvokledPath(secondRunCommandScriptFilePath,  getMatrixRunFromMatrixBuild(matrixBuild,"MATLAB_VERSION", "R2020a"));
        verifyDotMATLABFolderIsEmptyAfterBuild(secondRunWorkspace);


        jenkins.assertLogContains("R2020b completed", matrixBuild);
        jenkins.assertLogContains("R2020a completed", matrixBuild);

        jenkins.assertBuildStatus(Result.SUCCESS, matrixBuild);
    }


    @Test
    public void verifyMatrixTestsSourceFiles() throws Exception {
        matrixProject = jenkins.createProject(MatrixProject.class);
        Axis axes = new Axis("MATLAB_VERSION", "R2020b", "R2020a");
        matrixProject.setAxes(new AxisList(axes));
        String matlabRoot = MatlabRootSetup.getMatlabRoot();
        this.buildWrapper.setMatlabBuildWrapperContent(new MatlabBuildWrapperContent(Message.getValue("matlab.custom.location"), matlabRoot.replace("R2020b", "$MATLAB_VERSION")));
        //        this.buildWrapper.setMatlabRootFolder(matlabRoot.replace(TestData.getPropValues("matlab.version"), "$VERSION"));
        matrixProject.getBuildWrappersList().add(this.buildWrapper);
        matrixProject.setScm(new ExtractResourceSCM(MatlabRootSetup.getRunMATLABTestsData()));

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
        FilePath projectWorkspace =jenkins.jenkins.getWorkspaceFor(matrixProject);
        while(!checkMatrixRunWorkspacesAreCreated(projectWorkspace)){
            Thread.sleep(1000);
        }

        projectWorkspace = getFilePathOfFileFromWorkspace(projectWorkspace, "MATLAB_VERSION");
        FilePath firstRunWorkspace = getFilePathOfFileFromWorkspace(projectWorkspace, "R2020b");
        FilePath secondRunWorkspace = getFilePathOfFileFromWorkspace(projectWorkspace, "R2020a");

        while(!verifyMATLABScriptsFolderIsCreated(firstRunWorkspace) && !verifyMATLABScriptsFolderIsCreated(secondRunWorkspace)){
            Thread.sleep(1000);
        }
        thread.suspend();


        // verify folder structure and source files path for first run
        verifySourceFilesFolderStructure(firstRunWorkspace);
        verifyTestsScriptFolder(firstRunWorkspace);

        // verify folder structure and source files path for second run

        verifySourceFilesFolderStructure(secondRunWorkspace);
        verifyTestsScriptFolder(secondRunWorkspace);

        // Command script path for first run
        FilePath firstRunTestsScriptName = getMATLABrunnerScriptFilePath(firstRunWorkspace);

        // Command script path for second run
        FilePath secondRunTestsScriptName = getMATLABrunnerScriptFilePath(secondRunWorkspace);

        // Completing the build
        thread.resume();
        thread.join();

        // verify source files invoked path for first run
        verifyScriptInvokledPath(firstRunTestsScriptName, getMatrixRunFromMatrixBuild(matrixBuild,"MATLAB_VERSION", "R2020b"));
        verifyDotMATLABFolderIsEmptyAfterBuild(firstRunWorkspace);

        // verify source files invoked path for second run
        verifyScriptInvokledPath(secondRunTestsScriptName, getMatrixRunFromMatrixBuild(matrixBuild,"MATLAB_VERSION", "R2020a"));
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
        URL zipFile = MatlabRootSetup.getRunMATLABTestsData();
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
        URL zipFile = MatlabRootSetup.getRunMATLABTestsData();
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
        FilePath projectWorkspace =jenkins.jenkins.getWorkspaceFor(pipelineProject);

        thread.start();
        while(!verifyMATLABScriptsFolderIsCreated(projectWorkspace)){
            Thread.sleep(12000);
        }
        thread.suspend();

        verifyCommandScriptSourceFolder(projectWorkspace);
        FilePath MATLABCommandScriptFilePath = getMATLABCommandScriptFilePath(projectWorkspace);

        thread.resume();
        thread.join();
        verifyScriptInvokledPath(MATLABCommandScriptFilePath, pipelineBuild);
        verifyDotMATLABFolderIsEmptyAfterBuild(projectWorkspace);
    }

    @Test
    public void verifyPipelineTestsSourceFiles() throws Exception {
        FilePath runnerScriptFilePath;
        pipelineProject = jenkins.createProject(WorkflowJob.class);
        URL zipFile = MatlabRootSetup.getRunMATLABTestsData();
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
        FilePath projectWorkspace =jenkins.jenkins.getWorkspaceFor(pipelineProject);

        thread.start();
        while(!verifyMATLABScriptsFolderIsCreated(projectWorkspace)){
            Thread.sleep(12000);
        }
        thread.suspend();

        verifyTestsScriptFolder(projectWorkspace);
        FilePath runnerMATLABScriptFilePath = getMATLABrunnerScriptFilePath(projectWorkspace);

        thread.resume();
        thread.join();
        verifyScriptInvokledPath(runnerMATLABScriptFilePath, pipelineBuild);
        verifyDotMATLABFolderIsEmptyAfterBuild(projectWorkspace);
    }
}