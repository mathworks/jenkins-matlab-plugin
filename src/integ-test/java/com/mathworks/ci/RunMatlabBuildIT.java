package com.mathworks.ci;

import hudson.matrix.*;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;
import org.jvnet.hudson.test.ExtractResourceSCM;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.SingleFileSCM;
import org.jvnet.hudson.test.TestEnvironment;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class RunMatlabBuildIT {
    private FreeStyleProject project;
    private UseMatlabVersionBuildWrapper buildWrapper;
    private RunMatlabBuildBuilder tester;
    @Rule
    public Timeout timeout=Timeout.seconds(0);
    @Rule
    public JenkinsRule jenkins=new JenkinsRule();
    @Before
    public  void testSetup()throws IOException {
        this.project=jenkins.createFreeStyleProject();
        this.tester=new RunMatlabBuildBuilder();
        this.buildWrapper=new UseMatlabVersionBuildWrapper();
        project.setScm(new ExtractResourceSCM(MatlabRootSetup.getTestOnWarningData()));

    }
    @After
    public void testTearDown()
    {
        this.project=null;
        this.tester=null;
    }
    @Test
    public void verifyBuildPassesWhenTaskProvided()throws Exception
    {
        TestEnvironment env=TestEnvironment.get();
        tester.setTasks("test");
        project.getBuildersList().add(tester);
        FreeStyleBuild build=project.scheduleBuild2(0).get();
        jenkins.assertBuildStatus(Result.SUCCESS,build);
        jenkins.assertLogContains("buildtool test",build);
    }
    @Test
    public void verifyBuildPassesWhenTaskNotProvided()throws Exception
    {
        TestEnvironment env=TestEnvironment.get();
        tester.setTasks("");
        project.getBuildersList().add(tester);
        FreeStyleBuild build=project.scheduleBuild2(0).get();
        jenkins.assertBuildStatus(Result.SUCCESS,build);
        jenkins.assertLogContains("buildtool",build);
    }
    @Test
    public void verifyMatrixBuildPassesTaskProvided() throws Exception {
        MatrixProject matrixProject = jenkins.createProject(MatrixProject.class);
        String MATLABVersion1 = TestData.getPropValues("matlab.version");
        String MATLABVersion2 = TestData.getPropValues("matlab.matrix.version");
        Axis axes = new Axis("VERSION", MATLABVersion1, MATLABVersion2);
        matrixProject.setAxes(new AxisList(axes));
        String matlabRoot = MatlabRootSetup.getMatlabRoot();
        this.buildWrapper.setMatlabBuildWrapperContent(new MatlabBuildWrapperContent(Message.getValue("matlab.custom.location"), matlabRoot.replace(MATLABVersion1, "$VERSION")));
        matrixProject.getBuildWrappersList().add(this.buildWrapper);
        matrixProject.setScm(new ExtractResourceSCM(MatlabRootSetup.getTestOnWarningData()));

        tester.setTasks("test");
        matrixProject.getBuildersList().add(tester);
        MatrixBuild build = matrixProject.scheduleBuild2(0).get();

        Map<String, String> vals = new HashMap<String, String>();
        vals.put("VERSION", MATLABVersion1);
        Combination c = new Combination(vals);
        MatrixRun run = build.getRun(c);
        jenkins.assertLogContains("buildtool test",run);
        vals.put("VERSION", MATLABVersion2);
        c = new Combination(vals);
        run = build.getRun(c);

        jenkins.assertLogContains("R2022b completed with result SUCCESS",build);
        jenkins.assertLogContains("R2023a completed with result SUCCESS",build);
        jenkins.assertLogContains("buildtool test",run);
        jenkins.assertBuildStatus(Result.SUCCESS, build);

    }
    @Test
    public void verifyMatrixBuildPassesTaskNotProvided() throws Exception {
        MatrixProject matrixProject = jenkins.createProject(MatrixProject.class);
        String MATLABVersion1 = TestData.getPropValues("matlab.version");
        String MATLABVersion2 = TestData.getPropValues("matlab.matrix.version");
        Axis axes = new Axis("VERSION", MATLABVersion1, MATLABVersion2);
        matrixProject.setAxes(new AxisList(axes));
        String matlabRoot = MatlabRootSetup.getMatlabRoot();
        this.buildWrapper.setMatlabBuildWrapperContent(new MatlabBuildWrapperContent(Message.getValue("matlab.custom.location"), matlabRoot.replace(MATLABVersion1, "$VERSION")));
        matrixProject.getBuildWrappersList().add(this.buildWrapper);
        matrixProject.setScm(new ExtractResourceSCM(MatlabRootSetup.getTestOnWarningData()));

        tester.setTasks("");
        matrixProject.getBuildersList().add(tester);
        MatrixBuild build = matrixProject.scheduleBuild2(0).get();

        Map<String, String> vals = new HashMap<String, String>();
        vals.put("VERSION", MATLABVersion1);
        Combination c = new Combination(vals);
        MatrixRun run = build.getRun(c);
        jenkins.assertLogContains("buildtool",run);

        vals.put("VERSION", MATLABVersion2);
        c = new Combination(vals);
        run = build.getRun(c);

        jenkins.assertBuildStatus(Result.SUCCESS, build);
        jenkins.assertLogContains("R2022b completed with result SUCCESS",build);
        jenkins.assertLogContains("R2023a completed with result SUCCESS",build);
        jenkins.assertLogContains("buildtool",run);

    }


}
