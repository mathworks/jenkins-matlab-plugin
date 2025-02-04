package com.mathworks.ci.systemTests;

import com.mathworks.ci.*;
import com.mathworks.ci.freestyle.RunMatlabCommandBuilder;
import hudson.EnvVars;
import hudson.matrix.AxisList;
import hudson.matrix.Combination;
import hudson.matrix.MatrixProject;
import hudson.matrix.MatrixRun;
import hudson.matrix.MatrixBuild;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import hudson.slaves.DumbSlave;
import hudson.slaves.EnvironmentVariablesNodeProperty;
import hudson.tasks.Builder;
import org.htmlunit.WebAssert;
import org.htmlunit.html.HtmlPage;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.*;
import org.jvnet.hudson.test.JenkinsRule;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import static org.apache.commons.lang3.StringUtils.countMatches;
import static org.junit.Assert.assertEquals;

public class RunMATLABCommandIT {
    @Rule
    public JenkinsRule jenkins = new JenkinsRule();

    @BeforeClass
    public static void checkMatlabRoot() {
        // Check if the MATLAB_ROOT environment variable is defined
        String matlabRoot = System.getenv("MATLAB_ROOT");
        Assume.assumeTrue("Not running tests as MATLAB_ROOT environment variable is not defined", matlabRoot != null && !matlabRoot.isEmpty());
    }

    @Test
    public void verifyBuildStepWithRunMatlab() throws Exception {
        boolean found = false;
        FreeStyleProject project = jenkins.createFreeStyleProject();
        RunMatlabCommandBuilder scriptBuilder = new RunMatlabCommandBuilder();
        scriptBuilder.setMatlabCommand("");
        project.getBuildersList().add(scriptBuilder);
        List<Builder> bl = project.getBuildersList();
        for (Builder b : bl) {
            if (b.getDescriptor().getDisplayName().equalsIgnoreCase(
                    TestMessage.getValue("Builder.matlab.script.builder.display.name"))) {
                found = true;
            }
        }
        Assert.assertTrue("Build step does not contain Run MATLAB Command option", found);
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
        jenkins.assertLogContains(Utilities.getMatlabRoot(), build);
        jenkins.assertLogContains("run-matlab-command", build);
        assertEquals(countMatches(jenkins.getLog(build), "apple"), 2);
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
        assertEquals(countMatches(jenkins.getLog(build), "apple"), 3);
        jenkins.assertBuildStatus(Result.FAILURE, build);
        jenkins.assertLogContains(String.format(Message.getValue("matlab.execution.exception.prefix"), 1), build);
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
        assertEquals(countMatches(jenkins.getLog(run), "apple"), 2);
        jenkins.assertLogContains(matlabRoot, run);
        jenkins.assertLogNotContains(matlabRoot22b, run);

        c = new Combination(new AxisList(new MatlabInstallationAxis(Arrays.asList("MATLAB_PATH_22b"))), "MATLAB_PATH_22b");
        run = build.getRun(c);
        assertEquals(countMatches(jenkins.getLog(run), "apple"), 2);
        jenkins.assertLogContains(matlabRoot22b, run);

        jenkins.assertBuildStatus(Result.SUCCESS, build);
    }

    @Test
    public void verifyBuildPassesWhenMatlabCommandPassesInPipeline() throws Exception {
        String script = "pipeline {\n" +
                "  agent any\n" +
                Utilities.getEnvironmentDSL() + "\n" +
                "    stages{\n" +
                "        stage('Run MATLAB Command') {\n" +
                "            steps\n" +
                "            {\n" +
                "              runMATLABCommand 'matlabroot' \n" +
                "            }\n" +
                "        }\n" +
                "    }\n" +
                "}";

        WorkflowJob project = jenkins.createProject(WorkflowJob.class);
        project.setDefinition(new CpsFlowDefinition(script, true));
        WorkflowRun build = project.scheduleBuild2(0).get();

        jenkins.assertBuildStatus(Result.SUCCESS, build);
        assertEquals(countMatches(jenkins.getLog(build), Utilities.getMatlabRoot()), 1);
    }

    @Test
    public void verifyBuildFailsWhenMatlabCommandFailsInPipeline() throws Exception {
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
        project.setDefinition(new CpsFlowDefinition(script, true));
        WorkflowRun build = project.scheduleBuild2(0).get();

        assertEquals(countMatches(jenkins.getLog(build), "apple"), 3);
        jenkins.assertBuildStatus(Result.FAILURE, build);
    }

    @Test
    public void verifyPipelineOnSlave() throws Exception {
        DumbSlave s = jenkins.createOnlineSlave();
        String script = "node ('!built-in'){\n" +
                Utilities.getEnvironmentScriptedPipeline() + "\n" +
                "        runMATLABCommand 'matlabroot'\n" +
                "}";

        WorkflowJob project = jenkins.createProject(WorkflowJob.class);
        project.setDefinition(new CpsFlowDefinition(script, true));
        WorkflowRun build = project.scheduleBuild2(0).get();

        jenkins.assertLogNotContains("Running on Jenkins", build);
        jenkins.assertBuildStatus(Result.SUCCESS, build);
        assertEquals(countMatches(jenkins.getLog(build), Utilities.getMatlabRoot()), 1);
    }

    @Test
    public void verifyCommandSupportsEnvVar() throws Exception {
        FreeStyleProject project = jenkins.createFreeStyleProject();
        UseMatlabVersionBuildWrapper buildWrapper = new UseMatlabVersionBuildWrapper();
        RunMatlabCommandBuilder scriptBuilder = new RunMatlabCommandBuilder();

        EnvironmentVariablesNodeProperty prop = new EnvironmentVariablesNodeProperty();
        EnvVars var = prop.getEnvVars();
        var.put("PWDCMD", "pwd");
        jenkins.jenkins.getGlobalNodeProperties().add(prop);
        buildWrapper.setMatlabBuildWrapperContent(
                new MatlabBuildWrapperContent(Message.getValue("matlab.custom.location"), Utilities.getMatlabRoot()));
        project.getBuildWrappersList().add(buildWrapper);
        scriptBuilder.setMatlabCommand("$PWDCMD");
        project.getBuildersList().add(scriptBuilder);
        FreeStyleBuild build = project.scheduleBuild2(0).get();
        jenkins.assertLogContains("pwd", build);
    }

    @Test
    public void verifyErrorMessageOnEmptyCommand() throws Exception {
        FreeStyleProject project = jenkins.createFreeStyleProject();
        RunMatlabCommandBuilder scriptBuilder = new RunMatlabCommandBuilder();

        project.getBuildersList().add(scriptBuilder);
        HtmlPage page = jenkins.createWebClient().goTo("job/test0/configure");

        WebAssert.assertTextPresent(page, "Specify at least one script, function, or statement to execute.");
    }
}
