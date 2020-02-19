package com.mathworks.ci;

/*
 * Copyright 2020-2021 The MathWorks, Inc.
 * 
 * Test class for MatlabScriptBuilderTest
 * 
 */

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;
import java.util.Optional;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import hudson.EnvVars;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import hudson.slaves.EnvironmentVariablesNodeProperty;
import hudson.tasks.Builder;

public class MatlabScriptBuilderTest {

    private static String matlabExecutorAbsolutePath;
    private FreeStyleProject project;
    private MatlabBuildWrapper buildWrapper;
    private MatlabScriptBuilder scriptBuilder;
    private static URL url;
    private static String FileSeperator;
    private static String VERSION_INFO_XML_FILE = "VersionInfo.xml";

    @Rule
    public JenkinsRule jenkins = new JenkinsRule();

    @BeforeClass
    public static void classSetup() throws URISyntaxException, IOException {
        ClassLoader classLoader = MatlabScriptBuilderTest.class.getClassLoader();
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
        this.scriptBuilder = new MatlabScriptBuilder();
        this.buildWrapper = new MatlabBuildWrapper();
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
        return MatlabTestRunBuilderTest.class.getClassLoader().getResource(resource);
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
     * Test To verify MATLAB is launched with default arguments and with -batch when release
     * supports -batch
     */

    @Test
    public void verifyMATLABlaunchedWithDefaultArgumentsBatch() throws Exception {
        this.buildWrapper.setMatlabRootFolder(getMatlabroot("R2018b"));
        project.getBuildWrappersList().add(this.buildWrapper);
        scriptBuilder.setMatlabCommand("pwd");
        project.getBuildersList().add(this.scriptBuilder);
        FreeStyleBuild build = project.scheduleBuild2(0).get();
        jenkins.assertLogContains("-batch", build);
    }

    /*
     * Test To verify MATLAB is launched with default arguments and with -r when release supports -r
     * 
     */

    @Test
    public void verifyMATLABlaunchedWithDefaultArguments() throws Exception {
        this.buildWrapper.setMatlabRootFolder(getMatlabroot("R2017a"));
        project.getBuildWrappersList().add(this.buildWrapper);
        scriptBuilder.setMatlabCommand("pwd");
        project.getBuildersList().add(this.scriptBuilder);
        FreeStyleBuild build = project.scheduleBuild2(0).get();
        jenkins.assertLogContains("-r", build);
        jenkins.assertLogContains("eval(", build);
    }

    /*
     * Test to verify if job fails when invalid MATLAB path is provided and Exception is thrown
     */

    @Test
    public void verifyBuilderFailsForInvalidMATLABPath() throws Exception {
        this.buildWrapper.setMatlabRootFolder("/fake/matlabroot/that/does/not/exist");
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
        this.buildWrapper.setMatlabRootFolder(getMatlabroot("R2018b"));
        project.getBuildWrappersList().add(this.buildWrapper);
        MatlabScriptBuilderTester tester =
                new MatlabScriptBuilderTester(matlabExecutorAbsolutePath, "-positiveFail");
        tester.setMatlabCommand("pp");
        project.getBuildersList().add(tester);
        FreeStyleBuild build = project.scheduleBuild2(0).get();
        jenkins.assertBuildStatus(Result.FAILURE, build);
    }

    /*
     * Test to verify if Build FAILS when matlab command fails
     */

    @Test
    public void verifyBuildFailureWhenMatlabCommandPasses() throws Exception {
        this.buildWrapper.setMatlabRootFolder(getMatlabroot("R2018b"));
        project.getBuildWrappersList().add(this.buildWrapper);
        MatlabScriptBuilderTester tester =
                new MatlabScriptBuilderTester(matlabExecutorAbsolutePath, "-positive");
        tester.setMatlabCommand("pwd");
        project.getBuildersList().add(tester);
        FreeStyleBuild build = project.scheduleBuild2(0).get();
        jenkins.assertBuildStatus(Result.SUCCESS, build);
    }

    /*
     * Test to verify Builder picks the exact command that user entered in batch mode
     */

    @Test
    public void verifyBuildPicksTheCorretCommandBatch() throws Exception {
        this.buildWrapper.setMatlabRootFolder(getMatlabroot("R2018b"));
        project.getBuildWrappersList().add(this.buildWrapper);
        scriptBuilder.setMatlabCommand("pwd");
        project.getBuildersList().add(this.scriptBuilder);
        FreeStyleBuild build = project.scheduleBuild2(0).get();
        jenkins.assertLogContains("-batch", build);
        jenkins.assertLogContains("pwd", build);
    }

    /*
     * Test to verify Builder picks the exact command that user entered in -r mode
     */

    @Test
    public void verifyBuildPicksTheCorretCommandR() throws Exception {
        this.buildWrapper.setMatlabRootFolder(getMatlabroot("R2017a"));
        project.getBuildWrappersList().add(this.buildWrapper);
        scriptBuilder.setMatlabCommand("pwd");
        project.getBuildersList().add(this.scriptBuilder);
        FreeStyleBuild build = project.scheduleBuild2(0).get();
        jenkins.assertLogContains("-r", build);
        jenkins.assertLogContains("eval('pwd')", build);
    }

    /*
     * Test to verify if MATALB scratch file is not generated in workspace for this builder.
     */
    @Test
    public void verifyMATLABscratchFileNotGenerated() throws Exception {
        this.buildWrapper.setMatlabRootFolder(getMatlabroot("R2018b"));
        project.getBuildWrappersList().add(this.buildWrapper);
        scriptBuilder.setMatlabCommand("pwd");
        project.getBuildersList().add(this.scriptBuilder);
        FreeStyleBuild build = project.scheduleBuild2(0).get();
        File matlabRunner = new File(build.getWorkspace() + File.separator + "runMatlabTests.m");
        Assert.assertFalse(matlabRunner.exists());
    }
    
    /*
     * Test to verify command supports resolving environment variable (For MATRIX builds).
     */
    @Test
    public void verifyCommandSupportsEnvVar() throws Exception {
        EnvironmentVariablesNodeProperty prop = new EnvironmentVariablesNodeProperty();
        EnvVars var = prop.getEnvVars();
        var.put("PWDCMD", "pwd");
        jenkins.jenkins.getGlobalNodeProperties().add(prop);
        this.buildWrapper.setMatlabRootFolder(getMatlabroot("R2018b"));
        project.getBuildWrappersList().add(this.buildWrapper);
        scriptBuilder.setMatlabCommand("$PWDCMD");
        project.getBuildersList().add(scriptBuilder);
        FreeStyleBuild build = project.scheduleBuild2(0).get();
        jenkins.assertLogContains("pwd", build);
    }
}
