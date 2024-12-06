package com.mathworks.ci;

/**
 * Copyright 2019-2024 The MathWorks, Inc.
 * 
 * Test class for AddMatlabToPathBuildWrapper
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
import org.htmlunit.WebAssert;
import org.htmlunit.html.HtmlPage;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.tasks.BuildWrapper;

public class UseMatlabVersionBuildWrapperTest {

    private FreeStyleProject project;
    private UseMatlabVersionBuildWrapper buildWrapper;
    private static String FileSeperator;
    private static String VERSION_INFO_XML_FILE = "VersionInfo.xml";

    @BeforeClass
    public static void classSetup() {
        if (!System.getProperty("os.name").startsWith("Win")) {
            FileSeperator = "/";
        } else {
            FileSeperator = "\\";
        }
    }

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
    }

    // Private Method to get the valid MATLAB roots
    private String getMatlabroot(String version) throws URISyntaxException {
        String defaultVersionInfo = "versioninfo/R2017a/" + VERSION_INFO_XML_FILE;
        String userVersionInfo = "versioninfo/" + version + "/" + VERSION_INFO_XML_FILE;
        URL matlabRootURL = Optional.ofNullable(getResource(userVersionInfo))
                .orElseGet(() -> getResource(defaultVersionInfo));
        File matlabRoot = new File(matlabRootURL.toURI());
        return matlabRoot.getAbsolutePath().replace(FileSeperator + VERSION_INFO_XML_FILE, "").replace("R2017a",
                version);
    }

    private URL getResource(String resource) {
        return UseMatlabVersionBuildWrapperTest.class.getClassLoader().getResource(resource);
    }

    /*
     * Test Case to verify if job contains MATLAB build environment section.
     */
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

    /*
     * Verify if given MATLAB root is added in the PATH.
     * Should be added to integration test.
     */

    public void verifyPATHupdated() throws Exception {
        this.buildWrapper.setMatlabBuildWrapperContent(new MatlabBuildWrapperContent(
                Message.getValue("matlab.custom.location"), getMatlabroot("/test/MATLAB/R2019a")));
        project.getBuildWrappersList().add(this.buildWrapper);
        RunMatlabTestsBuilderTester buildTester = new RunMatlabTestsBuilderTester("", "");
        project.getBuildersList().add(buildTester);
        FreeStyleBuild build = project.scheduleBuild2(0).get();
        Assert.assertTrue("Build does not have MATLAB build environment",
                this.buildWrapper.getMatlabRootFolder().equalsIgnoreCase(buildTester.getMatlabRoot()));
    }

    /*
     * Verify if invalid MATLAB path throes error on console.
     */
    @Test
    public void verifyInvalidPATHError() throws Exception {
        this.buildWrapper.setMatlabBuildWrapperContent(new MatlabBuildWrapperContent(
                Message.getValue("matlab.custom.location"), getMatlabroot("/test/MATLAB/R2019a")));
        project.getBuildWrappersList().add(this.buildWrapper);
        RunMatlabTestsBuilderTester buildTester = new RunMatlabTestsBuilderTester("", "");
        project.getBuildersList().add(buildTester);
        project.scheduleBuild2(0).get();
        FreeStyleBuild build = project.scheduleBuild2(0).get();
        jenkins.assertLogContains("MatlabNotFoundError", build);
    }

    /*
     * Test To verify if UI throws an error when MATLAB root is empty.
     * 
     */

    @Test
    public void verifyEmptyMatlabRootError() throws Exception {
        project.getBuildWrappersList().add(this.buildWrapper);
        HtmlPage page = jenkins.createWebClient().goTo("job/test0/configure");
        WebAssert.assertTextPresent(page, TestMessage.getValue("Builder.matlab.root.empty.error"));
    }

    /*
     * Test To verify UI does throw error when in-valid MATLAB root entered
     * 
     */

    @Test
    public void verifyInvalidMatlabRootDisplaysWarnning() throws Exception {
        project.getBuildWrappersList().add(this.buildWrapper);
        this.buildWrapper.setMatlabBuildWrapperContent(new MatlabBuildWrapperContent(
                Message.getValue("matlab.custom.location"), getMatlabroot("/fake/MATLAB/path")));
        HtmlPage page = jenkins.createWebClient().goTo("job/test0/configure");
        WebAssert.assertTextPresent(page, TestMessage.getValue("Builder.invalid.matlab.root.warning"));
    }

    /*
     * Test To verify UI does not throw error when matrix variables are use
     * 
     */

    @Test
    public void verifyMatriVariableNoErrorOrWarnning() throws Exception {
        project.getBuildWrappersList().add(this.buildWrapper);
        this.buildWrapper.setMatlabBuildWrapperContent(new MatlabBuildWrapperContent(
                Message.getValue("matlab.custom.location"), getMatlabroot("/test/MATLAB/$VERSION")));
        HtmlPage page = jenkins.createWebClient().goTo("job/test0/configure");
        WebAssert.assertTextNotPresent(page, TestMessage.getValue("Builder.invalid.matlab.root.warning"));
    }

    /*
     * Test To verify UI does not throw warning when valid Matlab root is entered.
     * 
     */

    @Test
    public void verifyValidMatlabNoWarning() throws Exception {
        project.getBuildWrappersList().add(this.buildWrapper);
        this.buildWrapper.setMatlabBuildWrapperContent(
                new MatlabBuildWrapperContent(Message.getValue("matlab.custom.location"), getMatlabroot("R2018b")));
        HtmlPage page = jenkins.createWebClient().goTo("job/test0/configure");
        WebAssert.assertTextNotPresent(page, TestMessage.getValue("Builder.invalid.matlab.root.warning"));
    }

}
