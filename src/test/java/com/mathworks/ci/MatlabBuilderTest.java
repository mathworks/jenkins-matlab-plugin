package com.mathworks.ci;



import static org.junit.Assert.assertFalse;
import hudson.EnvVars;
import hudson.FilePath;
import hudson.model.FreeStyleBuild;
import hudson.model.Result;
import hudson.slaves.EnvironmentVariablesNodeProperty;
import hudson.model.FreeStyleProject;
import hudson.tasks.Builder;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
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
import com.mathworks.ci.MatlabBuilder.RunTestsAutomaticallyOption;
import com.mathworks.ci.MatlabBuilder.RunTestsWithCustomCommandOption;


/*
 * Copyright 2018-2020 The MathWorks, Inc.
 * 
 * Test class for MatlabBuilder
 * 
 * Author : Nikhil Bhoski email : nikhil.bhoski@mathworks.in Date : 28/03/2018 (Initial draft)
 */
public class MatlabBuilderTest {

    private static TestMessage messages;
    private static String matlabExecutorAbsolutePath;
    private FreeStyleProject project;
    private MatlabBuilder matlabBuilder;
    private static URL url;
    private static String FileSeperator;
    private static String VERSION_INFO_XML_FILE = "VersionInfo.xml";

    @Rule
    public JenkinsRule jenkins = new JenkinsRule();

    @BeforeClass
    public static void classSetup() throws URISyntaxException, IOException {
        ClassLoader classLoader = MatlabBuilderTest.class.getClassLoader();
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
        messages = new TestMessage();
    }

    @Before
    public void testSetup() throws IOException {

        this.project = jenkins.createFreeStyleProject();
        this.matlabBuilder = new MatlabBuilder();
    }

    @After
    public void testTearDown() {
        this.project = null;
        this.matlabBuilder = null;
    }

    private String getMatlabroot(String version) throws URISyntaxException {
        String defaultVersionInfo = "versioninfo/R2017a/" + VERSION_INFO_XML_FILE;
        String userVersionInfo = "versioninfo/"+version+"/" + VERSION_INFO_XML_FILE;
        URL matlabRootURL = Optional.ofNullable(getResource(userVersionInfo)).orElseGet(() -> getResource(defaultVersionInfo));
        File matlabRoot = new File(matlabRootURL.toURI());
        return matlabRoot.getAbsolutePath().replace(FileSeperator + VERSION_INFO_XML_FILE,"").replace("R2017a",version);
    }
    

