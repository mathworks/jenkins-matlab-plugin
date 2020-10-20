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

public class RunPipelineScript {

    private WorkflowJob project;

    @Rule
    public JenkinsRule jenkins = new JenkinsRule();

    @Before
    public void testSetup() throws IOException {
        this.project = jenkins.createProject(WorkflowJob.class);
    }

    @Test
    public void verifyBuildPassesWhenMatlabCommandPasses() throws Exception {
        project.setDefinition(new CpsFlowDefinition(
                "node {runMATLABCommand \"version\"}",true));
        WorkflowRun build = project.scheduleBuild2(0).get();
        jenkins.assertBuildStatus(Result.SUCCESS, build);
    }

    @Test
    public void verifyBuildFailsWhenMatlabCommandFails() throws Exception {
        project.setDefinition(new CpsFlowDefinition(
                                    "node {runMATLABCommand \"apple\"}", true));
        WorkflowRun build = project.scheduleBuild2(0).get();
        jenkins.assertLogContains("apple", build);
        jenkins.assertBuildStatus(Result.FAILURE, build);
    }

    @Test
    public void verifyBuildFailsWhenDSLNameFails() throws Exception {
        project.setDefinition(new CpsFlowDefinition(
                                "node {runMatlabCommand \"pwd\"}",true));
        WorkflowRun build = project.scheduleBuild2(0).get();
        String build_log = jenkins.getLog(build);
        jenkins.assertLogContains("No such DSL method",build);
        jenkins.assertBuildStatus(Result.FAILURE,build);
    }
}
