package com.mathworks.ci.systemTests;

import com.mathworks.ci.*;
import com.mathworks.ci.freestyle.RunMatlabCommandBuilder;
import hudson.matrix.AxisList;
import hudson.matrix.Combination;
import hudson.matrix.MatrixProject;
import hudson.matrix.MatrixRun;
import hudson.matrix.MatrixBuild;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.*;
import org.jvnet.hudson.test.JenkinsRule;
import java.util.Arrays;

public class RunMATLABCommandIT {
    @Rule
    public JenkinsRule jenkins = new JenkinsRule();

    @BeforeClass
    public static void checkMatlabRoot() {
        // Check if the MATLAB_ROOT environment variable is defined
        String matlabRoot = System.getenv("MATLAB_ROOT");
        Assume.assumeTrue("Not running tests as MATLAB_ROOT environment variable is not defined", matlabRoot != null && !matlabRoot.isEmpty());
    }

    /*
     *Test To Verify if Build passes when matlab command passes
     */
    @Test
    public void verifyBuildPassesWhenMatlabCommandPasses() throws Exception {
        FreeStyleProject project = jenkins.createFreeStyleProject();
        UseMatlabVersionBuildWrapper buildWrapper = new UseMatlabVersionBuildWrapper();
        RunMatlabCommandBuilder scriptBuilder = new RunMatlabCommandBuilder();

        buildWrapper.setMatlabBuildWrapperContent(new MatlabBuildWrapperContent(Message.getValue("matlab.custom.location"), Utilities.getMatlabRoot()));
        project.getBuildWrappersList().add(buildWrapper);

        scriptBuilder.setMatlabCommand("disp 'apple'");
        project.getBuildersList().add(scriptBuilder);

        FreeStyleBuild build = project.scheduleBuild2(0).get();
        jenkins.assertBuildStatus(Result.SUCCESS, build);
        jenkins.assertLogContains("apple", build);
        jenkins.assertLogContains(Utilities.getMatlabRoot(), build);
    }

    /*
     * Test to verify if Build FAILS when matlab command fails
     */

    @Test
    public void verifyBuildFailureWhenMatlabCommandFails() throws Exception {
        FreeStyleProject project = jenkins.createFreeStyleProject();
        UseMatlabVersionBuildWrapper buildWrapper = new UseMatlabVersionBuildWrapper();
        RunMatlabCommandBuilder scriptBuilder = new RunMatlabCommandBuilder();

        buildWrapper.setMatlabBuildWrapperContent(new MatlabBuildWrapperContent(Message.getValue("matlab.custom.location"), Utilities.getMatlabRoot()));
        project.getBuildWrappersList().add(buildWrapper);

        scriptBuilder.setMatlabCommand("apple");
        project.getBuildersList().add(scriptBuilder);
        FreeStyleBuild build = project.scheduleBuild2(0).get();
        jenkins.assertLogContains("apple", build);
        jenkins.assertBuildStatus(Result.FAILURE, build);
    }

    /*
     * Test to verify if Matrix build passes .
     */

    @Test
    public void verifyMatrixBuildPassesWithMATLABCommand() throws Exception {
        String matlabRoot = System.getenv("MATLAB_ROOT");
        String matlabRoot22b = System.getenv("MATLAB_ROOT_22b");
        Assume.assumeTrue("Not running tests as MATLAB_ROOT_22b environment variable is not defined", matlabRoot22b != null && !matlabRoot22b.isEmpty());

        Utilities.setMatlabInstallation("MATLAB_PATH_1", matlabRoot, jenkins);
        Utilities.setMatlabInstallation("MATLAB_PATH_22b", matlabRoot22b, jenkins);

        MatrixProject matrixProject = jenkins.createProject(MatrixProject.class);
        MatlabInstallationAxis MATLABAxis = new MatlabInstallationAxis(Arrays.asList("MATLAB_PATH_1", "MATLAB_PATH_22b"));
        matrixProject.setAxes(new AxisList(MATLABAxis));

        RunMatlabCommandBuilder scriptBuilder = new RunMatlabCommandBuilder();
        scriptBuilder.setMatlabCommand("disp('apple')");
        matrixProject.getBuildersList().add(scriptBuilder);

        MatrixBuild build = matrixProject.scheduleBuild2(0).get();

        Combination c = new Combination(new AxisList(new MatlabInstallationAxis(Arrays.asList("MATLAB_PATH_1"))), "MATLAB_PATH_1");
        MatrixRun run = build.getRun(c);
        jenkins.assertLogContains("disp('apple')", run);
        jenkins.assertLogContains(matlabRoot, run);
        jenkins.assertLogNotContains(matlabRoot22b, run);

        c = new Combination(new AxisList(new MatlabInstallationAxis(Arrays.asList("MATLAB_PATH_22b"))), "MATLAB_PATH_22b");
        run = build.getRun(c);
        jenkins.assertLogContains("disp('apple')", run);
        jenkins.assertLogContains(matlabRoot22b,run);

        jenkins.assertBuildStatus(Result.SUCCESS, build);
    }

    @Test
    public void verifyBuildPassesWhenMatlabCommandPassesPipeline() throws Exception {
        String script = "pipeline {\n" +
                "  agent any\n" +
                Utilities.getEnvironmentDSL() + "\n" +
                "    stages{\n" +
                "        stage('Run MATLAB Command') {\n" +
                "            steps\n" +
                "            {\n" +
                "              runMATLABCommand 'version' \n" +
                "            }\n" +
                "        }\n" +
                "    }\n" +
                "}";

        WorkflowJob project = jenkins.createProject(WorkflowJob.class);
        project.setDefinition(new CpsFlowDefinition(script,true));
        WorkflowRun build = project.scheduleBuild2(0).get();

        jenkins.assertBuildStatus(Result.SUCCESS, build);
    }

    @Test
    public void verifyBuildFailsWhenMatlabCommandFails() throws Exception {
        String script = "pipeline {\n" +
                "  agent any\n" +
                Utilities.getEnvironmentDSL() + "\n" +
                "    stages{\n" +
                "        stage('Run MATLAB Command') {\n" +
                "            steps\n" +
                "            {\n" +
                "              runMATLABCommand 'apple' \n" +
                "            }\n" +
                "        }\n" +
                "    }\n" +
                "}";

        WorkflowJob project = jenkins.createProject(WorkflowJob.class);
        project.setDefinition(new CpsFlowDefinition(script,true));
        WorkflowRun build = project.scheduleBuild2(0).get();

        jenkins.assertLogContains("apple",build);
        jenkins.assertBuildStatus(Result.FAILURE, build);
    }
}
