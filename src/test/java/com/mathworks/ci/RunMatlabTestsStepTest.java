package com.mathworks.ci;

/**
 * Copyright 2020 The MathWorks, Inc.
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
import hudson.slaves.DumbSlave;

public class RunMatlabTestsStepTest {

    private WorkflowJob project;

    @Rule
    public JenkinsRule j = new JenkinsRule();

    @Before
    public void testSetup() throws IOException {
        this.project = j.createProject(WorkflowJob.class);
    }


    /*
     * Verify when MATLAB Path is not set
     */
    @Test
    public void verifyMATLABPathNotSet() throws Exception {
        project.setDefinition(new CpsFlowDefinition(
                "node {runMATLABCommand \"version\"}", true));
        WorkflowRun build = project.scheduleBuild2(0).get();
        String build_log = j.getLog(build);
        j.assertLogContains("MATLAB_ROOT", build);
    }


    /*
     * VErify when MATLAB PATH is set.
     */

    @Test
    public void verifyMATLABPathSet() throws Exception {
        project.setDefinition(new CpsFlowDefinition(
                "node {testMATLABTests(testResultsPDF:'myresult/result.pdf')}", true));
        WorkflowRun build = project.scheduleBuild2(0).get();
        j.assertLogContains("tester_started", build);
    }

    /*
     * Verify Pipeline runs on slave node
     */

    @Test
    public void verifyOnslave() throws Exception {
        DumbSlave s = j.createOnlineSlave();
        project.setDefinition(new CpsFlowDefinition(
                "node('!master') {testMATLABTests(testResultsPDF:'myresult/result.pdf')}", true));
        s.getWorkspaceFor(project);
        WorkflowRun build = project.scheduleBuild2(0).get();
        j.assertBuildStatusSuccess(build);
    }

    /*
     * Verify artifact path is correct. Need to move this to integration test.
     */

    
    public void verifyArtifactPath() throws Exception {
        project.setDefinition(new CpsFlowDefinition(
                "node {runMATLABTests(testResultsPDF:'myresult/result.pdf')}", true));
        WorkflowRun build = project.scheduleBuild2(0).get();
        j.assertLogContains("producingPDF('myresult/result.pdf')", build);
    }
    
    /*
    * Verify default command options for test run.
    */

   @Test
   public void verifyCmdOptions() throws Exception {
       project.setDefinition(new CpsFlowDefinition(
               "node {runMATLABTests(testResultsPDF:'myresult/result.pdf')}", true));
       WorkflowRun build = project.scheduleBuild2(0).get();
       j.assertLogContains("addpath(", build);
       j.assertLogContains("test_runner", build);
   }

    /*
     * Verify Artifact is not sent as parameter.
     */

    @Test
    public void verifyArtifactParameters() throws Exception {
        project.setDefinition(new CpsFlowDefinition(
                "node {runMATLABTests(testResultsPDF:'myresult/result.pdf')}", true));
        WorkflowRun build = project.scheduleBuild2(0).get();
        j.assertLogNotContains("'PDFTestReport','myresult/result.pdf'", build);
        j.assertLogNotContains("TAPTestResults", build);
        j.assertLogNotContains("JUnitTestResults", build);
        j.assertLogNotContains("CoberturaCodeCoverage", build);
        j.assertLogNotContains("SimulinkTestResults", build);
        j.assertLogNotContains("CoberturaModelCoverage", build);
    }
    
    /*
     * Verify runMatlabTests runs with empty parameters when nothing no artifact selected 
     */

    @Test
    public void verifyEmptyParameter() throws Exception {
        project.setDefinition(new CpsFlowDefinition(
                "node {runMATLABTests()}", true));
        WorkflowRun build = project.scheduleBuild2(0).get();
        j.assertLogContains("test_runner", build);
        j.assertLogNotContains("PDFReportPath", build);
        j.assertLogNotContains("TAPResultsPath", build);
        j.assertLogNotContains("JUnitResultsPath", build);
        j.assertLogNotContains("CoberturaCodeCoveragePath", build);
        j.assertLogNotContains("SimulinkTestResultsPath", build);
        j.assertLogNotContains("CoberturaModelCoveragePath", build);
    }

    @Test
    public void verifyExceptionForNonZeroExitCode() throws Exception {
        project.setDefinition(new CpsFlowDefinition(
                "node {runMATLABTests()}", true));
        WorkflowRun build = project.scheduleBuild2(0).get();
        j.assertBuildStatus(Result.FAILURE, build);
        j.assertLogContains(String.format(Message.getValue("matlab.execution.exception.prefix"), 1), build);
    }
    
    /*@Integ Test
     * Verify default command options for test Filter using selectByFolder option 
     */

    public void verifyTestSelectByFolder () throws Exception {
        project.setDefinition(new CpsFlowDefinition(
                "node {runMATLABTests(selectByFolder:['mytest1','mytest2'])}", true));
        WorkflowRun build = project.scheduleBuild2(0).get();
        j.assertLogContains("mytest1", build);
        j.assertLogContains("mytest2", build);
        j.assertBuildStatusSuccess(build);
    }
    
    /*@Integ Test
     * Verify default command options for test Filter using selectByTag option 
     */

    public void verifyTestSelectByTag () throws Exception {
        project.setDefinition(new CpsFlowDefinition(
                "node {runMATLABTests(selectByTag: 'myTestTag')}", true));
        WorkflowRun build = project.scheduleBuild2(0).get();
        j.assertLogContains("myTestTag", build);
        j.assertBuildStatusSuccess(build);
    }
}
