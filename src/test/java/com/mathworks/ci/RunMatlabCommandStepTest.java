package com.mathworks.ci;
/**
 * Copyright 2019-2020 The MathWorks, Inc.
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
import hudson.FilePath;
import hudson.slaves.DumbSlave;

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
                new CpsFlowDefinition("node { writeFile text: 'worksapce', file: 'test.txt'\n"
                        + "runMATLABCommand(matlabCommand: 'pwd')}", true));
        WorkflowRun build = project.scheduleBuild2(0).get();
        j.assertLogContains("MATLAB_ROOT", build);
    }

    /*
     * Verify MATLAB is invoked when valid MATLAB is in PATH.
     *
     */

    @Test
    public void verifyMATLABPathSet() throws Exception {
        project.setDefinition(
                new CpsFlowDefinition("node { writeFile text: 'worksapce', file: 'test.txt'\n"
                        + "testMATLABCommand(matlabCommand: 'pwd')}", true));
        WorkflowRun build = project.scheduleBuild2(0).get();
        j.assertLogContains("tester_started", build);
    }

    /*
     * Verify Pipeline script runs on Slave with valid MATLAB
     *
     */

    @Test
    public void verifyPipelineOnSlave() throws Exception {
        DumbSlave s = j.createOnlineSlave();
        project.setDefinition(new CpsFlowDefinition(
                "node('!master') { writeFile text: 'worksapce', file: 'test.txt'\n"
                        + "testMATLABCommand(matlabCommand: 'pwd')}",
                true));

        s.getWorkspaceFor(project);
        WorkflowRun build = project.scheduleBuild2(0).get();
        j.assertLogContains("tester_started", build);
    }

    /*
     * Verify appropriate command is invoked as in pipeline script
     *
     */

    @Test
    public void verifyCommandSamAsScript() throws Exception {
        DumbSlave s = j.createOnlineSlave();
        project.setDefinition(
                new CpsFlowDefinition("node { writeFile text: 'worksapce', file: 'test.txt'\n"
                        + "runMATLABCommand(matlabCommand: 'pwd')}", true));

        s.getWorkspaceFor(project);
        WorkflowRun build = project.scheduleBuild2(0).get();
        j.assertLogContains("pwd", build);
    }

    /*
     * Verify script can run Matrix build
     *
     */

    @Test
    public void verifyMatrixBuild() throws Exception {
        project.setDefinition(
                new CpsFlowDefinition("node { writeFile text: 'worksapce', file: 'test.txt'\n"
                        + "matrix {\n" + "agent any\n" + "axes {\n" + "axis {\n" + "name: 'CMD'\n"
                        + "values: 'pwd','ver'\n }}\n"
                        + "runMATLABCommand(matlabCommand: '${CMD}')}}", true));

        WorkflowRun build = project.scheduleBuild2(0).get();
        j.assertLogContains("pwd", build);
        j.assertLogContains("ver", build);
    }
}
