package com.mathworks.ci;

/**
 * Copyright 2019-2024 The MathWorks, Inc.
 * 
 * Test class for RunMatlabCommandBuilderTest
 * 
 */

import org.htmlunit.WebAssert;
import org.htmlunit.html.HtmlCheckBoxInput;
import org.htmlunit.html.HtmlPage;
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

import com.mathworks.ci.freestyle.RunMatlabCommandBuilder;
import com.mathworks.ci.freestyle.options.StartupOptions;

public class RunMatlabCommandBuilderTest {

    private static String matlabExecutorAbsolutePath;
    private FreeStyleProject project;
    private UseMatlabVersionBuildWrapper buildWrapper;
    private RunMatlabCommandBuilder scriptBuilder;
    private static URL url;
    private static String FileSeperator;
    private static String VERSION_INFO_XML_FILE = "VersionInfo.xml";

    @Rule
    public JenkinsRule jenkins = new JenkinsRule();

    @Rule
    public Timeout globalTimeout = Timeout.seconds(500);

    @BeforeClass
    public static void classSetup() throws URISyntaxException, IOException {
        ClassLoader classLoader = RunMatlabCommandBuilderTest.class.getClassLoader();
        if (!System.getProperty("os.name").startsWith("Win")) {
            FileSeperator = "/";
            url = classLoader.getResource("com/mathworks/ci/linux/bin/matlab.sh");
            try {
                matlabExecutorAbsolutePath = new File(url.toURI()).getAbsolutePath();

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
        this.scriptBuilder = new RunMatlabCommandBuilder();
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
        return RunMatlabTestsBuilderTest.class.getClassLoader().getResource(resource);
    }

    /*
     * Test Case to verify if Build step contains "Run MATLAB Command" option.
     */
    @Test
    public void verifyBuildStepWithRunMatlab() throws Exception {
        boolean found = false;
        scriptBuilder.setMatlabCommand("");
        project.getBuildersList().add(scriptBuilder);
        List<Builder> bl = project.getBuildersList();
        for (Builder b : bl) {
            if (b.getDescriptor().getDisplayName().equalsIgnoreCase(
                    TestMessage.getValue("Builder.matlab.script.builder.display.name"))) {
                found = true;
            }
        }
        Assert.assertTrue("Build step does not contain Run MATLAB Command option", found);
    }

    /*
     * Test To verify MATLAB is launched using the default matlab runner binary.
     * 
     */

    @Test
    public void verifyMATLABlaunchedWithDefaultArguments() throws Exception {
        this.buildWrapper.setMatlabBuildWrapperContent(
                new MatlabBuildWrapperContent(Message.getValue("matlab.custom.location"), getMatlabroot("R2017a")));
        project.getBuildWrappersList().add(this.buildWrapper);
        scriptBuilder.setMatlabCommand("pwd");
        project.getBuildersList().add(this.scriptBuilder);
        FreeStyleBuild build = project.scheduleBuild2(0).get();
        jenkins.assertLogContains("run-matlab-command", build);
    }

    /*
     * Test To verify MATLAB is launched always from build workspace.
     * 
     */

    @Test
    public void verifyMATLABlaunchedfromWorkspace() throws Exception {
        this.buildWrapper.setMatlabBuildWrapperContent(
                new MatlabBuildWrapperContent(Message.getValue("matlab.custom.location"), getMatlabroot("R2017a")));
        project.getBuildWrappersList().add(this.buildWrapper);
        scriptBuilder.setMatlabCommand("pwd");
        project.getBuildersList().add(this.scriptBuilder);
        FreeStyleBuild build = project.scheduleBuild2(0).get();
        String workspace = build.getWorkspace().getName();
        jenkins.assertLogContains(File.separator + workspace, build);
    }

    /*
     * Test to verify if job fails when invalid MATLAB path is provided and
     * Exception is thrown
     */

    @Test
    public void verifyBuilderFailsForInvalidMATLABPath() throws Exception {
        this.buildWrapper.setMatlabBuildWrapperContent(new MatlabBuildWrapperContent(
                Message.getValue("matlab.custom.location"), "/fake/matlabroot/that/does/not/exist"));
        project.getBuildWrappersList().add(this.buildWrapper);
        scriptBuilder.setMatlabCommand("pwd");
        project.getBuildersList().add(this.scriptBuilder);
        FreeStyleBuild build = project.scheduleBuild2(0).get();
        jenkins.assertBuildStatus(Result.FAILURE, build);
    }

    /*
     * Test to verify if Build FAILS when matlab command fails
     */

    @Test
    public void verifyBuildFailureWhenMatlabCommandFails() throws Exception {
        this.buildWrapper.setMatlabBuildWrapperContent(
                new MatlabBuildWrapperContent(Message.getValue("matlab.custom.location"), getMatlabroot("R2018b")));
        project.getBuildWrappersList().add(this.buildWrapper);
        RunMatlabCommandBuilderTester tester = new RunMatlabCommandBuilderTester(matlabExecutorAbsolutePath,
                "-positiveFail");
        tester.setMatlabCommand("pp");
        project.getBuildersList().add(tester);
        FreeStyleBuild build = project.scheduleBuild2(0).get();
        jenkins.assertBuildStatus(Result.FAILURE, build);
    }

    /*
     * Test to verify if Build FAILS when matlab command fails
     */

    @Test
    public void verifyBuildPassesWhenMatlabCommandPasses() throws Exception {
        this.buildWrapper.setMatlabBuildWrapperContent(
                new MatlabBuildWrapperContent(Message.getValue("matlab.custom.location"), getMatlabroot("R2018b")));
        project.getBuildWrappersList().add(this.buildWrapper);
        RunMatlabCommandBuilderTester tester = new RunMatlabCommandBuilderTester(matlabExecutorAbsolutePath,
                "-positive");
        tester.setMatlabCommand("pwd");
        project.getBuildersList().add(tester);
        FreeStyleBuild build = project.scheduleBuild2(0).get();
        jenkins.assertBuildStatus(Result.SUCCESS, build);

    }

    /*
     * Test to verify Builder picks the exact command that user entered.
     * 
     */

    @Test
    public void verifyBuildPicksTheCorretCommandBatch() throws Exception {
        this.buildWrapper.setMatlabBuildWrapperContent(
                new MatlabBuildWrapperContent(Message.getValue("matlab.custom.location"), getMatlabroot("R2018b")));
        project.getBuildWrappersList().add(this.buildWrapper);
        scriptBuilder.setMatlabCommand("pwd");
        project.getBuildersList().add(this.scriptBuilder);
        FreeStyleBuild build = project.scheduleBuild2(0).get();
        jenkins.assertLogContains("run-matlab-command", build);
        jenkins.assertLogContains("Generating MATLAB script with content", build);
        jenkins.assertLogContains("pwd", build);
    }

    /*
     * Test to verify Builder picks the exact startup options that user entered.
     * 
     */

    @Test
    public void verifyBuildPicksTheCorrectStartupOptions() throws Exception {
        this.buildWrapper.setMatlabBuildWrapperContent(
                new MatlabBuildWrapperContent(Message.getValue("matlab.custom.location"), getMatlabroot("R2018b")));
        project.getBuildWrappersList().add(this.buildWrapper);
        scriptBuilder.setMatlabCommand("pwd");
        scriptBuilder.setStartupOptions(new StartupOptions("-nojvm -uniqueoption"));
        project.getBuildersList().add(this.scriptBuilder);
        FreeStyleBuild build = project.scheduleBuild2(0).get();
        jenkins.assertLogContains("run-matlab-command", build);
        jenkins.assertLogContains("Generating MATLAB script with content", build);
        jenkins.assertLogContains("pwd", build);
        jenkins.assertLogContains("-nojvm -uniqueoption", build);
    }

    /*
     * Test to verify if MATALB scratch file is not generated in workspace for this
     * builder.
     */
    @Test
    public void verifyMATLABscratchFileNotGenerated() throws Exception {
        this.buildWrapper.setMatlabBuildWrapperContent(
                new MatlabBuildWrapperContent(Message.getValue("matlab.custom.location"), getMatlabroot("R2018b")));
        project.getBuildWrappersList().add(this.buildWrapper);
        scriptBuilder.setMatlabCommand("pwd");
        project.getBuildersList().add(this.scriptBuilder);
        FreeStyleBuild build = project.scheduleBuild2(0).get();
        File matlabRunner = new File(build.getWorkspace() + File.separator + "runMatlabTests.m");
        Assert.assertFalse(matlabRunner.exists());
    }

    /*
     * Test to verify command supports resolving environment variable (For MATRIX
     * builds).
     * 
     */
    @Test
    public void verifyCommandSupportsEnvVar() throws Exception {
        EnvironmentVariablesNodeProperty prop = new EnvironmentVariablesNodeProperty();
        EnvVars var = prop.getEnvVars();
        var.put("PWDCMD", "pwd");
        jenkins.jenkins.getGlobalNodeProperties().add(prop);
        this.buildWrapper.setMatlabBuildWrapperContent(
                new MatlabBuildWrapperContent(Message.getValue("matlab.custom.location"), getMatlabroot("R2018b")));
        project.getBuildWrappersList().add(this.buildWrapper);
        scriptBuilder.setMatlabCommand("$PWDCMD");
        project.getBuildersList().add(scriptBuilder);
        FreeStyleBuild build = project.scheduleBuild2(0).get();
        jenkins.assertLogContains("pwd", build);
    }

    /*
     * Test to verify if appropriate MATALB runner file is copied in workspace.
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
        scriptBuilder.setMatlabCommand("pwd");
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
        scriptBuilder.setMatlabCommand("pwd");
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
    // Disabling this as it is a flaky test
    public void verifyMatrixBuildFails() throws Exception {
        MatrixProject matrixProject = jenkins.createProject(MatrixProject.class);
        Axis axes = new Axis("VERSION", "R2018a", "R2015b");
        matrixProject.setAxes(new AxisList(axes));
        String matlabRoot = getMatlabroot("R2018b");
        this.buildWrapper.setMatlabBuildWrapperContent(new MatlabBuildWrapperContent(
                Message.getValue("matlab.custom.location"), matlabRoot.replace("R2018b", "$VERSION")));
        matrixProject.getBuildWrappersList().add(this.buildWrapper);

        scriptBuilder.setMatlabCommand("pwd");
        matrixProject.getBuildersList().add(scriptBuilder);
        Map<String, String> vals = new HashMap<String, String>();
        vals.put("VERSION", "R2018a");
        Combination c1 = new Combination(vals);
        MatrixRun build = matrixProject.scheduleBuild2(0).get().getRun(c1);
        jenkins.assertLogContains("run-matlab-command", build);
        jenkins.assertBuildStatus(Result.FAILURE, build);
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
        RunMatlabCommandBuilderTester tester = new RunMatlabCommandBuilderTester(matlabExecutorAbsolutePath,
                "-positive");

        tester.setMatlabCommand("pwd");
        matrixProject.getBuildersList().add(tester);
        MatrixBuild build = matrixProject.scheduleBuild2(0).get();

        jenkins.assertLogContains("R2018a completed", build);
        jenkins.assertLogContains("R2018b completed", build);
        jenkins.assertBuildStatus(Result.SUCCESS, build);
    }

    /*
     * Test to verify if command parses succesfully when multiple combinations of
     * characters are passed. (candidate for integ-tests once integrated)
     */
    public void verifyMultispecialChar() throws Exception {
        final String actualCommand = "!\"\"#$%&'()*+,-./0123456789:;<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\]^_`abcdefghijklmnopqrstuvwxyz{|}~";
        final String expectedCommand = "!\"#$%&'()*+,-./0123456789:;<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\]^_`abcdefghijklmnopqrstuvwxyz{|}~";
        this.buildWrapper.setMatlabBuildWrapperContent(
                new MatlabBuildWrapperContent(Message.getValue("matlab.custom.location"), getMatlabroot("R2018b")));

        project.getBuildWrappersList().add(this.buildWrapper);
        scriptBuilder.setMatlabCommand("disp(" + actualCommand + ")");
        project.getBuildersList().add(this.scriptBuilder);
        FreeStyleBuild build = project.scheduleBuild2(0).get();

        jenkins.assertLogContains("Generating MATLAB script with content", build);
        jenkins.assertLogContains(expectedCommand, build);
    }

    /*
     * Test to verify error message when command is empty.
     */
    @Test
    public void verifyErrorMessageOnEmptyCommand() throws Exception {
        this.buildWrapper.setMatlabBuildWrapperContent(
                new MatlabBuildWrapperContent(Message.getValue("matlab.custom.location"), getMatlabroot("R2017a")));
        project.getBuildWrappersList().add(this.buildWrapper);
        project.getBuildersList().add(this.scriptBuilder);
        HtmlPage page = jenkins.createWebClient().goTo("job/test0/configure");

        WebAssert.assertTextPresent(page, "Specify at least one script, function, or statement to execute.");
    }

    /*
     * Test to verify no error message when command is provided.
     */

    @Test
    public void verifyWhenCommandNonEmpty() throws Exception {
        this.buildWrapper.setMatlabBuildWrapperContent(
                new MatlabBuildWrapperContent(Message.getValue("matlab.custom.location"), getMatlabroot("R2017a")));
        project.getBuildWrappersList().add(this.buildWrapper);
        this.scriptBuilder.setMatlabCommand("NONEMPTY");
        project.getBuildersList().add(this.scriptBuilder);
        HtmlPage page = jenkins.createWebClient().goTo("job/test0/configure");

        WebAssert.assertTextNotPresent(page, "Specify at least one script, function, or statement to execute.");
    }

}
