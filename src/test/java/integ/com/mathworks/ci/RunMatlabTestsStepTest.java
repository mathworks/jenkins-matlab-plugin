package com.mathworks.ci.pipeline;

/**
 * Copyright 2020-2024 The MathWorks, Inc.
 */

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import hudson.model.Result;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import hudson.slaves.DumbSlave;

import com.mathworks.ci.Message;

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
        j.assertLogContains("system path", build);
    }

    /*
     * Verify when MATLAB PATH is set.
     */

    @Test
    public void verifyMATLABPathSet() throws Exception {
        project.setDefinition(new CpsFlowDefinition(
                "node {runMATLABTests(testResultsPDF:'myresult/result.pdf')}", true));
        WorkflowRun build = project.scheduleBuild2(0).get();
        j.assertLogContains("myresult/result.pdf", build);
    }

    /*
     * Verify Pipeline runs on slave node
     */

    @Test
    public void verifyOnslave() throws Exception {
        DumbSlave s = j.createOnlineSlave();
        project.setDefinition(new CpsFlowDefinition(
                "node('!built-in') {runMATLABTests(testResultsPDF:'myresult/result.pdf')}", true));
        s.getWorkspaceFor(project);
        WorkflowRun build = project.scheduleBuild2(0).get();

        j.assertLogNotContains("Running on Jenkins", build);
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
     * Verify appropriate startup options are invoked as in pipeline script
     *
     */

    @Test
    public void verifyStartupOptionsSameAsScript() throws Exception {
        project.setDefinition(
                new CpsFlowDefinition(
                        "node {runMATLABTests(testResultsPDF:'myresult/result.pdf', startupOptions: '-nojvm -uniqueoption')}",
                        true));

        WorkflowRun build = project.scheduleBuild2(0).get();
        j.assertLogContains("-nojvm -uniqueoption", build);
    }

    /*
     * Verify default command options for test run.
     */

    @Test
    public void verifyCmdOptions() throws Exception {
        project.setDefinition(new CpsFlowDefinition(
                "node {runMATLABTests(testResultsPDF:'myresult/result.pdf')}", true));
        WorkflowRun build = project.scheduleBuild2(0).get();
        j.assertLogContains("setenv('MW_ORIG_WORKING_FOLDER',", build);
        j.assertLogContains("run-matlab-command", build);
    }

    /*
     * Verify Artifact is not sent as parameter.
     */

    @Test
    public void verifyArtifactParameters() throws Exception {
        project.setDefinition(new CpsFlowDefinition(
                "node {runMATLABTests(testResultsPDF:'myresult/result.pdf')}", true));
        WorkflowRun build = project.scheduleBuild2(0).get();
        j.assertLogContains("'PDFTestReport','myresult/result.pdf'", build);
        j.assertLogNotContains("TAPTestResults", build);
        j.assertLogNotContains("JUnitTestResults", build);
        j.assertLogNotContains("CoberturaCodeCoverage", build);
        j.assertLogNotContains("SimulinkTestResults", build);
        j.assertLogNotContains("CoberturaModelCoverage", build);
    }

    /*
     * Verify runMatlabTests runs with empty parameters when nothing no artifact
     * selected
     */

    @Test
    public void verifyEmptyParameter() throws Exception {
        project.setDefinition(new CpsFlowDefinition(
                "node {runMATLABTests()}", true));
        WorkflowRun build = project.scheduleBuild2(0).get();
        j.assertLogContains("run-matlab-command", build);
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

    /*
     * @Integ Test
     * Verify default command options for test Filter using selectByFolder option
     */

    public void verifyTestSelectByFolder() throws Exception {
        project.setDefinition(new CpsFlowDefinition(
                "node {runMATLABTests(selectByFolder:['mytest1','mytest2'])}", true));
        WorkflowRun build = project.scheduleBuild2(0).get();
        j.assertLogContains("mytest1", build);
        j.assertLogContains("mytest2", build);
    }

    /*
     * @Integ Test
     * Verify default command options for test Filter using selectByTag option
     */

    public void verifyTestSelectByTag() throws Exception {
        project.setDefinition(new CpsFlowDefinition(
                "node {runMATLABTests(selectByTag: 'myTestTag')}", true));
        WorkflowRun build = project.scheduleBuild2(0).get();
        j.assertLogContains("myTestTag", build);
    }

    /*
     * @Integ
     * Verify outputDetail set
     */

    public void verifyOutputDetailSet() {
        Map<String, String> outputDetail = new HashMap<String, String>();
        outputDetail.put("none", "'OutputDetail', 0");
        outputDetail.put("terse", "'OutputDetail', 1");
        outputDetail.put("concise", "'OutputDetail', 2");
        outputDetail.put("detailed", "'OutputDetail', 3");
        outputDetail.put("verbose", "'OutputDetail', 4");
        outputDetail.forEach((key, val) -> {
            project.setDefinition(new CpsFlowDefinition(
                    "node {runMATLABTests(outputDetail: '" + key + "')}", true));
            WorkflowRun build;

            try {
                build = project.scheduleBuild2(0).get();
                j.assertLogContains(val, build);
            } catch (InterruptedException | ExecutionException | IOException e) {
                System.out.println("Build Failed, refer logs for details");
                e.printStackTrace();
            }
        });
    }

    /*
     * @Integ
     * Verify loggingLevel set
     */

    public void verifyLoggingLevelSet() {
        Map<String, String> outputDetail = new HashMap<String, String>();
        outputDetail.put("none", "'LoggingLevel', 0");
        outputDetail.put("terse", "'LoggingLevel', 1");
        outputDetail.put("concise", "'LoggingLevel', 2");
        outputDetail.put("detailed", "'LoggingLevel', 3");
        outputDetail.put("verbose", "'LoggingLevel', 4");
        outputDetail.forEach((key, val) -> {
            project.setDefinition(new CpsFlowDefinition(
                    "node {runMATLABTests(loggingLevel: '" + key + "', outputDetail: 'None')}",
                    true));
            WorkflowRun build;

            try {
                build = project.scheduleBuild2(0).get();
                j.assertLogContains(val, build);
            } catch (InterruptedException | ExecutionException | IOException e) {
                System.out.println("Build Failed, refer logs for details");
                e.printStackTrace();
            }
        });
    }

    /*
     * @Integ
     * Verify when useParallel Set
     */

    public void verifyUseParallelSet() throws Exception {
        project.setDefinition(new CpsFlowDefinition(
                "node {runMATLABTests(useParallel: true)}", true));
        WorkflowRun build = project.scheduleBuild2(0).get();
        j.assertLogContains("runInParallel", build);
    }

    /*
     * @Integ
     * Verify when useParallel Not Set
     */

    public void verifyUseParallelNotSet() throws Exception {
        project.setDefinition(new CpsFlowDefinition(
                "node {runMATLABTests(useParallel: false)}", true));
        WorkflowRun build = project.scheduleBuild2(0).get();
        j.assertLogNotContains("runInParallel", build);
    }

    /*
     * @Integ
     * Verify when strict Set
     */

    public void verifyStrictSet() throws Exception {
        project.setDefinition(new CpsFlowDefinition(
                "node {runMATLABTests(strict: true)}", true));
        WorkflowRun build = project.scheduleBuild2(0).get();
        j.assertLogContains("FailOnWarningsPlugin", build);
    }

    /*
     * @Integ
     * Verify when strict is not Set
     */

    public void verifyStrictNotSet() throws Exception {
        project.setDefinition(new CpsFlowDefinition(
                "node {runMATLABTests(strict: false)}", true));
        WorkflowRun build = project.scheduleBuild2(0).get();
        j.assertLogNotContains("FailOnWarningsPlugin", build);
    }
}
