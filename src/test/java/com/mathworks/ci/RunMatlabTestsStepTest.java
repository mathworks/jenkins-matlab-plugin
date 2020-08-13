package com.mathworks.ci;

/**
 * Copyright 2020 The MathWorks, Inc.
 *  
 */

import java.io.IOException;
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
                "node {runMATLABTests(testResultsPDF:'myresult/result.pdf')}", true));
        WorkflowRun build = project.scheduleBuild2(0).get();
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
     * Verify artifact path is correct.
     */

    @Test
    public void verifyArtifactPath() throws Exception {
        project.setDefinition(new CpsFlowDefinition(
                "node {runMATLABTests(testResultsPDF:'myresult/result.pdf')}", true));
        WorkflowRun build = project.scheduleBuild2(0).get();
        j.assertLogContains("'PDFReportPath','myresult/result.pdf'", build);
    }
    
    /*
    * Verify default command options for test run.
    */

   @Test
   public void verifyCmdOptions() throws Exception {
       project.setDefinition(new CpsFlowDefinition(
               "node {runMATLABTests(testResultsPDF:'myresult/result.pdf')}", true));
       WorkflowRun build = project.scheduleBuild2(0).get();
       j.assertLogContains("cd(", build);
       j.assertLogContains("test_runner", build);
   }

    /*
     * Verify Artifact is not sent as parameter if not selected in script.
     */

    @Test
    public void verifyArtifactParameters() throws Exception {
        project.setDefinition(new CpsFlowDefinition(
                "node {runMATLABTests(testResultsPDF:'myresult/result.pdf')}", true));
        WorkflowRun build = project.scheduleBuild2(0).get();
        j.assertLogContains("'PDFReportPath','myresult/result.pdf'", build);
        j.assertLogNotContains("TAPResultsPath", build);
        j.assertLogNotContains("JUnitResultsPath", build);
        j.assertLogNotContains("CoberturaCodeCoveragePath", build);
        j.assertLogNotContains("SimulinkTestResultsPath", build);
        j.assertLogNotContains("CoberturaModelCoveragePath", build);
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
}
