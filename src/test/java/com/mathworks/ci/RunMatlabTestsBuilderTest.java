package com.mathworks.ci;
/**
 * Copyright 2019-2020 The MathWorks, Inc.
 * 
 * Test class for RunMatlabTestsBuilder
 * 
 */

import static org.junit.Assert.assertFalse;
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
import com.gargoylesoftware.htmlunit.html.HtmlCheckBoxInput;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import hudson.FilePath;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import hudson.tasks.Builder;

public class RunMatlabTestsBuilderTest {


    private static String matlabExecutorAbsolutePath;
    private FreeStyleProject project;
    private AddMatlabToPathBuildWrapper buildWrapper;
    private RunMatlabTestsBuilder testBuilder;
    private static URL url;
    private static String FileSeperator;
    private static String VERSION_INFO_XML_FILE = "VersionInfo.xml";

    @Rule
    public JenkinsRule jenkins = new JenkinsRule();

    @BeforeClass
    public static void classSetup() throws URISyntaxException, IOException {
        ClassLoader classLoader = RunMatlabTestsBuilderTest.class.getClassLoader();
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
        this.testBuilder = new RunMatlabTestsBuilder();
        this.buildWrapper = new AddMatlabToPathBuildWrapper();
    }

    @After
    public void testTearDown() {
        this.project = null;
        this.testBuilder = null;
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
     * Test Case to verify if Build step contains "Run MATLAB Tests" option.
     */
    @Test
    public void verifyBuildStepWithMatlabTestBuilder() throws Exception {
        boolean found = false;
        setAllTestArtifacts(false, testBuilder);
        project.getBuildersList().add(testBuilder);
        List<Builder> bl = project.getBuildersList();
        for (Builder b : bl) {
            if (b.getDescriptor().getDisplayName()
                    .equalsIgnoreCase(Message.getBuilderDisplayName())) {
                found = true;
            }
        }
        Assert.assertTrue("Build step does not contain Run MATLAB Tests option", found);
    }

    /*
     * Test To verify MATLAB is launched using run matlab script for version above R2018b
     * 
     */

    @Test
    public void verifyMATLABlaunchedWithDefaultArgumentsBatch() throws Exception {
        this.buildWrapper.setMatlabRootFolder(getMatlabroot("R2018b"));
        project.getBuildWrappersList().add(this.buildWrapper);
        setAllTestArtifacts(false, this.testBuilder);
        project.getBuildersList().add(this.testBuilder);
        FreeStyleBuild build = project.scheduleBuild2(0).get();
        jenkins.assertLogContains("run_matlab_command", build);
        jenkins.assertLogContains("exit(runMatlabTests", build);
    }

    /*
     * Test To verify MATLAB is launched using run matlab script for version below R2018b
     * on windows
     */

    @Test
    public void verifyMATLABlaunchedWithDefaultArgumentsRWindows() throws Exception {
        this.buildWrapper.setMatlabRootFolder(getMatlabroot("R2017a"));
        project.getBuildWrappersList().add(this.buildWrapper);
        setAllTestArtifacts(false, this.testBuilder);
        project.getBuildersList().add(testBuilder);
        FreeStyleBuild build = project.scheduleBuild2(0).get();
        jenkins.assertLogContains("run_matlab_command", build);
        jenkins.assertLogContains("exit(runMatlabTests", build);
    }

    /*
     * Test to verify if job fails when invalid MATLAB path is provided and Exception is thrown
     */

    @Test
    public void verifyBuilderFailsForInvalidMATLABPath() throws Exception {
        this.buildWrapper.setMatlabRootFolder("/fake/matlabroot/that/does/not/exist");
        project.getBuildWrappersList().add(this.buildWrapper);
        setAllTestArtifacts(false, this.testBuilder);
        project.getBuildersList().add(this.testBuilder);

        FreeStyleBuild build = project.scheduleBuild2(0).get();
        jenkins.assertBuildStatus(Result.FAILURE, build);
    }

    /*
     * Test to verify if Build FAILS when matlab test fails
     */

    @Test
    public void verifyBuildFailureWhenMatlabException() throws Exception {
        this.buildWrapper.setMatlabRootFolder(getMatlabroot("R2018b"));
        project.getBuildWrappersList().add(this.buildWrapper);
        RunMatlabTestsBuilderTester tester =
                new RunMatlabTestsBuilderTester(matlabExecutorAbsolutePath, "-positiveFail");
        setAllTestArtifacts(false, tester);
        project.getBuildersList().add(tester);
        FreeStyleBuild build = project.scheduleBuild2(0).get();
        jenkins.assertBuildStatus(Result.FAILURE, build);
    }

    /*
     * Test to verify if Build PASSES when matlab test PASSES
     */

    @Test
    public void verifyBuildPassWhenTestPass() throws Exception {
        this.buildWrapper.setMatlabRootFolder(getMatlabroot("R2018b"));
        project.getBuildWrappersList().add(this.buildWrapper);
        RunMatlabTestsBuilderTester tester =
                new RunMatlabTestsBuilderTester(matlabExecutorAbsolutePath, "-positive");
        setAllTestArtifacts(false, tester);
        project.getBuildersList().add(tester);
        FreeStyleBuild build = project.scheduleBuild2(0).get();
        jenkins.assertBuildStatus(Result.SUCCESS, build);
    }

    /*
     * Tests to verify if verLessThan() method compares values appropriately.
     */

    @Test
    public void verifyVerlessThan() throws Exception {
        FilePath matlabRoot = new FilePath(new File(getMatlabroot("R2017a")));
        MatlabReleaseInfo rel = new MatlabReleaseInfo(matlabRoot);

        // verLessthan() will check all the versions against 9.2 which is version of R2017a
        assertFalse(rel.verLessThan(9.1));
        assertFalse(rel.verLessThan(9.0));
        assertFalse(rel.verLessThan(9.2));
        Assert.assertTrue(rel.verLessThan(9.9));
        Assert.assertTrue(rel.verLessThan(10.1));
    }

    /*
     * Test to verify if Automatic option passes appropriate test atrtifact values.
     */

    @Test
    public void verifyRunTestAutomaticallyIsDefault() throws Exception {
        this.buildWrapper.setMatlabRootFolder(getMatlabroot("R2018b"));
        project.getBuildWrappersList().add(this.buildWrapper);
        setAllTestArtifacts(true, testBuilder);
        project.getBuildersList().add(this.testBuilder);
        FreeStyleBuild build = project.scheduleBuild2(0).get();
        jenkins.assertLogContains("run_matlab_command", build);
        jenkins.assertLogContains("\'PDFReport\',true,\'TAPResults\',true,"
                + "\'JUnitResults\',true,\'SimulinkTestResults\',true,"
                + "\'CoberturaCodeCoverage\',true,\'CoberturaModelCoverage\',true", build);
    }

    /*
     * Test to verify if MATALB scratch file is generated in workspace.
     */
    @Test
    public void verifyMATLABscratchFileGeneratedForAutomaticOption() throws Exception {
        this.buildWrapper.setMatlabRootFolder(getMatlabroot("R2018b"));
        project.getBuildWrappersList().add(this.buildWrapper);
        setAllTestArtifacts(false, testBuilder);
        project.getBuildersList().add(testBuilder);
        FreeStyleBuild build = project.scheduleBuild2(0).get();
        File matlabRunner = new File(build.getWorkspace() + File.separator + "runMatlabTests.m");
        Assert.assertTrue(matlabRunner.exists());
    }
    
    /*
     * Test to verify if appropriate MATALB runner file is copied in workspace.
     */
    @Test
    public void verifyMATLABrunnerFileGeneratedForAutomaticOption() throws Exception {
        this.buildWrapper.setMatlabRootFolder(getMatlabroot("R2018b"));
        project.getBuildWrappersList().add(this.buildWrapper);
        
        project.getBuildersList().add(testBuilder);
        FreeStyleBuild build = project.scheduleBuild2(0).get();
        String runnerFile;
        if (!System.getProperty("os.name").startsWith("Win")) {
            runnerFile = "run_matlab_command.sh";
        }else {
            runnerFile = "run_matlab_command.bat";
        }
        File matlabRunner = new File(build.getWorkspace() + File.separator + runnerFile);
        Assert.assertTrue(matlabRunner.exists());
    }

    /*
     * Test To verify UI displays Cobertura Warning message when unsupported MATLAB version used.
     * 
     */

    @Test
    public void verifyCoberturaWarning() throws Exception {
        this.buildWrapper.setMatlabRootFolder(getMatlabroot("R2017a"));
        project.getBuildWrappersList().add(this.buildWrapper);
        project.getBuildersList().add(this.testBuilder);
        HtmlPage page = jenkins.createWebClient().goTo("job/test0/configure");
        HtmlCheckBoxInput coberturaChkBx = page.getElementByName("coberturaChkBx");
        coberturaChkBx.setChecked(true);
        Thread.sleep(2000);
        WebAssert.assertTextPresent(page,
                TestMessage.getValue("Builder.matlab.cobertura.support.warning"));
    }

    /*
     * Test to verify that UI displays model coverage warning message when unsupported MATLAB
     * version is used.
     *
     */

    @Test
    public void verifyModelCoverageWarning() throws Exception {
        this.buildWrapper.setMatlabRootFolder(getMatlabroot("R2018a"));
        project.getBuildWrappersList().add(this.buildWrapper);
        project.getBuildersList().add(this.testBuilder);
        HtmlPage page = jenkins.createWebClient().goTo("job/test0/configure");
        HtmlCheckBoxInput modelCoverageChkBx = page.getElementByName("modelCoverageChkBx");
        modelCoverageChkBx.setChecked(true);
        Thread.sleep(2000);
        WebAssert.assertTextPresent(page,
                TestMessage.getValue("Builder.matlab.modelcoverage.support.warning"));
    }

    /*
     * Test to verify that UI displays STM results warning message when unsupported MATLAB version
     * is used.
     *
     */

    @Test
    public void verifySTMResultsWarning() throws Exception {
        this.buildWrapper.setMatlabRootFolder(getMatlabroot("R2018b"));
        project.getBuildWrappersList().add(this.buildWrapper);
        project.getBuildersList().add(this.testBuilder);
        HtmlPage page = jenkins.createWebClient().goTo("job/test0/configure");
        HtmlCheckBoxInput stmResultsChkBx = page.getElementByName("stmResultsChkBx");
        stmResultsChkBx.setChecked(true);
        Thread.sleep(2000);
        WebAssert.assertTextPresent(page,
                TestMessage.getValue("Builder.matlab.exportstmresults.support.warning"));
    }

    /*
     * Test To verify UI displays Cobertura warning message when invalid MATLAB root entered.
     * 
     */

    @Test
    public void verifyCoberturaError() throws Exception {
        this.buildWrapper.setMatlabRootFolder(getMatlabroot("/fake/path"));
        project.getBuildWrappersList().add(this.buildWrapper);
        project.getBuildersList().add(this.testBuilder);
        HtmlPage page = jenkins.createWebClient().goTo("job/test0/configure");
        HtmlCheckBoxInput coberturaChkBx = page.getElementByName("coberturaChkBx");
        coberturaChkBx.setChecked(true);
        Thread.sleep(2000);
        String pageText = page.asText();
        String filteredPageText = pageText
                .replaceFirst(TestMessage.getValue("Builder.invalid.matlab.root.warning"), "");
        Assert.assertTrue(filteredPageText
                .contains(TestMessage.getValue("Builder.invalid.matlab.root.warning")));
    }

    /*
     * Test To verify UI displays Model coverage warning message when MATLAB version does not
     * support.
     * 
     */

    @Test
    public void verifyInvalidMatlabWarningForModelCoverage() throws Exception {
        this.buildWrapper.setMatlabRootFolder(getMatlabroot("/fake/path"));
        project.getBuildWrappersList().add(this.buildWrapper);
        project.getBuildersList().add(this.testBuilder);
        HtmlPage page = jenkins.createWebClient().goTo("job/test0/configure");
        HtmlCheckBoxInput modelCoverageChkBx = page.getElementByName("modelCoverageChkBx");
        modelCoverageChkBx.setChecked(true);
        Thread.sleep(2000);
        String pageText = page.asText();
        String filteredPageText = pageText
                .replaceFirst(TestMessage.getValue("Builder.invalid.matlab.root.warning"), "");
        Assert.assertTrue(filteredPageText
                .contains(TestMessage.getValue("Builder.invalid.matlab.root.warning")));
    }

    /*
     * Test To verify UI displays Model test report warning message when MATLAB version does not
     * support.
     * 
     */

    @Test
    public void verifyInvalidMatlabWarningForSTMResults() throws Exception {
        this.buildWrapper.setMatlabRootFolder(getMatlabroot("/fake/path"));
        project.getBuildWrappersList().add(this.buildWrapper);
        project.getBuildersList().add(this.testBuilder);
        HtmlPage page = jenkins.createWebClient().goTo("job/test0/configure");
        HtmlCheckBoxInput stmResultsChkBx = page.getElementByName("stmResultsChkBx");
        stmResultsChkBx.setChecked(true);
        Thread.sleep(2000);
        String pageText = page.asText();
        String filteredPageText = pageText
                .replaceFirst(TestMessage.getValue("Builder.invalid.matlab.root.warning"), "");
        Assert.assertTrue(filteredPageText
                .contains(TestMessage.getValue("Builder.invalid.matlab.root.warning")));
    }

    private void setAllTestArtifacts(boolean val, RunMatlabTestsBuilder testBuilder) {
        testBuilder.setCoberturaChkBx(val);
        testBuilder.setJunitChkBx(val);
        testBuilder.setModelCoverageChkBx(val);
        testBuilder.setPdfReportChkBx(val);
        testBuilder.setTapChkBx(val);
        testBuilder.setStmResultsChkBx(val);
    }

}
