package com.mathworks.ci;
/**
 * Copyright 2022 The MathWorks, Inc.
 * 
 */

import java.io.IOException;

import hudson.model.Result;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import hudson.FilePath;
import hudson.slaves.DumbSlave;

public class RunMatlabBuildStepTest {


    private WorkflowJob project;



    @Rule
    public JenkinsRule j = new JenkinsRule();

    @Before
    public void testSetup() throws IOException {
        this.project = j.createProject(WorkflowJob.class);
    }


    /*
     * Verify when MATLAB is not on system path.
     */
    @Test
    public void verifyMATLABPathNotSet() throws Exception {
        project.setDefinition(
                new CpsFlowDefinition("node { runMATLABBuild() }", true));
        WorkflowRun build = project.scheduleBuild2(0).get();
        j.assertLogContains("MATLAB_ROOT", build);
    }

    /*
     * Verify MATLAB gets invoked from workspace.
     */
    @Test
    public void verifyMATLABstartsInWorkspace() throws Exception {
        DumbSlave s = j.createOnlineSlave();
        project.setDefinition(
                new CpsFlowDefinition("node('!master') { runMATLABBuild() }", true));

        FilePath workspace = s.getWorkspaceFor(project);
        String workspaceName = workspace.getName();
        WorkflowRun build = project.scheduleBuild2(0).get();

        j.assertLogContains(workspaceName, build);
    }

    /*
     * Verify MATLAB is invoked when valid MATLAB is on system path.
     */
    @Test
    public void verifyMATLABPathSet() throws Exception {
        project.setDefinition(
                new CpsFlowDefinition("node { testMATLABBuild() }", true));
        WorkflowRun build = project.scheduleBuild2(0).get();
        j.assertLogContains("tester_started", build);
    }

    /*
     * Verify Pipeline script runs on Jenkins node with valid MATLAB
     */
    @Test
    public void verifyPipelineOnSlave() throws Exception {
        DumbSlave s = j.createOnlineSlave();
        project.setDefinition(new CpsFlowDefinition(
                "node('!master') { testMATLABBuild() }", true));

        s.getWorkspaceFor(project);
        WorkflowRun build = project.scheduleBuild2(0).get();

        j.assertBuildStatusSuccess(build);
    }

    /*
     * Verify appropriate task is invoked as in pipeline script
     */
    @Test
    public void verifyTasksSameAsScript() throws Exception {
        project.setDefinition(
                new CpsFlowDefinition("node { runMATLABBuild(tasks: 'compile') }", true));

        WorkflowRun build = project.scheduleBuild2(0).get();
        j.assertLogContains("compile", build);
    }

    /*
     * Verify script can run Matrix build
     */
    @Test
    public void verifyMatrixBuild() throws Exception {
        project.setDefinition(new CpsFlowDefinition(
                "node { matrix {\n" + "agent any\n" + "axes {\n" + "axis {\n" + "name: 'TASKS'\n"
                        + "values: 'compile','lint'\n } }\n" + "runMATLABBuild(tasks: '${TASKS}')}}",
                true));

        WorkflowRun build = project.scheduleBuild2(0).get();
        j.assertLogContains("compile", build);
        j.assertLogContains("lint", build);
    }

    /*
    * Test for verifying Run Matlab Build raises exception for non-zero exit code.
    * */
    @Test
    public void verifyExceptionForNonZeroExitCode() throws Exception {
        // exitMatlab is a mock build for run_matlab_build script to exit with 1.
        project.setDefinition(
                new CpsFlowDefinition("node { try { runMATLABBuild() } catch(exc) { echo exc.getMessage() } }", true));

        WorkflowRun build = project.scheduleBuild2(0).get();
        j.assertLogContains(String.format(Message.getValue("matlab.execution.exception.prefix"), 1), build);
        j.assertBuildStatusSuccess(build);
    }

    /*
     * Test for verifying Run Matlab Build raises exception for non-zero exit code.
     */
    @Test
    public void verifyExceptionStackTraceForNonZeroExitCode() throws Exception {
        // exitMatlab is a mock build for run_matlab_command script to exit with 1.
        project.setDefinition(
                new CpsFlowDefinition("node { runMATLABBuild() }", true));

        WorkflowRun build = project.scheduleBuild2(0).get();
        j.assertBuildStatus(Result.FAILURE, build);
        j.assertLogContains("com.mathworks.ci.MatlabExecutionException", build);
        j.assertLogContains(String.format(Message.getValue("matlab.execution.exception.prefix"), 1), build);
    }
    
    /*
     * Verify .matlab folder is generated 
     */
    @Test
    public void verifyMATLABtempFolderGenerated() throws Exception {
        project.setDefinition(
                new CpsFlowDefinition("node { testMATLABBuild() }", true));

        WorkflowRun build = project.scheduleBuild2(0).get();
        j.assertLogContains(".matlab", build);
        j.assertBuildStatusSuccess(build);
    }
}
