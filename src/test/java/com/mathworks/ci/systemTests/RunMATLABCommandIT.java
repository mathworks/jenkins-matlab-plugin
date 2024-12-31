package com.mathworks.ci.systemTests;

import com.mathworks.ci.*;
import com.mathworks.ci.freestyle.RunMatlabCommandBuilder;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import org.junit.*;
import org.junit.rules.Timeout;
import org.jvnet.hudson.test.JenkinsRule;
import java.io.IOException;


public class RunMATLABCommandIT {

    private FreeStyleProject project;
    private UseMatlabVersionBuildWrapper buildWrapper;
    private RunMatlabCommandBuilder scriptBuilder;

    @Rule
    public Timeout timeout = Timeout.seconds(0);

    @Rule
    public JenkinsRule jenkins = new JenkinsRule();

    @BeforeClass
    public static void checkMatlabRoot() {
        // Check if the MATLAB_ROOT environment variable is defined
        String matlabRoot = System.getenv("MATLAB_ROOT");
        Assume.assumeTrue("Not running tests as MATLAB_ROOT environment variable is not defined", matlabRoot != null && !matlabRoot.isEmpty());
    }

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

    /*
     * Test to verify if Build FAILS when matlab command fails
     */

    @Test
    public void verifyBuildFailureWhenMatlabCommandFails() throws Exception {
        this.buildWrapper.setMatlabBuildWrapperContent(new MatlabBuildWrapperContent(Message.getValue("matlab.custom.location"), MatlabRootSetup.getMatlabRoot()));
        project.getBuildWrappersList().add(this.buildWrapper);
        RunMatlabCommandBuilder tester =
                new RunMatlabCommandBuilder();
        tester.setMatlabCommand("apple");
        project.getBuildersList().add(tester);
        FreeStyleBuild build = project.scheduleBuild2(0).get();
        jenkins.assertLogContains("apple", build);
        jenkins.assertBuildStatus(Result.FAILURE, build);
    }


    /* Test To Verify if Build passes when matlab command passes
    */
    @Test
    public void verifyBuildPassesWhenMatlabCommandPasses() throws Exception {
       this.buildWrapper.setMatlabBuildWrapperContent(new MatlabBuildWrapperContent(Message.getValue("matlab.custom.location"), MatlabRootSetup.getMatlabRoot()));
       project.getBuildWrappersList().add(this.buildWrapper);
        RunMatlabCommandBuilder tester =
                new RunMatlabCommandBuilder();
        tester.setMatlabCommand("disp 'apple'");
        project.getBuildersList().add(tester);
        FreeStyleBuild build = project.scheduleBuild2(0).get();
        jenkins.assertBuildStatus(Result.SUCCESS, build);
        jenkins.assertLogContains("apple", build);
    }
}