    /*
     * Test Case to verify if Build step contains "Run MATLAB Tests" option.
     */
    @Test
    public void verifyBuildStepWithMATLABBuilder() throws Exception {
        boolean found = false;
        this.matlabBuilder.setMatlabRoot("");
        project.getBuildersList().add(this.matlabBuilder);
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
     * Test To verify MATLAB is launched with default arguments and with -batch when release
     * supports -batch
     */

    @Test
    public void verifyMATLABlaunchedWithDefaultArgumentsBatch() throws Exception {
        this.matlabBuilder.setMatlabRoot(getMatlabroot("R2018b"));
        this.matlabBuilder.setTestRunTypeList(new RunTestsAutomaticallyOption());
        project.getBuildersList().add(this.matlabBuilder);
        FreeStyleBuild build = project.scheduleBuild2(0).get();
        jenkins.assertLogContains("-batch", build);
        jenkins.assertLogContains("exit(runMatlabTests", build);
        Assert.assertEquals(3, matlabBuilder.constructMatlabCommandWithBatch().size());
    }

    /*
     * Test To verify MATLAB is launched with default arguments and with -r when release supports -r
     * on windows
     */

    @Test
    public void verifyMATLABlaunchedWithDefaultArgumentsRWindows() throws Exception {
        this.matlabBuilder.setMatlabRoot(getMatlabroot("R2017a"));
        this.matlabBuilder.setTestRunTypeList(new RunTestsAutomaticallyOption());
        project.getBuildersList().add(this.matlabBuilder);
        FreeStyleBuild build = project.scheduleBuild2(0).get();
        jenkins.assertLogContains("-r", build);
        jenkins.assertLogContains("try,exit(runMatlabTests", build);
        Assert.assertEquals(9, matlabBuilder.constructDefaultMatlabCommand(false).size());
    }

    /*
     * Test To verify MATLAB is launched with default arguments and with -r when release supports -r
     * on Linux
     */

    @Test
    public void verifyMATLABlaunchedWithDefaultArgumentsRLinux() throws Exception {
        this.matlabBuilder.setMatlabRoot(getMatlabroot("R2017a"));
        this.matlabBuilder.setTestRunTypeList(new RunTestsAutomaticallyOption());
        project.getBuildersList().add(this.matlabBuilder);
        FreeStyleBuild build = project.scheduleBuild2(0).get();
        jenkins.assertLogContains("-r", build);
        jenkins.assertLogContains("try,exit(runMatlabTests", build);
        Assert.assertEquals(7, matlabBuilder.constructDefaultMatlabCommand(true).size());
    }

    /*
     * Test to verify if job fails when invalid MATLAB path is provided and Exception is thrown
     */

    @Test
    public void verifyBuilderFailsForInvalidMATLABPath() throws Exception {
        this.matlabBuilder.setMatlabRoot("/fake/matlabroot/that/does/not/exist");
        project.getBuildersList().add(this.matlabBuilder);

        FreeStyleBuild build = project.scheduleBuild2(0).get();
        jenkins.assertBuildStatus(Result.FAILURE, build);
    }

    /*
     * Test to verify if Build FAILS when matlab test fails
     */

    @Test
    public void verifyBuildFailureWhenMatlabException() throws Exception {
        MatlabBuilderTester tester = new MatlabBuilderTester(getMatlabroot("R2018b"),
                matlabExecutorAbsolutePath, "-positiveFail");
        tester.setTestRunTypeList(new RunTestsAutomaticallyOption());
        project.getBuildersList().add(tester);
        FreeStyleBuild build = project.scheduleBuild2(0).get();
        jenkins.assertBuildStatus(Result.FAILURE, build);
    }

    /*
     * Test to verify if MATLAB gets invoked and job sets to UNSTABLE when valid matlabroot is
     * provided and all test passed.
     */
    @Test
    public void verifyMatlabInvokedWithValidExecutable() throws Exception {
        MatlabBuilderTester tester = new MatlabBuilderTester(getMatlabroot("R2018b"),
                matlabExecutorAbsolutePath, "-positive");
        tester.setTestRunTypeList(new RunTestsAutomaticallyOption());
        project.getBuildersList().add(tester);
        FreeStyleBuild build = project.scheduleBuild2(0).get();
        jenkins.assertBuildStatus(Result.SUCCESS, build);
        jenkins.assertLogContains(messages.getMatlabInvokesPositive(), build);

    }

    /*
     * Test to verify MATLAB executable path is same as provide by user
     */

    @Test
    public void verifyMatlabPointsToValidExecutable() throws Exception {
        MatlabBuilderTester tester = new MatlabBuilderTester(getMatlabroot("R2018b"),
                matlabExecutorAbsolutePath, "-positive");
        project.getBuildersList().add(tester);
        tester.setTestRunTypeList(new RunTestsAutomaticallyOption());
        FreeStyleBuild build = project.scheduleBuild2(0).get();
        jenkins.assertBuildStatus(Result.SUCCESS, build);
        jenkins.assertLogContains(matlabExecutorAbsolutePath, build);
    }

    /*
     * Test to verify Build is set to FAILED when test fails
     * 
     */

    @Test
    public void verifyBuildStatusWhenTestFails() throws Exception {
        MatlabBuilderTester tester = new MatlabBuilderTester(getMatlabroot("R2018b"),
                matlabExecutorAbsolutePath, "failTests");
        project.getBuildersList().add(tester);

        FreeStyleBuild build = project.scheduleBuild2(0).get();
        jenkins.assertBuildStatus(Result.FAILURE, build);
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
     * Test to verify if plugin invokes MATLAB with custom MATLAB command when Custom MATLAB command
     * option is selected. for -r option
     */

    @Test
    public void verifyCustomCommandInvoked() throws Exception {
        this.matlabBuilder.setMatlabRoot(getMatlabroot("R2017a"));
        RunTestsWithCustomCommandOption runOption = new RunTestsWithCustomCommandOption();
        runOption.setCustomMatlabCommand("runtests");
        this.matlabBuilder.setTestRunTypeList(runOption);
        project.getBuildersList().add(this.matlabBuilder);
        FreeStyleBuild build = project.scheduleBuild2(0).get();
        jenkins.assertLogContains("-r", build);
        jenkins.assertLogContains("try,eval", build);
    }

    /*
     * Test to verify if plugin invokes MATLAB with custom MATLAB command when Custom MATLAB command
     * option is selected. for -batch option
     */

    @Test
    public void verifyCustomCommandInvokedForBatchMode() throws Exception {
        this.matlabBuilder.setMatlabRoot(getMatlabroot("R2018b"));
        RunTestsWithCustomCommandOption runOption = new RunTestsWithCustomCommandOption();
        runOption.setCustomMatlabCommand("runtests");
        this.matlabBuilder.setTestRunTypeList(runOption);
        project.getBuildersList().add(this.matlabBuilder);
        FreeStyleBuild build = project.scheduleBuild2(0).get();
        jenkins.assertLogContains("-batch", build);
        jenkins.assertLogContains("runtests", build);
    }
    
    /*
     * Test to verify if Automatic option passes appropriate test atrtifact values.
     */

    @Test
    public void verifyRunTestAutomaticallyIsDefault() throws Exception {
        this.matlabBuilder.setMatlabRoot(getMatlabroot("R2018b"));
        FreeStyleBuild build = getBuildforRunTestAutomatically();
        jenkins.assertLogContains("-batch", build);
        jenkins.assertLogContains("\'PDFReport\',true,\'TAPResults\',true," +
                                  "\'JUnitResults\',true,\'SimulinkTestResults\',true," +
                                  "\'CoberturaCodeCoverage\',true,\'CoberturaModelCoverage\',true", build);
    }
    
    /*
     * Test to verify if MATALB scratch file is generated in workspace for Automatic option.
     */
    @Test
    public void verifyMATLABscratchFileGeneratedForAutomaticOption() throws Exception {
        this.matlabBuilder.setMatlabRoot(getMatlabroot("R2018b"));
        this.matlabBuilder.setTestRunTypeList(new RunTestsAutomaticallyOption());
        project.getBuildersList().add(this.matlabBuilder);
        FreeStyleBuild build = project.scheduleBuild2(0).get();
        File matlabRunner = new File(build.getWorkspace() + File.separator + "runMatlabTests.m");
        Assert.assertTrue(matlabRunner.exists());
    }
    
    /*
     * Test to verify if MATALB scratch file is not generated in workspace for Custom option.
     */
    @Test
    public void verifyMATLABscratchFileGeneratedForCustomOption() throws Exception {
        this.matlabBuilder.setMatlabRoot(getMatlabroot("R2018b"));
        this.matlabBuilder.setTestRunTypeList(new RunTestsWithCustomCommandOption());
        project.getBuildersList().add(this.matlabBuilder);
        FreeStyleBuild build = project.scheduleBuild2(0).get();
        File matlabRunner = new File(build.getWorkspace() + File.separator + "runMatlabTests.m");
        Assert.assertFalse(matlabRunner.exists());
    }
    
    /*
     * Test to verify default value of getStringByName() when Automatic test mode. 
     */

    @Test
    public void verifyDefaultValueOfgetStringByName() throws Exception {
        this.matlabBuilder.setMatlabRoot(getMatlabroot("R2018b"));
        RunTestsAutomaticallyOption runOption = new RunTestsAutomaticallyOption();    
        Assert.assertNull(runOption.getStringByName("fakeChkBox"));
    }
    
    /*
     * Test to verify default value of getBooleanByName() when Custom test mode. 
     */

    @Test
    public void verifyDefaultValueOfgetBooleanByName() throws Exception {
        this.matlabBuilder.setMatlabRoot(getMatlabroot("R2018b"));
        RunTestsWithCustomCommandOption runOption = new RunTestsWithCustomCommandOption();    
        Assert.assertFalse(runOption.getBooleanByName("fakeCommand"));
    }
    
    /*
     * Test to verify when MATLAB version is older the R2017a
     */

    @Test
    public void verifyMatlabVersionOlderThanR17a() throws Exception {
        this.matlabBuilder.setMatlabRoot(getMatlabroot("R2016b"));
        FreeStyleBuild build = getBuildforRunTestAutomatically();
        jenkins.assertLogContains("-r", build);
        jenkins.assertLogContains("try,exit(", build);
    }
    
    /*
     * Test To verify if UI  throws an error when MATLAB root is empty.
     * 
     */

    @Test
    public void verifyEmptyMatlabRootError() throws Exception {
        project.getBuildersList().add(this.matlabBuilder);
        HtmlPage page = jenkins.createWebClient().goTo("job/test0/configure");
        WebAssert.assertTextPresent(page, TestMessage.getValue("Builder.matlab.root.empty.error"));
    }
    
    /*
     * Test To verify UI does throw error when in valid MATLAB root entered
     * 
     */

    @Test
    public void verifyInvalidMatlabRootDisplaysError() throws Exception {
        project.getBuildersList().add(this.matlabBuilder);
        this.matlabBuilder.setMatlabRoot("/fake/matlab/path");
        HtmlPage page = jenkins.createWebClient().goTo("job/test0/configure");
        WebAssert.assertTextPresent(page, TestMessage.getValue("Builder.invalid.matlab.root.warning"));
    }
    
    /*
     * Test To verify UI does not throw any error when valid MATLAB root entered
     * 
     */

    @Test
    public void verifyValidMatlabRootDoesntDisplayError() throws Exception {
        project.getBuildersList().add(this.matlabBuilder);
        this.matlabBuilder.setMatlabRoot(getMatlabroot("R2018b"));
        HtmlPage page = jenkins.createWebClient().goTo("job/test0/configure");
        WebAssert.assertTextNotPresent(page, TestMessage.getValue("Builder.invalid.matlab.root.warning"));
    }
    
    /*
     * Test To verify UI displays Cobertura Warning message when unsupported MATLAB version used.
     * 
     */

    @Test
    public void verifyCoberturaWarning() throws Exception {
        project.getBuildersList().add(this.matlabBuilder);
        this.matlabBuilder.setMatlabRoot(getMatlabroot("R2017a"));
        HtmlPage page = jenkins.createWebClient().goTo("job/test0/configure");
        HtmlCheckBoxInput coberturaChkBx = page.getElementByName("taCoberturaChkBx");
        coberturaChkBx.setChecked(true);
        Thread.sleep(2000);
        WebAssert.assertTextPresent(page, TestMessage.getValue("Builder.matlab.cobertura.support.warning"));
    }
    
    /*
     * Test to verify that UI displays model coverage warning message when unsupported MATLAB version is used.
     *
     */

    @Test
    public void verifyModelCoverageWarning() throws Exception {
        project.getBuildersList().add(this.matlabBuilder);
        this.matlabBuilder.setMatlabRoot(getMatlabroot("R2018a"));
        HtmlPage page = jenkins.createWebClient().goTo("job/test0/configure");
        HtmlCheckBoxInput modelCoverageChkBx = page.getElementByName("taModelCoverageChkBx");
        modelCoverageChkBx.setChecked(true);
        Thread.sleep(2000);
        WebAssert.assertTextPresent(page, TestMessage.getValue("Builder.matlab.modelcoverage.support.warning"));
    }
    
    /*
     * Test to verify that UI displays STM results warning message when unsupported MATLAB version is used.
     *
     */

    @Test
    public void verifySTMResultsWarning() throws Exception {
        project.getBuildersList().add(this.matlabBuilder);
        this.matlabBuilder.setMatlabRoot(getMatlabroot("R2018b"));
        HtmlPage page = jenkins.createWebClient().goTo("job/test0/configure");
        HtmlCheckBoxInput stmResultsChkBx = page.getElementByName("taSTMResultsChkBx");
        stmResultsChkBx.setChecked(true);
        Thread.sleep(2000);
        WebAssert.assertTextPresent(page, TestMessage.getValue("Builder.matlab.exportstmresults.support.warning"));
    }
    
    /*
     * Test To verify UI displays Cobertura warning message when invalid MATLAB root entered.
     * 
     */

    @Test
    public void verifyCoberturaError() throws Exception {
        project.getBuildersList().add(this.matlabBuilder);
        this.matlabBuilder.setMatlabRoot("/fake/matlab/path");
        HtmlPage page = jenkins.createWebClient().goTo("job/test0/configure");
        HtmlCheckBoxInput coberturaChkBx = page.getElementByName("taCoberturaChkBx");
        coberturaChkBx.setChecked(true);
        Thread.sleep(2000);
        String pageText = page.asText();
        String filteredPageText = pageText.replaceFirst(TestMessage.getValue("Builder.invalid.matlab.root.warning"), "");
        Assert.assertTrue(filteredPageText.contains(TestMessage.getValue("Builder.invalid.matlab.root.warning")));
    }
    
    @Test
    public void verifyInvalidMatlabWarningForModelCoverage() throws Exception {
        project.getBuildersList().add(this.matlabBuilder);
        this.matlabBuilder.setMatlabRoot("/fake/matlab/path");
        HtmlPage page = jenkins.createWebClient().goTo("job/test0/configure");
        HtmlCheckBoxInput modelCoverageChkBx = page.getElementByName("taModelCoverageChkBx");
        modelCoverageChkBx.setChecked(true);
        Thread.sleep(2000);
        String pageText = page.asText();
        String filteredPageText = pageText.replaceFirst(TestMessage.getValue("Builder.invalid.matlab.root.warning"), "");
        Assert.assertTrue(filteredPageText.contains(TestMessage.getValue("Builder.invalid.matlab.root.warning")));
    }
    
    @Test
    public void verifyInvalidMatlabWarningForSTMResults() throws Exception {
        project.getBuildersList().add(this.matlabBuilder);
        this.matlabBuilder.setMatlabRoot("/fake/matlab/path");
        HtmlPage page = jenkins.createWebClient().goTo("job/test0/configure");
        HtmlCheckBoxInput stmResultsChkBx = page.getElementByName("taSTMResultsChkBx");
        stmResultsChkBx.setChecked(true);
        Thread.sleep(2000);
        String pageText = page.asText();
        String filteredPageText = pageText.replaceFirst(TestMessage.getValue("Builder.invalid.matlab.root.warning"), "");
        Assert.assertTrue(filteredPageText.contains(TestMessage.getValue("Builder.invalid.matlab.root.warning")));
    }
    
    /*
     * Test to verify if MatlabRoot field supports Jenkins environment variables.
     * 
     */
    
    @Test
    public void verifyEnvVarSupportForMatlabRoot() throws Exception {
        EnvironmentVariablesNodeProperty prop = new EnvironmentVariablesNodeProperty();
        EnvVars var = prop.getEnvVars();
        var.put("MATLAB", "R2017a");
        jenkins.jenkins.getGlobalNodeProperties().add(prop);
        String mroot = getMatlabroot("R2017a");
        this.matlabBuilder.setMatlabRoot(mroot.replace("R2017a", "$MATLAB"));
        this.matlabBuilder.setTestRunTypeList(new RunTestsAutomaticallyOption());
        project.getBuildersList().add(this.matlabBuilder);
        FreeStyleBuild build = project.scheduleBuild2(0).get();
        jenkins.assertLogContains(mroot, build);
        
    }
    
    /*
     * Test to verify if Custom command field supports Jenkins environment variables.
     * 
     */
    
    @Test
    public void verifyEnvVarSupportForCustomCommandField() throws Exception {
        EnvironmentVariablesNodeProperty prop = new EnvironmentVariablesNodeProperty();
        EnvVars var = prop.getEnvVars();
        var.put("TAG", "R2017a");
        jenkins.jenkins.getGlobalNodeProperties().add(prop);
        String mroot = getMatlabroot("R2017a");
        this.matlabBuilder.setMatlabRoot(mroot);
        RunTestsWithCustomCommandOption runOption = new RunTestsWithCustomCommandOption();
        this.matlabBuilder.setTestRunTypeList(runOption);
        runOption.setCustomMatlabCommand("disp($TAG)");
        project.getBuildersList().add(this.matlabBuilder);
        FreeStyleBuild build = project.scheduleBuild2(0).get();
        jenkins.assertLogContains("R2017a", build);
        
    }
    
    /*
     * Test to verify -noAppIcon is not displayed for MATLAB version R2015a
     */

    @Test
    public void verifyNoAppIconForR2015a() throws Exception {
        this.matlabBuilder.setMatlabRoot(getMatlabroot("R2015a"));
        FreeStyleBuild build = getBuildforRunTestAutomatically();
        jenkins.assertLogContains("-r", build);
        jenkins.assertLogNotContains("-noAppIcon", build);
    }
    
    /*
     * Test to verify -noAppIcon is displayed for MATLAB version R2015b
     */

    @Test
    public void verifyNoAppIconForR2015b() throws Exception {
        this.matlabBuilder.setMatlabRoot(getMatlabroot("R2015b"));
        FreeStyleBuild build = getBuildforRunTestAutomatically();
        jenkins.assertLogContains("-r", build);
        jenkins.assertLogContains("-noAppIcon", build);
    }
    
    /*
     * Private helper methods for tests
     */
    
    private FreeStyleBuild getBuildforRunTestAutomatically() throws InterruptedException, ExecutionException {
        RunTestsAutomaticallyOption runOption = new RunTestsAutomaticallyOption();
        runOption.setTaCoberturaChkBx(true);
        runOption.setTaJunitChkBx(true);
        runOption.setTatapChkBx(true);
        runOption.setTaModelCoverageChkBx(true);
        runOption.setTaSTMResultsChkBx(true);
        runOption.setTaPDFReportChkBx(true);
        this.matlabBuilder.setTestRunTypeList(runOption);
        project.getBuildersList().add(this.matlabBuilder);
        FreeStyleBuild build = project.scheduleBuild2(0).get();
        return build;
    }
    
    private URL getResource(String resource) {
        return MatlabBuilderTest.class.getClassLoader().getResource(resource); 
    }
}
