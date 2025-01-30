package com.mathworks.ci.systemTests;

import com.mathworks.ci.MatlabBuildWrapperContent;
import com.mathworks.ci.MatlabInstallationAxis;
import com.mathworks.ci.Message;
import com.mathworks.ci.UseMatlabVersionBuildWrapper;
import com.mathworks.ci.freestyle.RunMatlabBuildBuilder;
import com.mathworks.ci.freestyle.RunMatlabCommandBuilder;
import com.mathworks.ci.freestyle.RunMatlabTestsBuilder;
import com.mathworks.ci.freestyle.options.*;
import hudson.FilePath;
import hudson.matrix.*;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.*;
import org.junit.rules.Timeout;
import org.jvnet.hudson.test.ExtractResourceSCM;
import org.jvnet.hudson.test.JenkinsRule;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertTrue;

public class StartupOptionsIT {
    volatile private FreeStyleProject project;
    volatile WorkflowJob pipelineProject;
    private String envScripted;
    private String envDSL;
    volatile private UseMatlabVersionBuildWrapper buildWrapper;
    volatile FreeStyleBuild build;

    @Rule
    public Timeout timeout = Timeout.seconds(0);

    @Rule
    public JenkinsRule jenkins = new JenkinsRule();

    @BeforeClass
    public static void checkMatlabRoot() {
        // Check if the MATLAB_ROOT environment variable is defined
        String matlabRoot = System.getenv("MATLAB_ROOT");
        Assume.assumeTrue("Not running tests as MATLAB_ROOT environment variable is not defined", matlabRoot != null && !matlabRoot.isEmpty());
    }

    @Before
    public void testSetup() throws IOException {
        project = jenkins.createFreeStyleProject();
        pipelineProject = jenkins.createProject(WorkflowJob.class);
        buildWrapper = new UseMatlabVersionBuildWrapper();
        this.envDSL = Utilities.getEnvironmentDSL();
        this.envScripted = Utilities.getEnvironmentScriptedPipeline();
    }

    @After
    public void testTearDown() {
        project = null;
        buildWrapper = null;
        pipelineProject = null;
    }

    private WorkflowRun getPipelineBuild(String script) throws Exception{
        pipelineProject.setDefinition(new CpsFlowDefinition(script,true));
        return pipelineProject.scheduleBuild2(0).get();
    }

    @Test
    public void verifyStartupOptionsInFreeStyleProject() throws Exception {
        this.buildWrapper.setMatlabBuildWrapperContent(new MatlabBuildWrapperContent(Message.getValue("matlab.custom.location"), Utilities.getMatlabRoot()));
        project.getBuildWrappersList().add(this.buildWrapper);
        //Command Step
        RunMatlabCommandBuilder commandStep =
                new RunMatlabCommandBuilder();
        commandStep.setMatlabCommand("pwd,version");
        StartupOptions startupOptions = new StartupOptions("-logfile outputCommand.log");
        commandStep.setStartupOptions(startupOptions);
        project.getBuildersList().add(commandStep);

        //Run tests step
        project.setScm(new ExtractResourceSCM(Utilities.getRunMATLABTestsData()));
        RunMatlabTestsBuilder runTestsStep = new RunMatlabTestsBuilder();
        //Adding src folder
        List<SourceFolderPaths> list=new ArrayList<SourceFolderPaths>();
        list.add(new SourceFolderPaths("src"));
        runTestsStep.setSourceFolder(new SourceFolder(list));

        // Adding list of test folder
        List<TestFolders> testFolders = new ArrayList<TestFolders>();
        testFolders.add(new TestFolders("test/TestSquare"));
        runTestsStep.setSelectByFolder(new SelectByFolder(testFolders));
        runTestsStep.setStartupOptions(new StartupOptions("-logfile outputTests.log"));
        project.getBuildersList().add(runTestsStep);

        // Run Build step
        RunMatlabBuildBuilder buildStep = new RunMatlabBuildBuilder();
        buildStep.setTasks("check");
        buildStep.setStartupOptions(new StartupOptions("-logfile outputBuild.log"));
        project.getBuildersList().add(buildStep);

        FreeStyleBuild build = project.scheduleBuild2(0).get();

        assertTrue(new FilePath(jenkins.getInstance().getWorkspaceFor(project), "outputCommand.log").exists());
        assertTrue(new FilePath(jenkins.getInstance().getWorkspaceFor(project), "outputBuild.log").exists());
        assertTrue(new FilePath(jenkins.getInstance().getWorkspaceFor(project), "outputTests.log").exists());
        jenkins.assertBuildStatus(Result.SUCCESS, build);
    }

    @Test
    public void verifyStartupOptionsInDeclarativePipeline() throws Exception {
        String script = "pipeline {\n" +
                "  agent any\n" +
                envDSL + "\n" +
                "    stages{\n" +
                "        stage('Run MATLAB Command') {\n" +
                "            steps\n" +
                "            {\n" +
                "              unzip '" + Utilities.getRunMATLABTestsData().getPath() + "'" + "\n" +
                "              runMATLABCommand(command: 'pwd,version', startupOptions: '-logfile outputCommand.log -nojvm')\n" +
                "              runMATLABTests(sourceFolder: ['src'], testResultsJUnit: 'test-results/results.xml'," +
                "              codeCoverageCobertura: 'code-coverage/coverage.xml', startupOptions: '-logfile outputTests.log -nojvm')\n" +
                "              runMATLABBuild(tasks: 'check', startupOptions: '-logfile outputBuild.log -nojvm')" +
                "            }\n" +
                "        }\n" +
                "    }\n" +
                "}";
        WorkflowRun build = getPipelineBuild(script);
        jenkins.assertBuildStatus(Result.SUCCESS, build);
        assertTrue(new FilePath(jenkins.getInstance().getWorkspaceFor(pipelineProject), "outputCommand.log").exists());
        assertTrue(new FilePath(jenkins.getInstance().getWorkspaceFor(pipelineProject), "outputTests.log").exists());
        assertTrue(new FilePath(jenkins.getInstance().getWorkspaceFor(pipelineProject), "outputBuild.log").exists());
        jenkins.assertLogContains("-logfile outputCommand.log", build);
        jenkins.assertLogContains("-logfile outputBuild.log", build);
        jenkins.assertLogContains("-logfile outputTests.log", build);
    }

