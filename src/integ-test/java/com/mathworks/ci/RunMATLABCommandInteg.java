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
        String  MATLAB_ROOT;

        if (System.getProperty("os.name").startsWith("Win")) {
            MATLAB_ROOT = TestData.getPropValues("matlab.windows.installed.path");
            // Prints the root folder of MATLAB
            System.out.println(MATLAB_ROOT);
        }
        else if (System.getProperty("os.name").startsWith("Linux")){
            MATLAB_ROOT = TestData.getPropValues("matlab.linux.installed.path");
            // Prints the root folder of MATLAB
            System.out.println(MATLAB_ROOT);
        }
        else {
            MATLAB_ROOT = TestData.getPropValues("matlab.mac.installed.path");
            // Prints the root folder of MATLAB
            System.out.println(MATLAB_ROOT);
        }
        return MATLAB_ROOT;
    }

    /*
     * Test to verify if Build FAILS when matlab command fails
     */

    @Test
    public void verifyBuildFailureWhenMatlabCommandFails() throws Exception {
        String matlabRoot = getMatlabroot();
        this.buildWrapper.setMatlabRootFolder(matlabRoot);
        project.getBuildWrappersList().add(this.buildWrapper);
        RunMatlabCommandBuilder tester =
                new RunMatlabCommandBuilder();
        tester.setMatlabCommand(TestData.getPropValues("matlab.invalid.command"));
        project.getBuildersList().add(tester);
        FreeStyleBuild build = project.scheduleBuild2(0).get();
        jenkins.assertBuildStatus(Result.FAILURE, build);
    }


    /* Test To Verify if Build passes when matlab command passes
    */
    @Test
    public void verifyBuildPassesWhenMatlabCommandPasses() throws Exception {
        String matlabRoot = getMatlabroot();
        this.buildWrapper.setMatlabRootFolder(matlabRoot);
        project.getBuildWrappersList().add(this.buildWrapper);
        RunMatlabCommandBuilder tester =
                new RunMatlabCommandBuilder();
        tester.setMatlabCommand(TestData.getPropValues("matlab.command"));
        project.getBuildersList().add(tester);
        FreeStyleBuild build = project.scheduleBuild2(0).get();
        jenkins.assertBuildStatus(Result.SUCCESS, build);
    }

    /*
     * Test to verify if error is displayed when the Matlab Command is empty
     */
    @Test
    public void verifyMatlabCommandEmptyError() throws Exception{
        RunMatlabCommandBuilder tester =
                new RunMatlabCommandBuilder();
        project.getBuildersList().add(tester);
        HtmlPage page = jenkins.createWebClient().goTo("job/test0/configure");
        WebAssert.assertTextPresent(page,"Enter the Matlab command");
    }

    /*
     * Test to verify if Matrix build fails when MATLAB is not available.
     */
    @Test
    public void verifyMatrixBuildFails() throws Exception {
        MatrixProject matrixProject = jenkins.createProject(MatrixProject.class);
        Axis axes = new Axis("VERSION", "R2018a", "R2018b");
        matrixProject.setAxes(new AxisList(axes));
        String matlabRoot = getMatlabroot();
        this.buildWrapper.setMatlabRootFolder(matlabRoot.replace(TestData.getPropValues("matlab.version"), "$VERSION"));
        matrixProject.getBuildWrappersList().add(this.buildWrapper);

        matrixProject.getBuildersList().add(scriptBuilder);
        Map<String, String> vals = new HashMap<String, String>();
        vals.put("VERSION", "R2018a");
        Combination c1 = new Combination(vals);

        MatrixRun build = matrixProject.scheduleBuild2(0).get().getRun(c1);
        jenkins.assertBuildStatus(Result.FAILURE, build);
        vals.put("VERSION", "R2018b");
        Combination c2 = new Combination(vals);
        MatrixRun build2 = matrixProject.scheduleBuild2(0).get().getRun(c2);
        jenkins.assertBuildStatus(Result.FAILURE, build2);
    }

    /*
     * Test to verify if Matrix build passes .
     */
    @Test
    public void verifyMatrixBuildPasses() throws Exception {
        MatrixProject matrixProject = jenkins.createProject(MatrixProject.class);
        Axis axes = new Axis("VERSION", "R2019a", "R2020a");
        matrixProject.setAxes(new AxisList(axes));
        String matlabRoot = getMatlabroot();
        this.buildWrapper.setMatlabRootFolder(matlabRoot.replace(TestData.getPropValues("matlab.version"), "$VERSION"));
        matrixProject.getBuildWrappersList().add(this.buildWrapper);
        RunMatlabCommandBuilder tester = new RunMatlabCommandBuilder();

        tester.setMatlabCommand("pwd,version");
        matrixProject.getBuildersList().add(tester);
        MatrixBuild build = matrixProject.scheduleBuild2(0).get();

        jenkins.assertLogContains("R2019a completed", build);
        jenkins.assertLogContains("R2020a completed", build);

        jenkins.assertBuildStatus(Result.SUCCESS, build);
    }
}
