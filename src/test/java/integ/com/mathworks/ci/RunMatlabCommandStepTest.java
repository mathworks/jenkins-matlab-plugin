package com.mathworks.ci.pipeline;

/**
 * Copyright 2020-2024 The MathWorks, Inc.
 */

import java.io.IOException;

import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import hudson.FilePath;
import hudson.slaves.DumbSlave;

import com.mathworks.ci.Message;

public class RunMatlabCommandStepTest {

    private WorkflowJob project;

    @Rule
    public JenkinsRule j = new JenkinsRule();

    @Before
    public void testSetup() throws IOException {
        this.project = j.createProject(WorkflowJob.class);
    }

    /*
     * Verify when MATLAB is not in PATH variable.
     */

    @Test
    public void verifyMATLABPathNotSet() throws Exception {
        project.setDefinition(
                new CpsFlowDefinition("node { runMATLABCommand(command: 'pwd')}", true));
        WorkflowRun build = project.scheduleBuild2(0).get();
        j.assertLogContains("system path", build);
    }

    /*
     * Verify MATLAB gets invoked from workspace.
     */

    @Test
    public void verifyMATLABstartsInWorkspace() throws Exception {
        DumbSlave s = j.createOnlineSlave();
        project.setDefinition(
                new CpsFlowDefinition("node('!built-in') { runMATLABCommand(command: 'pwd')}", true));

        FilePath workspace = s.getWorkspaceFor(project);
        String workspaceName = workspace.getName();
        WorkflowRun build = project.scheduleBuild2(0).get();

        j.assertLogContains(workspaceName, build);
    }

    /*
     * Verify MATLAB is invoked when valid MATLAB is in PATH.
     *
     */

    // @Test
    // public void verifyMATLABPathSet() throws Exception {
    // project.setDefinition(
    // new CpsFlowDefinition("node { runMATLABCommand(command: 'pwd')}", true));
    // WorkflowRun build = project.scheduleBuild2(0).get();
    // j.assertLogContains("tester_started", build);
    // }

    /*
     * Verify Pipeline script runs on Slave with valid MATLAB
     *
     */

    @Test
    public void verifyPipelineOnSlave() throws Exception {
        DumbSlave s = j.createOnlineSlave();
        project.setDefinition(new CpsFlowDefinition(
                "node('!built-in') { runMATLABCommand(command: 'pwd')}", true));

        s.getWorkspaceFor(project);
        WorkflowRun build = project.scheduleBuild2(0).get();

        j.assertLogNotContains("Running on Jenkins", build);
    }

    /*
     * Verify appropriate command is invoked as in pipeline script
     *
     */

    @Test
    public void verifyCommandSameAsScript() throws Exception {
        project.setDefinition(
                new CpsFlowDefinition("node { runMATLABCommand(command: 'pwd')}", true));

        WorkflowRun build = project.scheduleBuild2(0).get();
        j.assertLogContains("pwd", build);
    }

    /*
     * 
     * Verify appropriate startup options are invoked as in pipeline script
     *
     */

    @Test
    public void verifyStartupOptionsSameAsScript() throws Exception {
        project.setDefinition(
                new CpsFlowDefinition(
                        "node { runMATLABCommand(command: 'pwd', startupOptions: '-nojvm -uniqueoption')}", true));

        WorkflowRun build = project.scheduleBuild2(0).get();
        j.assertLogContains("-nojvm -uniqueoption", build);
    }

    /*
     * Verify script can run Matrix build
     *
     */

    @Test
    public void verifyMatrixBuild() throws Exception {
        project.setDefinition(new CpsFlowDefinition(
                "node { matrix {\n" + "agent any\n" + "axes {\n" + "axis {\n" + "name: 'CMD'\n"
                        + "values: 'pwd','ver'\n }}\n" + "runMATLABCommand(command: '${CMD}')}}",
                true));

        WorkflowRun build = project.scheduleBuild2(0).get();
        j.assertLogContains("pwd", build);
        j.assertLogContains("ver", build);
    }

    /*
     * Test for verifying Run Matlab Command raises exception for non-zero exit
     * code.
     */
    @Test
    public void verifyExceptionForNonZeroExitCode() throws Exception {
        // exitMatlab is a mock command for run_matlab_command script to exit with 1.
        project.setDefinition(
                new CpsFlowDefinition(
                        "node { try {runMATLABCommand(command: 'exitMatlab')}catch(exc){echo exc.getMessage()}}",
                        true));

        WorkflowRun build = project.scheduleBuild2(0).get();
        j.assertLogContains(String.format(Message.getValue("matlab.execution.exception.prefix"), 1), build);
        j.assertBuildStatusSuccess(build);
    }
}
