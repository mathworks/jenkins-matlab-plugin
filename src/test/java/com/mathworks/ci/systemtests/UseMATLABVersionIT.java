package com.mathworks.ci.systemtests;

import com.mathworks.ci.MatlabBuildWrapperContent;
import com.mathworks.ci.Message;
import com.mathworks.ci.TestMessage;
import com.mathworks.ci.UseMatlabVersionBuildWrapper;
import com.mathworks.ci.freestyle.RunMatlabCommandBuilder;
import hudson.matrix.*;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import org.htmlunit.WebAssert;
import org.htmlunit.html.HtmlPage;
import org.junit.*;
import org.junit.rules.Timeout;
import org.jvnet.hudson.test.JenkinsRule;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import hudson.tasks.BuildWrapper;

public class UseMATLABVersionIT {
    private FreeStyleProject project;
    private UseMatlabVersionBuildWrapper buildWrapper;

    @Rule
    public Timeout timeout = Timeout.seconds(0);

    @Rule
    public JenkinsRule jenkins = new JenkinsRule();

    @Before
    public void testSetup() throws IOException {
        this.project = jenkins.createFreeStyleProject();
        this.buildWrapper = new UseMatlabVersionBuildWrapper();
    }

    @After
    public void testTearDown() {
        this.project = null;
        this.buildWrapper = null;
    }

    @Test
    public void verifyBuildEnvForMatlab() throws Exception {
        boolean found = false;
        this.buildWrapper.setMatlabBuildWrapperContent(
                new MatlabBuildWrapperContent(Message.getValue("matlab.custom.location"), ""));
        project.getBuildWrappersList().add(this.buildWrapper);
        List<BuildWrapper> bw = project.getBuildWrappersList();
        for (BuildWrapper b : bw) {
            if (b.getDescriptor().getDisplayName()
                    .equalsIgnoreCase(Message.getValue("Buildwrapper.display.name"))) {
                found = true;
            }
        }
        Assert.assertTrue("Build does not have MATLAB build environment", found);
    }

    @Test
    public void verifyEmptyMatlabRootError() throws Exception {
        project.getBuildWrappersList().add(this.buildWrapper);
        HtmlPage page = jenkins.createWebClient().goTo("job/test0/configure");
        WebAssert.assertTextPresent(page, TestMessage.getValue("Builder.matlab.root.empty.error"));
    }

    @Test
    public void verifyValidMatlabNoWarning() throws Exception {
        project.getBuildWrappersList().add(this.buildWrapper);
        this.buildWrapper.setMatlabBuildWrapperContent(
                new MatlabBuildWrapperContent(Message.getValue("matlab.custom.location"), Utilities.getMatlabRoot()));
        HtmlPage page = jenkins.createWebClient().goTo("job/test0/configure");
        WebAssert.assertTextNotPresent(page, TestMessage.getValue("Builder.invalid.matlab.root.warning"));
    }

    @Test
    public void verifyMatrixVariableNoErrorOrWarnning() throws Exception {
        project.getBuildWrappersList().add(this.buildWrapper);
        this.buildWrapper.setMatlabBuildWrapperContent(new MatlabBuildWrapperContent(
                Message.getValue("matlab.custom.location"), "/test/MATLAB/$VERSION"));
        HtmlPage page = jenkins.createWebClient().goTo("job/test0/configure");
        WebAssert.assertTextNotPresent(page, TestMessage.getValue("Builder.invalid.matlab.root.warning"));
    }

    @Test
    public void verifyBuilderFailsForInvalidMATLABPath() throws Exception {
        this.buildWrapper.setMatlabBuildWrapperContent(new MatlabBuildWrapperContent(
        Message.getValue("matlab.custom.location"), "/fake/matlabroot/that/does/not/exist"));
        project.getBuildWrappersList().add(this.buildWrapper);

        RunMatlabCommandBuilder scriptBuilder = new RunMatlabCommandBuilder();
        scriptBuilder.setMatlabCommand("pwd");

        FreeStyleBuild build = project.scheduleBuild2(0).get();
        jenkins.assertBuildStatus(Result.FAILURE, build);
        jenkins.assertLogContains("MatlabNotFoundError", build);
    }

     @Test
     public void verifyDefaultMatlabNotPicked() throws Exception {
        this.buildWrapper.setMatlabBuildWrapperContent(
                new MatlabBuildWrapperContent(Message.getValue("matlab.custom.location"),"/invalid/path/Matlab"));
        project.getBuildWrappersList().add(this.buildWrapper);
        RunMatlabCommandBuilder scriptBuilder = new RunMatlabCommandBuilder();
        scriptBuilder.setMatlabCommand("pwd");
        project.getBuildersList().add(scriptBuilder);
        FreeStyleBuild build = project.scheduleBuild2(0).get();
        jenkins.assertLogContains("MatlabNotFoundError", build);
     }

     @Test
     public void verifyMatrixBuildFails() throws Exception {
        MatrixProject matrixProject = jenkins.createProject(MatrixProject.class);
        Axis axes = new Axis("VERSION", "R2018a", "R2015b");
        matrixProject.setAxes(new AxisList(axes));
        String matlabRoot = "path/to/matlab/R2018a";
        this.buildWrapper.setMatlabBuildWrapperContent(new MatlabBuildWrapperContent(
                Message.getValue("matlab.custom.location"), matlabRoot.replace("R2018b", "$VERSION")));
        matrixProject.getBuildWrappersList().add(this.buildWrapper);

        RunMatlabCommandBuilder scriptBuilder = new RunMatlabCommandBuilder();
        scriptBuilder.setMatlabCommand("pwd");
        matrixProject.getBuildersList().add(scriptBuilder);
        Map<String, String> vals = new HashMap<String, String>();
        vals.put("VERSION", "R2018a");
        Combination c1 = new Combination(vals);
        MatrixRun build = matrixProject.scheduleBuild2(0).get().getRun(c1);
        jenkins.assertBuildStatus(Result.FAILURE, build);
        vals.put("VERSION", "R2015b");
        Combination c2 = new Combination(vals);
        MatrixRun build2 = matrixProject.scheduleBuild2(0).get().getRun(c2);
        jenkins.assertLogContains("MatlabNotFoundError", build2);
        jenkins.assertBuildStatus(Result.FAILURE, build2);
     }
}