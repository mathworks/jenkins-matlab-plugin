package com.mathworks.ci.systemTests;

import com.mathworks.ci.MatlabBuildWrapperContent;
import com.mathworks.ci.Message;
import com.mathworks.ci.UseMatlabVersionBuildWrapper;
import com.mathworks.ci.freestyle.RunMatlabTestsBuilder;
import com.mathworks.ci.freestyle.options.SourceFolder;
import com.mathworks.ci.freestyle.options.SourceFolderPaths;
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
import java.util.List;

public class RunMATLABTestsCustomizeRunIT {
    private FreeStyleProject project;
    private UseMatlabVersionBuildWrapper buildWrapper;
    private RunMatlabTestsBuilder testBuilder;

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
        this.project = jenkins.createFreeStyleProject();
        this.testBuilder = new RunMatlabTestsBuilder();
        this.buildWrapper = new UseMatlabVersionBuildWrapper();
        testBuilder.setLoggingLevel("default");
        testBuilder.setOutputDetail("default");
    }

    @After
    public void testTearDown() {
        this.project = null;
        this.testBuilder = null;
    }

    @Test
    public void verifyLoggingLevelSetToNone() throws Exception {

        this.buildWrapper.setMatlabBuildWrapperContent(new MatlabBuildWrapperContent(
                Message.getValue("matlab.custom.location"), MatlabRootSetup.getMatlabRoot()));
        project.getBuildWrappersList().add(this.buildWrapper);
        testBuilder.setLoggingLevel("None");
        project.getBuildersList().add(this.testBuilder);
        FreeStyleBuild build;
        build = project.scheduleBuild2(0).get();
        jenkins.assertLogContains("LoggingLevel', 0", build);

    }

    @Test
    public void verifyLoggingLevelSetToTerse() throws Exception {

        this.buildWrapper.setMatlabBuildWrapperContent(new MatlabBuildWrapperContent(
                Message.getValue("matlab.custom.location"), MatlabRootSetup.getMatlabRoot()));
        project.getBuildWrappersList().add(this.buildWrapper);
        testBuilder.setLoggingLevel("Terse");
        project.getBuildersList().add(this.testBuilder);
        FreeStyleBuild build;
        build = project.scheduleBuild2(0).get();
        jenkins.assertLogContains("LoggingLevel', 1", build);

    }

    @Test
    public void verifyLoggingLevelSetToConcise() throws Exception {

        this.buildWrapper.setMatlabBuildWrapperContent(new MatlabBuildWrapperContent(
                Message.getValue("matlab.custom.location"), MatlabRootSetup.getMatlabRoot()));
        project.getBuildWrappersList().add(this.buildWrapper);
        testBuilder.setLoggingLevel("Concise");
        project.getBuildersList().add(this.testBuilder);
        FreeStyleBuild build;
        build = project.scheduleBuild2(0).get();
        jenkins.assertLogContains("LoggingLevel', 2", build);

    }

    @Test
    public void verifyLoggingLevelSetToDetailed() throws Exception {

        this.buildWrapper.setMatlabBuildWrapperContent(new MatlabBuildWrapperContent(
                Message.getValue("matlab.custom.location"), MatlabRootSetup.getMatlabRoot()));
        project.getBuildWrappersList().add(this.buildWrapper);
        testBuilder.setLoggingLevel("Detailed");
        project.getBuildersList().add(this.testBuilder);
        FreeStyleBuild build;
        build = project.scheduleBuild2(0).get();
        jenkins.assertLogContains("LoggingLevel', 3", build);

    }

    /*@Integ
     * Test To verify if Output Detail  is set correctly
     *
     */

    @Test
    public void verifyOutputDetailSetToNone() throws Exception {
        this.buildWrapper.setMatlabBuildWrapperContent(new MatlabBuildWrapperContent(
                Message.getValue("matlab.custom.location"), MatlabRootSetup.getMatlabRoot()));
        project.getBuildWrappersList().add(this.buildWrapper);
        testBuilder.setLoggingLevel("None");

        testBuilder.setOutputDetail("None");
        project.getBuildersList().add(this.testBuilder);
        FreeStyleBuild build;
        build = project.scheduleBuild2(0).get();
        jenkins.assertLogContains("'OutputDetail', 0", build);
    }

    @Test
    public void verifyOutputDetailSetToTerse() throws Exception {
        this.buildWrapper.setMatlabBuildWrapperContent(new MatlabBuildWrapperContent(
                Message.getValue("matlab.custom.location"), MatlabRootSetup.getMatlabRoot()));
        project.getBuildWrappersList().add(this.buildWrapper);
        testBuilder.setLoggingLevel("None");

        testBuilder.setOutputDetail("Terse");
        project.getBuildersList().add(this.testBuilder);
        FreeStyleBuild build;
        build = project.scheduleBuild2(0).get();
        jenkins.assertLogContains("'OutputDetail', 1", build);
    }

    @Test
    public void verifyOutputDetailSetToConcise() throws Exception {
        this.buildWrapper.setMatlabBuildWrapperContent(new MatlabBuildWrapperContent(
                Message.getValue("matlab.custom.location"), MatlabRootSetup.getMatlabRoot()));
        project.getBuildWrappersList().add(this.buildWrapper);
        testBuilder.setLoggingLevel("None");

        testBuilder.setOutputDetail("Concise");
        project.getBuildersList().add(this.testBuilder);
        FreeStyleBuild build;
        build = project.scheduleBuild2(0).get();
        jenkins.assertLogContains("'OutputDetail', 2", build);
    }

    @Test
    public void verifyOutputDetailSetToDetailed() throws Exception {
        this.buildWrapper.setMatlabBuildWrapperContent(new MatlabBuildWrapperContent(
                Message.getValue("matlab.custom.location"), MatlabRootSetup.getMatlabRoot()));
        project.getBuildWrappersList().add(this.buildWrapper);
        testBuilder.setLoggingLevel("None");

        testBuilder.setOutputDetail("Detailed");
        project.getBuildersList().add(this.testBuilder);
        FreeStyleBuild build;
        build = project.scheduleBuild2(0).get();
        jenkins.assertLogContains("'OutputDetail', 3", build);
    }

    @Test
    public void verifyStrictSet() throws Exception {
        this.buildWrapper.setMatlabBuildWrapperContent(new MatlabBuildWrapperContent(
                Message.getValue("matlab.custom.location"), MatlabRootSetup.getMatlabRoot()));
        project.getBuildWrappersList().add(this.buildWrapper);
        testBuilder.setLoggingLevel("None");
        testBuilder.setStrict(true);
        project.getBuildersList().add(this.testBuilder);
        FreeStyleBuild build = project.scheduleBuild2(0).get();
        jenkins.assertLogContains("FailOnWarningsPlugin", build);

    }

    @Test
    public void verifyStrictNotSet() throws Exception {
        this.buildWrapper.setMatlabBuildWrapperContent(new MatlabBuildWrapperContent(
                Message.getValue("matlab.custom.location"), MatlabRootSetup.getMatlabRoot()));
        project.getBuildWrappersList().add(this.buildWrapper);
        testBuilder.setLoggingLevel("None");
        testBuilder.setStrict(false);
        project.getBuildersList().add(this.testBuilder);
        FreeStyleBuild build = project.scheduleBuild2(0).get();
        jenkins.assertLogNotContains("FailOnWarningsPlugin", build);

    }

    @Test
    public void verifyRunParallelSet() throws Exception {
        this.buildWrapper.setMatlabBuildWrapperContent(new MatlabBuildWrapperContent(
                Message.getValue("matlab.custom.location"), MatlabRootSetup.getMatlabRoot()));
        project.getBuildWrappersList().add(this.buildWrapper);
        testBuilder.setLoggingLevel("None");
        testBuilder.setUseParallel(true);
        project.getBuildersList().add(this.testBuilder);
        FreeStyleBuild build = project.scheduleBuild2(0).get();
        jenkins.assertLogContains("runInParallel", build);
    }

    @Test
    public void verifyRunParallelNotSet() throws Exception {
        this.buildWrapper.setMatlabBuildWrapperContent(new MatlabBuildWrapperContent(
                Message.getValue("matlab.custom.location"), MatlabRootSetup.getMatlabRoot()));
        project.getBuildWrappersList().add(this.buildWrapper);
        testBuilder.setLoggingLevel("None");
        testBuilder.setUseParallel(false);
        project.getBuildersList().add(this.testBuilder);
        FreeStyleBuild build = project.scheduleBuild2(0).get();
        jenkins.assertLogNotContains("runInParallel", build);
    }

    @Test
    public void verifyRunInParallel() throws Exception {
        String script = "pipeline {\n" +
                "  agent any\n" +
                MatlabRootSetup.getEnvironmentDSL()  + "\n" +
                "    stages{\n" +
                "        stage('Run MATLAB Command') {\n" +
                "            steps\n" +
                "            {\n" +
                "              runMATLABTests(useParallel:true)\n" +
                "            }\n" +
                "        }\n" +
                "    }\n" +
                "}";
        WorkflowRun build = getPipelineBuild(script);
        jenkins.assertLogContains("runInParallel", build);
        jenkins.assertBuildStatus(Result.SUCCESS,build);
    }

    @Test
    public void verifyStrictSetPipeline() throws Exception {
        String script = "pipeline {\n" +
                "  agent any\n" +
                MatlabRootSetup.getEnvironmentDSL()  + "\n" +
                "    stages{\n" +
                "        stage('Run MATLAB Command') {\n" +
                "            steps\n" +
                "            {\n" +
                "              runMATLABTests(strict:true)\n" +
                "            }\n" +
                "        }\n" +
                "    }\n" +
                "}";
        WorkflowRun build = getPipelineBuild(script);
        jenkins.assertLogContains("FailOnWarningsPlugin", build);
        jenkins.assertBuildStatus(Result.SUCCESS,build);
    }

    @Test
    public void verifyLoggingLevelSet() throws Exception {
        String script = "pipeline {\n" +
                "  agent any\n" +
                MatlabRootSetup.getEnvironmentDSL()  + "\n" +
                "    stages{\n" +
                "        stage('Run MATLAB Command') {\n" +
                "            steps\n" +
                "            {\n" +
                "              runMATLABTests(sourceFolder:['src'], loggingLevel:'None')\n" +
                "            }\n" +
                "        }\n" +
                "    }\n" +
                "}";
        WorkflowRun build = getPipelineBuild(script);
        jenkins.assertLogContains("'LoggingLevel', 0", build);
        jenkins.assertBuildStatus(Result.SUCCESS,build);
    }

    @Test
    public void verifyOutoutDetailSet() throws Exception {
        String script = "pipeline {\n" +
                "  agent any\n" +
                MatlabRootSetup.getEnvironmentDSL()  + "\n" +
                "    stages{\n" +
                "        stage('Run MATLAB Command') {\n" +
                "            steps\n" +
                "            {\n" +
                "              runMATLABTests(outputDetail:'None')\n" +
                "            }\n" +
                "        }\n" +
                "    }\n" +
                "}";
        WorkflowRun build = getPipelineBuild(script);
        jenkins.assertLogContains("'OutputDetail', 0", build);
        jenkins.assertBuildStatus(Result.SUCCESS,build);
    }

    /*
     * Test to verify if Matrix build passes (mock MATLAB).
     */
    @Test
    public void verifyMatrixBuildPasses() throws Exception {
        MatrixProject matrixProject = jenkins.createProject(MatrixProject.class);
        Axis axes = new Axis("VERSION", TestData.getPropValues("matlab.version"), TestData.getPropValues("matlab.matrix.version"));
        matrixProject.setAxes(new AxisList(axes));
        String matlabRoot = MatlabRootSetup.getMatlabRoot();
        this.buildWrapper.setMatlabBuildWrapperContent(new MatlabBuildWrapperContent(TestData.getPropValues("matlab.custom.location"), matlabRoot.replace(TestData.getPropValues("matlab.version"), "$VERSION")));
        matrixProject.getBuildWrappersList().add(this.buildWrapper);

        testBuilder.setOutputDetail("None");
        testBuilder.setLoggingLevel("None");
        testBuilder.setStrict(true);
        testBuilder.setUseParallel(true);

        matrixProject.getBuildersList().add(testBuilder);
        MatrixBuild build = matrixProject.scheduleBuild2(0).get();
        List<MatrixRun> runs = build.getRuns();

        for (MatrixRun run : runs) {
            jenkins.assertLogContains("LoggingLevel', 0", run);
            jenkins.assertLogContains("OutputDetail', 0", run);
            jenkins.assertLogContains("FailOnWarningsPlugin", run);
            jenkins.assertLogContains("runInParallel", run);
        }


        jenkins.assertLogContains(TestData.getPropValues("matlab.version")+" completed", build);
        jenkins.assertLogContains(TestData.getPropValues("matlab.matrix.version")+" completed", build);
    }


    private WorkflowRun getPipelineBuild(String script) throws Exception{
        WorkflowJob project = jenkins.createProject(WorkflowJob.class);
        project.setDefinition(new CpsFlowDefinition(script,true));
        return project.scheduleBuild2(0).get();
    }
}
