package com.mathworks.ci;

/**
 * Copyright 2022-2024 The MathWorks, Inc.
 *  
 */

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;
import org.jvnet.hudson.test.JenkinsRule;
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

import com.mathworks.ci.freestyle.RunMatlabBuildBuilder;
import com.mathworks.ci.freestyle.options.StartupOptions;
import com.mathworks.ci.freestyle.options.BuildOptions;

public class RunMatlabBuildBuilderTest {

    private static String matlabExecutorAbsolutePath;
    private FreeStyleProject project;
    private UseMatlabVersionBuildWrapper buildWrapper;
    private RunMatlabBuildBuilder scriptBuilder;
    private static URL url;
    private static String FileSeperator;
    private static String VERSION_INFO_XML_FILE = "VersionInfo.xml";

    @Rule
    public JenkinsRule jenkins = new JenkinsRule();

    @Rule
    public Timeout globalTimeout = Timeout.seconds(500);

    @BeforeClass
    public static void classSetup() throws URISyntaxException, IOException {
        ClassLoader classLoader = RunMatlabBuildBuilderTest.class.getClassLoader();
        if (!System.getProperty("os.name").startsWith("Win")) {
            FileSeperator = "/";
            url = classLoader.getResource("com/mathworks/ci/linux/bin/matlab.sh");
            try {
                matlabExecutorAbsolutePath = new File(url.toURI()).getAbsolutePath();
                System.out.println("THE EXECUTOR PATH IS" + matlabExecutorAbsolutePath);

                // Need to do this operation due to bug in maven Resource copy plugin [
                // https://issues.apache.org/jira/browse/MRESOURCES-132 ]

                ProcessBuilder pb = new ProcessBuilder("chmod", "755", matlabExecutorAbsolutePath);
                pb.start();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (URISyntaxException e) {
                e.printStackTrace();
            }
        } else {
            FileSeperator = "\\";
            url = classLoader.getResource("com/mathworks/ci/win/bin/matlab.bat");
            matlabExecutorAbsolutePath = new File(url.toURI()).getAbsolutePath();
        }
    }

    @Before
    public void testSetup() throws IOException {
        this.project = jenkins.createFreeStyleProject();
        this.scriptBuilder = new RunMatlabBuildBuilder();
        this.buildWrapper = new UseMatlabVersionBuildWrapper();
    }

    @After
    public void testTearDown() {
        this.project = null;
        this.scriptBuilder = null;
    }

    private String getMatlabroot(String version) throws URISyntaxException {
        String defaultVersionInfo = "versioninfo/R2017a/" + VERSION_INFO_XML_FILE;
        String userVersionInfo = "versioninfo/" + version + "/" + VERSION_INFO_XML_FILE;
        URL matlabRootURL = Optional.ofNullable(getResource(userVersionInfo))
                .orElseGet(() -> getResource(defaultVersionInfo));
        File matlabRoot = new File(matlabRootURL.toURI());
        return matlabRoot.getAbsolutePath().replace(FileSeperator + VERSION_INFO_XML_FILE, "")
                .replace("R2017a", version);
    }

    private URL getResource(String resource) {
        return RunMatlabBuildBuilderTest.class.getClassLoader().getResource(resource);
    }

    /*
     * Test to verify build step contains "Run MATLAB Build" option.
     */
    @Test
    public void verifyBuildStepWithRunMatlab() throws Exception {
        boolean found = false;
        project.getBuildersList().add(scriptBuilder);
        List<Builder> bl = project.getBuildersList();
        for (Builder b : bl) {
            if (b.getDescriptor().getDisplayName().equalsIgnoreCase(
                    TestMessage.getValue("Builder.build.builder.display.name"))) {
                found = true;
            }
        }
        Assert.assertTrue("Build step does not contain Run MATLAB Build option", found);
    }

    /*
     * Test to verify MATLAB is launched using the default MATLAB runner script.
     */
    @Test
    public void verifyMATLABlaunchedWithDefaultArguments() throws Exception {
        this.buildWrapper.setMatlabBuildWrapperContent(
                new MatlabBuildWrapperContent(Message.getValue("matlab.custom.location"), getMatlabroot("R2017a")));
        project.getBuildWrappersList().add(this.buildWrapper);
        scriptBuilder.setTasks("");
        project.getBuildersList().add(this.scriptBuilder);
        FreeStyleBuild build = project.scheduleBuild2(0).get();
        jenkins.assertLogContains("buildtool", build);
    }

    /*
     * Test to verify MATLAB is launched always from build workspace.
     */
    @Test
    public void verifyMATLABlaunchedfromWorkspace() throws Exception {
        this.buildWrapper.setMatlabBuildWrapperContent(
                new MatlabBuildWrapperContent(Message.getValue("matlab.custom.location"), getMatlabroot("R2017a")));
        project.getBuildWrappersList().add(this.buildWrapper);
        scriptBuilder.setTasks("");
        project.getBuildersList().add(this.scriptBuilder);
        FreeStyleBuild build = project.scheduleBuild2(0).get();
        String workspace = build.getWorkspace().getName();
        jenkins.assertLogContains("[" + workspace + "]", build);
    }

    /*
     * Test to verify job fails when invalid MATLAB path is provided and Exception
     * is thrown
     */
    @Test
    public void verifyBuilderFailsForInvalidMATLABPath() throws Exception {
        this.buildWrapper.setMatlabBuildWrapperContent(new MatlabBuildWrapperContent(
                Message.getValue("matlab.custom.location"), "/fake/matlabroot/that/does/not/exist"));
        project.getBuildWrappersList().add(this.buildWrapper);
        scriptBuilder.setTasks("");
        project.getBuildersList().add(this.scriptBuilder);
        FreeStyleBuild build = project.scheduleBuild2(0).get();
        jenkins.assertBuildStatus(Result.FAILURE, build);
    }

    /*
     * Test to verify build fails when MATLAB build fails
     */
    @Test
    public void verifyBuildFailureWhenMatlabBuildFails() throws Exception {
        this.buildWrapper.setMatlabBuildWrapperContent(
                new MatlabBuildWrapperContent(Message.getValue("matlab.custom.location"), getMatlabroot("R2018b")));
        project.getBuildWrappersList().add(this.buildWrapper);
        RunMatlabBuildBuilderTester tester = new RunMatlabBuildBuilderTester(matlabExecutorAbsolutePath,
                "-positiveFail");
        scriptBuilder.setTasks("");
        project.getBuildersList().add(tester);
        FreeStyleBuild build = project.scheduleBuild2(0).get();
        jenkins.assertBuildStatus(Result.FAILURE, build);
    }

    /*
     * Test to verify build suceeds when matlab build suceeds
     */
    @Test
    public void verifyBuildPassesWhenMatlabBuildPasses() throws Exception {
        this.buildWrapper.setMatlabBuildWrapperContent(
                new MatlabBuildWrapperContent(Message.getValue("matlab.custom.location"), getMatlabroot("R2018b")));
        project.getBuildWrappersList().add(this.buildWrapper);
        RunMatlabBuildBuilderTester tester = new RunMatlabBuildBuilderTester(matlabExecutorAbsolutePath, "-positive");
        scriptBuilder.setTasks("");
        project.getBuildersList().add(tester);
        FreeStyleBuild build = project.scheduleBuild2(0).get();
        jenkins.assertBuildStatus(Result.SUCCESS, build);

    }

    /*
     * Test to verify builder correctly sets tasks that user entered.
     */
    @Test
    public void verifyBuildPicksTheCorrectBuildBatch() throws Exception {
        this.buildWrapper.setMatlabBuildWrapperContent(
                new MatlabBuildWrapperContent(Message.getValue("matlab.custom.location"), getMatlabroot("R2018b")));
        project.getBuildWrappersList().add(this.buildWrapper);
        scriptBuilder.setTasks("compile");
        project.getBuildersList().add(this.scriptBuilder);
        FreeStyleBuild build = project.scheduleBuild2(0).get();
        jenkins.assertLogContains("Generating MATLAB script with content", build);
        jenkins.assertLogContains("buildtool", build);
        jenkins.assertLogContains("compile", build);
    }

    /*
     * Test to verify builder correctly sets startup options that user entered.
     */
    @Test
    public void verifyBuildPicksTheCorrectStartupOptions() throws Exception {
        this.buildWrapper.setMatlabBuildWrapperContent(
                new MatlabBuildWrapperContent(Message.getValue("matlab.custom.location"), getMatlabroot("R2018b")));
        project.getBuildWrappersList().add(this.buildWrapper);
        scriptBuilder.setTasks("");
        scriptBuilder.setStartupOptions(new StartupOptions("-nojvm -uniqueoption"));
        project.getBuildersList().add(this.scriptBuilder);
        FreeStyleBuild build = project.scheduleBuild2(0).get();
        jenkins.assertLogContains("Generating MATLAB script with content", build);
        jenkins.assertLogContains("-nojvm -uniqueoption", build);
    }

    /*
     * Test to verify builder correctly sets build options that user entered.
     */
    @Test
    public void verifyBuildPicksTheCorrectBuildOptions() throws Exception {
        this.buildWrapper.setMatlabBuildWrapperContent(
                new MatlabBuildWrapperContent(Message.getValue("matlab.custom.location"), getMatlabroot("R2018b")));
        project.getBuildWrappersList().add(this.buildWrapper);
        scriptBuilder.setTasks("");
        scriptBuilder.setBuildOptions(new BuildOptions("-continueOnFailure -skip compile"));
        project.getBuildersList().add(this.scriptBuilder);
        FreeStyleBuild build = project.scheduleBuild2(0).get();
        jenkins.assertLogContains("Generating MATLAB script with content", build);
        jenkins.assertLogContains("-continueOnFailure -skip compile", build);
    }

    /*
     * Test to verify if MATLAB scratch file is not generated in workspace for this
     * builder.
     */
    @Test
    public void verifyMATLABscratchFileNotGenerated() throws Exception {
        this.buildWrapper.setMatlabBuildWrapperContent(
                new MatlabBuildWrapperContent(Message.getValue("matlab.custom.location"), getMatlabroot("R2018b")));
        project.getBuildWrappersList().add(this.buildWrapper);
        scriptBuilder.setTasks("");
        project.getBuildersList().add(this.scriptBuilder);
        FreeStyleBuild build = project.scheduleBuild2(0).get();
        File matlabRunner = new File(build.getWorkspace() + File.separator + "runMatlabTests.m");
        Assert.assertFalse(matlabRunner.exists());
    }

    /*
     * Test to verify build supports resolving environment variable (For matrix
     * builds).
     */
    @Test
    public void verifyBuildSupportsEnvVar() throws Exception {
        EnvironmentVariablesNodeProperty prop = new EnvironmentVariablesNodeProperty();
        EnvVars var = prop.getEnvVars();
        var.put("TASKS", "compile");
        var.put("BUILD_OPTIONS", "-continueOnFailure -skip test");
        jenkins.jenkins.getGlobalNodeProperties().add(prop);
        this.buildWrapper.setMatlabBuildWrapperContent(
                new MatlabBuildWrapperContent(Message.getValue("matlab.custom.location"), getMatlabroot("R2018b")));
        project.getBuildWrappersList().add(this.buildWrapper);
        scriptBuilder.setTasks("$TASKS");
        scriptBuilder.setBuildOptions(new BuildOptions("$BUILD_OPTIONS"));
        project.getBuildersList().add(scriptBuilder);
        FreeStyleBuild build = project.scheduleBuild2(0).get();
        jenkins.assertLogContains("compile", build);
        jenkins.assertLogContains("-continueOnFailure -skip test", build);
    }

    /*
     * Test to verify if appropriate MATLAB runner file is copied in workspace.
     * 
     * NOTE: This test assumes there is no MATLAB installed and is not on System
     * Path.
     * 
     */
    @Test
    public void verifyMATLABrunnerFileGenerated() throws Exception {
        this.buildWrapper.setMatlabBuildWrapperContent(
                new MatlabBuildWrapperContent(Message.getValue("matlab.custom.location"), getMatlabroot("R2018b")));
        project.getBuildWrappersList().add(this.buildWrapper);
        scriptBuilder.setTasks("");
        project.getBuildersList().add(scriptBuilder);
        FreeStyleBuild build = project.scheduleBuild2(0).get();
        jenkins.assertLogContains("run-matlab-command", build);
    }

    /*
     * Verify default MATLAB is not picked if invalid MATLAB path is provided
     */
    @Test
    public void verifyDefaultMatlabNotPicked() throws Exception {
        this.buildWrapper.setMatlabBuildWrapperContent(
                new MatlabBuildWrapperContent(Message.getValue("matlab.custom.location"), getMatlabroot("R2020b")));
        project.getBuildWrappersList().add(this.buildWrapper);
        scriptBuilder.setTasks("");
        project.getBuildersList().add(scriptBuilder);
        FreeStyleBuild build = project.scheduleBuild2(0).get();
        jenkins.assertLogContains("MatlabNotFoundError", build);
    }

    /*
     * Test to verify if Matrix build fails when MATLAB is not available.
     *
     * NOTE: This test assumes there is no MATLAB installed and is not on System
     * Path.
     * 
     */
    // Disabling test as it is flaky
    public void verifyMatrixBuildFails() throws Exception {
        MatrixProject matrixProject = jenkins.createProject(MatrixProject.class);
        Axis axes = new Axis("VERSION", "R2018a", "R2015b");
        matrixProject.setAxes(new AxisList(axes));
        String matlabRoot = getMatlabroot("R2018b");
        this.buildWrapper.setMatlabBuildWrapperContent(new MatlabBuildWrapperContent(
                Message.getValue("matlab.custom.location"), matlabRoot.replace("R2018b", "$VERSION")));
        matrixProject.getBuildWrappersList().add(this.buildWrapper);

        scriptBuilder.setTasks("");
        matrixProject.getBuildersList().add(scriptBuilder);

        // Check for first matrix combination.
        Map<String, String> vals = new HashMap<String, String>();
        vals.put("VERSION", "R2018a");
        Combination c1 = new Combination(vals);
        MatrixRun build1 = matrixProject.scheduleBuild2(0).get().getRun(c1);

        jenkins.assertLogContains("buildtool", build1);
        jenkins.assertBuildStatus(Result.FAILURE, build1);

        // Check for second Matrix combination
        vals.put("VERSION", "R2015b");
        Combination c2 = new Combination(vals);
        MatrixRun build2 = matrixProject.scheduleBuild2(0).get().getRun(c2);

        jenkins.assertLogContains("MatlabNotFoundError", build2);
        jenkins.assertBuildStatus(Result.FAILURE, build2);
    }

    /*
     * Test to verify if Matrix build passes (mock MATLAB).
     */
    @Test
    public void verifyMatrixBuildPasses() throws Exception {
        MatrixProject matrixProject = jenkins.createProject(MatrixProject.class);
        Axis axes = new Axis("VERSION", "R2018a", "R2018b");
        matrixProject.setAxes(new AxisList(axes));
        String matlabRoot = getMatlabroot("R2018b");
        this.buildWrapper.setMatlabBuildWrapperContent(new MatlabBuildWrapperContent(
                Message.getValue("matlab.custom.location"), matlabRoot.replace("R2018b", "$VERSION")));
        matrixProject.getBuildWrappersList().add(this.buildWrapper);
        RunMatlabBuildBuilderTester tester = new RunMatlabBuildBuilderTester(matlabExecutorAbsolutePath,
                "-positive");

        tester.setTasks("");
        matrixProject.getBuildersList().add(tester);
        MatrixBuild build = matrixProject.scheduleBuild2(0).get();

        jenkins.assertLogContains("R2018a completed", build);
        jenkins.assertLogContains("R2018b completed", build);
        jenkins.assertBuildStatus(Result.SUCCESS, build);
    }
}
