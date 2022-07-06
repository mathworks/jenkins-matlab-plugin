package com.mathworks.ci;

import hudson.model.Result;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;
import org.jvnet.hudson.test.JenkinsRule;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class StageFailureExceptionIT {
    private WorkflowJob project;
    private String envScripted;
    private String envDSL;

    @Rule
    public Timeout timeout = Timeout.seconds(0);

    @Rule
    public JenkinsRule jenkins = new JenkinsRule();

    @Before
    public void testSetup() throws IOException {
        this.project = jenkins.createProject(WorkflowJob.class);
        this.envDSL = MatlabRootSetup.getEnvironmentDSL();
        this.envScripted = MatlabRootSetup.getEnvironmentScriptedPipeline();
    }

    /*
     * Utility function which returns the build of the project
     */
    private WorkflowRun getBuild(String script) throws Exception{
        project.setDefinition(new CpsFlowDefinition(script,true));
        WorkflowRun build = project.scheduleBuild2(0).get();
        return build;
    }

    @Test
    public void verifyExceptionisRaisedForStageFailureInScripted() throws Exception{
        String script = "node {\n" +
                            envScripted + "\n" +
                            "stage('Error') { " + "\n" +
                                "runMATLABCommand 'exit(5)'" + "\n" +
                            "}" + "\n" +
                        "}";
        WorkflowRun build = getBuild(script);
        jenkins.assertLogContains("MatlabExecutionException", build);
        jenkins.assertBuildStatus(Result.FAILURE,build);
    }

    @Test
    public void verifyExceptionisRaisedForStageFailureInDSL() throws Exception{
        String script = "pipeline {\n" +
                            "agent any" + "\n" +
                            envDSL + "\n" +
                            "stages{" + "\n" +
                                "stage('Run MATLAB Command') {\n" +
                                    "steps {"+
                                            "runMATLABCommand 'exit(5)'\n" +
                                    "}" + "\n" +
                                "}" + "\n" +
                            "}" + "\n" +
                        "}";
        WorkflowRun build = getBuild(script);
        jenkins.assertLogContains("MatlabExecutionException", build);
        jenkins.assertBuildStatus(Result.FAILURE,build);
    }

    @Test
    public void verifyExceptionMessageInScripted() throws Exception{
        String script = "node {\n" +
                            envScripted + "\n" +
                            "stage('Error') { " + "\n" +
                                "try {" + "\n" +
                                    "runMATLABCommand 'exit(5)'" + "\n" +
                                "}" + "\n" +
                                "catch(exc){" + "\n" +
                                    "echo exc.getMessage()"+ "\n" +
                                "}"+ "\n" +
                            "}" + "\n" +
                        "}";
        WorkflowRun build = getBuild(script);
        jenkins.assertLogContains("Received a nonzero exit code 5 while trying to run MATLAB", build);
        jenkins.assertBuildStatus(Result.SUCCESS,build);

    }

    @Test
    public void verifyExceptionMessageInDSL() throws Exception{
        String script = "pipeline {\n" +
                            "agent any" + "\n" +
                            envDSL + "\n" +
                            "stages{" + "\n" +
                                "stage('Run MATLAB Command') {\n" +
                                    "steps {"+ "\n" +
                                        "script {" + "\n" +
                                            "try{" + "\n" +
                                                "runMATLABCommand 'exit(5)'" + "\n" +
                                            "}" + "\n" +
                                            "catch(exc) {" + "\n" +
                                                "echo exc.getMessage()" + "\n" +
                                            "}" + "\n" +
                                        "}" + "\n" +
                                    "}" + "\n" +
                                "}" + "\n" +
                            "}" + "\n" +
                        "}";
        WorkflowRun build = getBuild(script);
        jenkins.assertLogContains("Received a nonzero exit code 5 while trying to run MATLAB", build);
        jenkins.assertBuildStatus(Result.SUCCESS,build);

    }

    @Test
    public void catchErrorInScripted() throws Exception {
        String script  = "node {\n" +
                envScripted + "\n" +
                "catchError(buildResult: 'SUCCESS', stageResult: 'SUCCESS'){" + "\n"+
                "runMATLABCommand 'exit(2)'"+ "\n"+
                "}" + "\n"+
                "}";
        WorkflowRun build = getBuild(script);
        jenkins.assertLogContains("Received a nonzero exit code 2 while trying to run MATLAB", build);
        jenkins.assertBuildStatus(Result.SUCCESS,build);
    }

    @Test
    public void verifyExceptionIsCaughtUsingcatchErrorINDSL() throws Exception {
        String script = "pipeline {\n" +
                            "agent any" + "\n" +
                            envDSL + "\n" +
                            "stages{" + "\n" +
                                "stage('Run MATLAB Command') {\n" +
                                    "steps {"+ "\n" +
                                        "catchError(buildResult: 'SUCCESS', stageResult: 'SUCCESS'){\n" +
                "                           runMATLABCommand 'exit(5)'\n" +
                "                       }" + "\n" +
                                    "}" + "\n" +
                                "}" + "\n" +
                            "}" + "\n" +
                        "}";
        WorkflowRun build = getBuild(script);
        jenkins.assertLogContains("Received a nonzero exit code 5 while trying to run MATLAB", build);
        jenkins.assertBuildStatus(Result.SUCCESS,build);
    }

    @Test
    public void verifyBuildIsSuccessWhenExceptionIsCaughtInScripted() throws Exception {
        String script = "node {" +
                            envScripted + "\n" +
                            "stage('Error') { " + "\n" +
                                "try {" + "\n" +
                                    "runMATLABCommand 'exit(5)'" + "\n" +
                                "}" + "\n" +
                                "catch(exc){" + "\n" +
                                    "echo exc.getMessage()"+ "\n" +
                                "}"+ "\n" +
                            "}" + "\n" +
                            "stage('script') {"+ "\n" +
                                "runMATLABCommand 'version'" + "\n" +
                            "}"+ "\n" +
                        "}";
        WorkflowRun build = getBuild(script);
        jenkins.assertLogContains("Received a nonzero exit code 5 while trying to run MATLAB", build);
        jenkins.assertLogContains("(script)", build);
        jenkins.assertBuildStatus(Result.SUCCESS,build);
    }

    @Test
    public void verifyBuildIsSuccessWhenExceptionIsCaughtInDSL() throws Exception {
        String script = "pipeline {\n" +
                            "agent any" + "\n" +
                            envDSL + "\n" +
                            "stages{" + "\n" +
                                "stage('Run MATLAB Command') {\n" +
                                    "steps {"+ "\n" +
                                        "catchError(buildResult: 'SUCCESS', stageResult: 'SUCCESS'){\n" +
                                            "runMATLABCommand 'exit(5)'" + "\n" +
                "                       }" + "\n" +
                "}" + "\n" +
                "}" + "\n" +
                "}" + "\n" +
                "}";
        WorkflowRun build = getBuild(script);
        jenkins.assertBuildStatus(Result.SUCCESS,build);
    }
}
