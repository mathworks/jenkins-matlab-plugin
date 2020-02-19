package com.mathworks.ci;
/*
 * Copyright 2020-2021 The MathWorks, Inc.
 * 
 * Test class for MatlabBuildWrapper
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
import com.gargoylesoftware.htmlunit.WebAssert;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import hudson.model.FreeStyleProject;
import hudson.tasks.BuildWrapper;


public class MatlabBuildWrapperTest {
    
    private FreeStyleProject project;
    private MatlabBuildWrapper buildWrapper;
    private static String FileSeperator;
    private static String VERSION_INFO_XML_FILE = "VersionInfo.xml";
    
    @BeforeClass
    public static void classSetup() {
        if (!System.getProperty("os.name").startsWith("Win")) {
            FileSeperator = "/";
        }else {
            FileSeperator = "\\";
        }
    }
         

    
    @Rule
    public JenkinsRule jenkins = new JenkinsRule();
    
    @Before
    public void testSetup() throws IOException {
        this.project = jenkins.createFreeStyleProject();
        this.buildWrapper = new MatlabBuildWrapper();
    }

    @After
    public void testTearDown() {
        this.project = null;
    }
    
    //Private Method to get the valid MATLAB roots
    private String getMatlabroot(String version) throws URISyntaxException {
        String defaultVersionInfo = "versioninfo/R2017a/" + VERSION_INFO_XML_FILE;
        String userVersionInfo = "versioninfo/"+version+"/" + VERSION_INFO_XML_FILE;
        URL matlabRootURL = Optional.ofNullable(getResource(userVersionInfo)).orElseGet(() -> getResource(defaultVersionInfo));
        File matlabRoot = new File(matlabRootURL.toURI());
        return matlabRoot.getAbsolutePath().replace(FileSeperator + VERSION_INFO_XML_FILE,"").replace("R2017a",version);
    }
    
    private URL getResource(String resource) {
        return MatlabBuildWrapperTest.class.getClassLoader().getResource(resource); 
    }
    
    /*
     * Test Case to verify if job contains MATLAB build environment section.
     */
    @Test
    public void verifyBuildEnvForMatlab() throws Exception {
        boolean found = false;
        this.buildWrapper.setMatlabRootFolder("");
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
     */
    @Test
    public void verifyPATHupdated() throws Exception {
        this.buildWrapper.setMatlabRootFolder("/test/MATLAB/R2019a");
        project.getBuildWrappersList().add(this.buildWrapper);
        MatlabTestRunBuilderTester buildTester = new MatlabTestRunBuilderTester("","");
        buildTester.setCoberturaChkBx(false);
        buildTester.setJunitChkBx(false);
        buildTester.setModelCoverageChkBx(false);
        buildTester.setPdfReportChkBx(false);
        buildTester.setTapChkBx(false);
        buildTester.setStmResultsChkBx(false);   
        project.getBuildersList().add(buildTester);
        project.scheduleBuild2(0).get();
        Assert.assertTrue("Build does not have MATLAB build environment", this.buildWrapper.getMatlabRootFolder().equalsIgnoreCase(buildTester.getMatlabRoot()));
    }
    
    /*
     * Test To verify if UI  throws an error when MATLAB root is empty.
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
        this.buildWrapper.setMatlabRootFolder("/fake/MATLAB/path");
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
        this.buildWrapper.setMatlabRootFolder("/test/MATLAB/$VERSION");
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
        this.buildWrapper.setMatlabRootFolder(getMatlabroot("R2018b"));
        HtmlPage page = jenkins.createWebClient().goTo("job/test0/configure");
        WebAssert.assertTextNotPresent(page, TestMessage.getValue("Builder.invalid.matlab.root.warning"));
    }
    
    

}
