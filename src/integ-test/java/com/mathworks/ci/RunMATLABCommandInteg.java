package com.mathworks.ci;

import com.gargoylesoftware.htmlunit.*;
import com.gargoylesoftware.htmlunit.html.*;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import hudson.tasks.Builder;
import org.junit.*;
import hudson.EnvVars;
import hudson.matrix.Axis;
import hudson.matrix.AxisList;
import hudson.matrix.Combination;
import hudson.matrix.MatrixBuild;
import hudson.matrix.MatrixProject;
import hudson.matrix.MatrixRun;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import hudson.slaves.EnvironmentVariablesNodeProperty;
import hudson.tasks.Builder;


import org.jvnet.hudson.test.JenkinsRule;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.*;


public class RunMATLABCommandInteg {

    private FreeStyleProject project;
    private UseMatlabVersionBuildWrapper buildWrapper;
    private RunMatlabCommandBuilder scriptBuilder;

    @Rule
    public JenkinsRule jenkins = new JenkinsRule();

    @Before
    public void testSetup() throws IOException {

        this.project = jenkins.createFreeStyleProject();
        this.scriptBuilder = new RunMatlabCommandBuilder();
        this.buildWrapper = new UseMatlabVersionBuildWrapper();
    }

    @After
    public void testTearDown() {
        this.project = null;
        this.scriptBuilder = null;
    }

    private String getMatlabroot() throws URISyntaxException {
        String ML_version = TestData.getPropValues("matlab.version");
        String installed_path, MATLAB_ROOT;

        if (System.getProperty("os.name").startsWith("Win")) {
            installed_path = TestData.getPropValues("matlab.windows.installed.path");
            MATLAB_ROOT = installed_path + "\\" + ML_version;
        }
        else {
            installed_path = TestData.getPropValues("matlab.linux.installed.path");
            MATLAB_ROOT = installed_path + "/" + ML_version;
        }
        return MATLAB_ROOT;
    }

    /*
     * Test to verify if Build FAILS when matlab command fails
     */

    @Test
    public void verifyBuildFailureWhenMatlabCommandFails() throws Exception {
        String matlabRoot = getMatlabroot();
        System.out.println(matlabRoot);
        this.buildWrapper.setMatlabRootFolder(matlabRoot);
        project.getBuildWrappersList().add(this.buildWrapper);
        RunMatlabCommandBuilder tester =
                new RunMatlabCommandBuilder();
        tester.setMatlabCommand(TestData.getPropValues("matlab.invalid.command"));
        project.getBuildersList().add(tester);
        FreeStyleBuild build = project.scheduleBuild2(0).get();
        String build_log = jenkins.getLog(build);
        jenkins.assertBuildStatus(Result.FAILURE, build);
    }


    /* Test To Verify if Build passes when matlab command passes
    */
    @Test
    public void verifyBuildPassesWhenMatlabCommandPasses() throws Exception {
        String matlabRoot = getMatlabroot();
        System.out.println(matlabRoot);
        this.buildWrapper.setMatlabRootFolder(matlabRoot);
        project.getBuildWrappersList().add(this.buildWrapper);
        RunMatlabCommandBuilder tester =
                new RunMatlabCommandBuilder();
        tester.setMatlabCommand("version");
        project.getBuildersList().add(tester);
        FreeStyleBuild build = project.scheduleBuild2(0).get();
        String build_log = jenkins.getLog(build);
        jenkins.assertBuildStatus(Result.SUCCESS, build);
    }

    /*
     * Test to verify if Matrix build passes (mock MATLAB).
     */
    @Test
    public void verifyMatrixBuildPasses() throws Exception {
        MatrixProject matrixProject = jenkins.createProject(MatrixProject.class);
        Axis axes = new Axis("VERSION", "R2019b", "R2020a");
        matrixProject.setAxes(new AxisList(axes));
        String matlabRoot = getMatlabroot();
        System.out.println(matlabRoot);
        this.buildWrapper.setMatlabRootFolder(matlabRoot.replace(TestData.getPropValues("matlab.version"), "$VERSION"));
        matrixProject.getBuildWrappersList().add(this.buildWrapper);
        RunMatlabCommandBuilder tester = new RunMatlabCommandBuilder();

        tester.setMatlabCommand((TestData.getPropValues("matlab.command")));
        matrixProject.getBuildersList().add(tester);
        MatrixBuild build = matrixProject.scheduleBuild2(0).get();
        String build_log = jenkins.getLog(build);

        jenkins.assertLogContains("R2019b completed", build);
        jenkins.assertLogContains("R2020a completed", build);
        
        jenkins.assertBuildStatus(Result.SUCCESS, build);
    }
}