    @Test
    public void verifyStartupOptionsInScriptedPipeline() throws Exception {
        String script = "node {\n" +
                            envScripted + "\n" +
                            "              unzip '" + Utilities.getRunMATLABTestsData().getPath() + "'" + "\n" +
                            "              runMATLABCommand(command: 'pwd,version', startupOptions: '-logfile outputCommand.log -nojvm')\n" +
                            "              runMATLABTests(sourceFolder: ['src'], testResultsJUnit: 'test-results/results.xml'," +
                            "              codeCoverageCobertura: 'code-coverage/coverage.xml', startupOptions: '-logfile outputTests.log -nojvm')\n" +
                            "              runMATLABBuild(tasks: 'check', startupOptions: '-logfile outputBuild.log -nojvm')" +
                        "}";
        WorkflowRun build = getPipelineBuild(script);
        jenkins.assertBuildStatus(Result.SUCCESS, build);
        assertTrue(new FilePath(jenkins.getInstance().getWorkspaceFor(pipelineProject), "outputCommand.log").exists());
        assertTrue(new FilePath(jenkins.getInstance().getWorkspaceFor(pipelineProject), "outputTests.log").exists());
        assertTrue(new FilePath(jenkins.getInstance().getWorkspaceFor(pipelineProject), "outputBuild.log").exists());
        jenkins.assertLogContains("-logfile outputCommand.log", build);
        jenkins.assertLogContains("-logfile outputBuild.log", build);
        jenkins.assertLogContains("-logfile outputTests.log", build);
    }

    @Test
    public void verifyStartUpOptionsInMatrixProject() throws Exception {
        String matlabRoot = System.getenv("MATLAB_ROOT");
        String matlabRoot22b = System.getenv("MATLAB_ROOT_22b");
        Assume.assumeTrue("Not running tests as MATLAB_ROOT_22b environment variable is not defined", matlabRoot22b != null && !matlabRoot22b.isEmpty());

        Utilities.setMatlabInstallation("MATLAB_PATH_1", matlabRoot, jenkins);
        Utilities.setMatlabInstallation("MATLAB_PATH_22b", matlabRoot22b, jenkins);

        MatrixProject project = jenkins.createProject(MatrixProject.class);
        MatlabInstallationAxis MATLABAxis = new MatlabInstallationAxis(Arrays.asList("MATLAB_PATH_1", "MATLAB_PATH_22b"));
        project.setAxes(new AxisList(MATLABAxis));

        //Command Step
        RunMatlabCommandBuilder commandStep =
                new RunMatlabCommandBuilder();
        commandStep.setMatlabCommand("pwd,version");
        StartupOptions startupOptions = new StartupOptions("-logfile outputCommand.log");
        commandStep.setStartupOptions(startupOptions);
        project.getBuildersList().add(commandStep);

        //Run tests step
        project.setScm(new ExtractResourceSCM(Utilities.getRunMATLABTestsData()));
        RunMatlabTestsBuilder runTestsStep = new RunMatlabTestsBuilder();
        //Adding src folder
        List<SourceFolderPaths> list=new ArrayList<SourceFolderPaths>();
        list.add(new SourceFolderPaths("src"));
        runTestsStep.setSourceFolder(new SourceFolder(list));

        // Adding list of test folder
        List<TestFolders> testFolders = new ArrayList<TestFolders>();
        testFolders.add(new TestFolders("test/TestSquare"));
        runTestsStep.setSelectByFolder(new SelectByFolder(testFolders));
        runTestsStep.setStartupOptions(new StartupOptions("-logfile outputTests.log"));
        project.getBuildersList().add(runTestsStep);

        //Rub Build step
        RunMatlabBuildBuilder buildStep = new RunMatlabBuildBuilder();
        buildStep.setTasks("check");
        buildStep.setStartupOptions(new StartupOptions("-logfile outputBuild.log"));
        project.getBuildersList().add(buildStep);

        MatrixBuild build = project.scheduleBuild2(0).get();

        Combination c = new Combination(new AxisList(new MatlabInstallationAxis(Arrays.asList("MATLAB_PATH_1"))), "MATLAB_PATH_1");
        MatrixRun run = build.getRun(c);
        assertTrue(new FilePath(run.getWorkspace(), "outputCommand.log").exists());
        assertTrue(new FilePath(run.getWorkspace(), "outputBuild.log").exists());
        assertTrue(new FilePath(run.getWorkspace(), "outputTests.log").exists());

        c = new Combination(new AxisList(new MatlabInstallationAxis(Arrays.asList("MATLAB_PATH_22b"))), "MATLAB_PATH_22b");
        run = build.getRun(c);
        assertTrue(new FilePath(run.getWorkspace(), "outputCommand.log").exists());
        assertTrue(new FilePath(run.getWorkspace(), "outputBuild.log").exists());
        assertTrue(new FilePath(run.getWorkspace(), "outputTests.log").exists());

        jenkins.assertBuildStatus(Result.SUCCESS, build);
    }
}
