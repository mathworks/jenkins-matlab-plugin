package com.mathworks.ci;

import com.google.common.io.Resources;
import com.google.common.base.Charsets;
import hudson.model.Result;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.IOUtils;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import java.io.IOException;
import java.net.URL;

public class PipelineInteg {

    private WorkflowJob project;

    @Rule
    public JenkinsRule jenkins = new JenkinsRule();

    @Before
    public void testSetup() throws IOException {
        this.project = jenkins.createProject(WorkflowJob.class);
    }

    @Test
    public void verifyEmptyRootError() throws Exception {
        project.setDefinition(new CpsFlowDefinition(
                "node {runMATLABCommand \"version\"}",true));
        WorkflowRun build = project.scheduleBuild2(0).get();
        String build_log = jenkins.getLog(build);
        jenkins.assertLogContains("MATLAB_ROOT",build);
        jenkins.assertBuildStatus(Result.FAILURE,build);
    }

//    @Test
//    public void verifyWrongMatlabVersion() throws Exception {
//        project.setDefinition(new CpsFlowDefinition(
//                "node {env.PATH = \"C:\\\\Program Files\\\\MATLAB\\\\R2009a\\\\bin;${env.PATH}\" \n" +
//                        "runMATLABCommand \"version\"}",true));
//        WorkflowRun build = project.scheduleBuild2(0).get();
//        String build_log = jenkins.getLog(build);
//        jenkins.assertLogContains("No such DSL method",build);
//        jenkins.assertBuildStatus(Result.FAILURE,build);
//    }

    @Test
    public void verifyBuildPassesWhenMatlabCommandPasses() throws Exception {
        project.setDefinition(new CpsFlowDefinition(
                "node {env.PATH = \"C:\\\\Program Files\\\\MATLAB\\\\R2020a\\\\bin;${env.PATH}\" \n" +
                        "runMATLABCommand \"version\"}",true));
        WorkflowRun build = project.scheduleBuild2(0).get();
        jenkins.assertBuildStatus(Result.SUCCESS, build);
    }

    @Test
    public void verifyBuildFailsWhenMatlabCommandFails() throws Exception {
        project.setDefinition(new CpsFlowDefinition(
                "node {env.PATH = \"C:\\\\Program Files\\\\MATLAB\\\\R2020a\\\\bin;${env.PATH}\" \n" +
                "runMATLABCommand \"apple\"}", true));
        WorkflowRun build = project.scheduleBuild2(0).get();
        String build_log = jenkins.getLog(build);
        jenkins.assertLogContains("Unrecognized function or variable",build);
        jenkins.assertBuildStatus(Result.FAILURE, build);
    }

    @Test
    public void verifyBuildFailsWhenDSLNameFails() throws Exception {
        project.setDefinition(new CpsFlowDefinition(
                                "node {env.PATH = \"C:\\\\Program Files\\\\MATLAB\\\\R2020a\\\\bin;${env.PATH}\" \n" +
                                        "runMatlabCommand \"version\"}",true));
        WorkflowRun build = project.scheduleBuild2(0).get();
        String build_log = jenkins.getLog(build);
        jenkins.assertLogContains("No such DSL method",build);
        jenkins.assertBuildStatus(Result.FAILURE,build);
    }

    @Test
    public void verifyCustomeFilenamesForArtifacts() throws Exception
    {
        project.setDefinition(new CpsFlowDefinition(
                "pipeline {\n" +
                        "  agent any\n" +
                        "  environment {\n" +
                        "      PATH = \"C:\\\\Program Files\\\\MATLAB\\\\R2020a\\\\bin;${PATH}\"   \n" +
                        "  }\n" +
                        "    stages{\n" +
                        "        stage('Run MATLAB Command') {\n" +
                        "            steps\n" +
                        "            {\n" +
                        "              runMATLABTests(testResultsPDF:'test-results/results.pdf',\n" +
                        "                             testResultsTAP: 'test-results/results.tap',\n" +
                        "                             testResultsJUnit: 'test-results/results.xml',\n" +
                        "                             testResultsSimulinkTest: 'test-results/results.mldatx',\n" +
                        "                             codeCoverageCobertura: 'code-coverage/coverage.xml',\n" +
                        "                             modelCoverageCobertura: 'model-coverage/coverage.xml')\n" +
                        "            }\n" +
                        "        }\n" +
                        "    }\n" +
                        "}",true));
        WorkflowRun build = project.scheduleBuild2(0).get();
        jenkins.assertBuildStatus(Result.SUCCESS,build);
    }

    // .pdf extension
    @Test
    public void verifyExtForPDF() throws Exception {
        project.setDefinition(new CpsFlowDefinition(
                "pipeline {\n" +
                        "  agent any\n" +
                        "  environment {\n" +
                        "      PATH = \"C:\\\\Program Files\\\\MATLAB\\\\R2020a\\\\bin;${PATH}\"   \n" +
                        "  }\n" +
                        "    stages{\n" +
                        "        stage('Run MATLAB Command') {\n" +
                        "            steps\n" +
                        "            {\n" +
                        "              runMATLABTests(testResultsPDF:'test-results/results')\n" +
                        "            }\n" +
                        "        }\n" +
                        "    }\n" +
                        "}",true));
        WorkflowRun build = project.scheduleBuild2(0).get();
        String build_log = jenkins.getLog(build);
        jenkins.assertLogContains("File extension missing.  Expected '.pdf'", build);
        jenkins.assertBuildStatus(Result.FAILURE,build);
    }

    // invalid filename
    @Test
    public void verifyInvalidFilename() throws Exception {
        project.setDefinition(new CpsFlowDefinition(
                "pipeline {\n" +
                        "  agent any\n" +
                        "  environment {\n" +
                        "      PATH = \"C:\\\\Program Files\\\\MATLAB\\\\R2020a\\\\bin;${PATH}\"   \n" +
                        "  }\n" +
                        "    stages{\n" +
                        "        stage('Run MATLAB Command') {\n" +
                        "            steps\n" +
                        "            {\n" +
                        "              runMATLABTests(testResultsPDF:'abc/x?.pdf')\n" +
                        "            }\n" +
                        "        }\n" +
                        "    }\n" +
                        "}",true));
        WorkflowRun build = project.scheduleBuild2(0).get();
        String build_log = jenkins.getLog(build);
        jenkins.assertLogContains("Unable to write to file", build);
        jenkins.assertBuildStatus(Result.FAILURE,build);
    }

}
