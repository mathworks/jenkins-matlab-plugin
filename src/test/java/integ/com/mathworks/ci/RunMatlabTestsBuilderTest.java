package com.mathworks.ci;

/**
 * Copyright 2019-2024 The MathWorks, Inc.
 * 
 * Test class for RunMatlabTestsBuilder
 */

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ExecutionException;
import org.htmlunit.html.HtmlInput;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.htmlunit.WebAssert;
import org.htmlunit.html.HtmlCheckBoxInput;
import org.htmlunit.html.HtmlPage;
import org.htmlunit.html.HtmlSelect;
import com.mathworks.ci.freestyle.RunMatlabTestsBuilder.CoberturaArtifact;
import com.mathworks.ci.freestyle.RunMatlabTestsBuilder.JunitArtifact;
import com.mathworks.ci.freestyle.RunMatlabTestsBuilder.ModelCovArtifact;
import com.mathworks.ci.freestyle.RunMatlabTestsBuilder.PdfArtifact;
import com.mathworks.ci.freestyle.RunMatlabTestsBuilder.StmResultsArtifact;
import com.mathworks.ci.freestyle.RunMatlabTestsBuilder.TapArtifact;
import hudson.FilePath;
import hudson.matrix.Axis;
import hudson.matrix.AxisList;
import hudson.matrix.Combination;
import hudson.matrix.MatrixBuild;
import hudson.matrix.MatrixProject;
import hudson.matrix.MatrixRun;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import hudson.tasks.Builder;

import com.mathworks.ci.freestyle.RunMatlabTestsBuilder;
import com.mathworks.ci.freestyle.options.StartupOptions;

import static org.junit.Assert.*;

public class RunMatlabTestsBuilderTest {

    private static String matlabExecutorAbsolutePath;
    private FreeStyleProject project;
    private UseMatlabVersionBuildWrapper buildWrapper;
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
        testBuilder.setLoggingLevel("default");
        testBuilder.setOutputDetail("default");
        this.buildWrapper = new UseMatlabVersionBuildWrapper();
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
     * Test to verify if job fails when invalid MATLAB path is provided and
     * Exception is thrown
     */

    @Test
    public void verifyBuilderFailsForInvalidMATLABPath() throws Exception {
        this.buildWrapper.setMatlabBuildWrapperContent(new MatlabBuildWrapperContent(
                Message.getValue("matlab.custom.location"), "/fake/matlabroot/that/does/not/exist"));
        project.getBuildWrappersList().add(this.buildWrapper);
        project.getBuildersList().add(this.testBuilder);

        FreeStyleBuild build = project.scheduleBuild2(0).get();
        jenkins.assertBuildStatus(Result.FAILURE, build);
    }

    /*
     * Test to verify if Build FAILS when matlab test fails
     */

    @Test
    public void verifyBuildFailureWhenMatlabException() throws Exception {
        this.buildWrapper.setMatlabBuildWrapperContent(
                new MatlabBuildWrapperContent(Message.getValue("matlab.custom.location"), getMatlabroot("R2018b")));
        project.getBuildWrappersList().add(this.buildWrapper);
        RunMatlabTestsBuilderTester tester = new RunMatlabTestsBuilderTester(matlabExecutorAbsolutePath,
                "-positiveFail");
        project.getBuildersList().add(tester);
        FreeStyleBuild build = project.scheduleBuild2(0).get();
        jenkins.assertBuildStatus(Result.FAILURE, build);
    }

    /*
     * Test to verify if Build PASSES when matlab test PASSES
     */

    @Test
    public void verifyBuildPassWhenTestPass() throws Exception {
        this.buildWrapper.setMatlabBuildWrapperContent(
                new MatlabBuildWrapperContent(Message.getValue("matlab.custom.location"), getMatlabroot("R2018b")));
        project.getBuildWrappersList().add(this.buildWrapper);
        RunMatlabTestsBuilderTester tester = new RunMatlabTestsBuilderTester(matlabExecutorAbsolutePath, "-positive");
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

        // verLessthan() will check all the versions against 9.2 which is version of
        // R2017a
        assertFalse(rel.verLessThan(9.1));
        assertFalse(rel.verLessThan(9.0));
        assertFalse(rel.verLessThan(9.2));
        Assert.assertTrue(rel.verLessThan(9.9));
        Assert.assertTrue(rel.verLessThan(10.1));
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
        testBuilder.setStartupOptions(new StartupOptions("-nojvm -uniqueoption"));
        project.getBuildersList().add(this.testBuilder);
        FreeStyleBuild build = project.scheduleBuild2(0).get();
        jenkins.assertLogContains("run-matlab-command", build);
        jenkins.assertLogContains("-nojvm -uniqueoption", build);
    }

    /*
     * Test to verify appropriate test atrtifact values are passed. Need to
     * include in integration test.
     */

    public void verifySpecificTestArtifactsParameters() throws Exception {
        this.buildWrapper.setMatlabBuildWrapperContent(
                new MatlabBuildWrapperContent(Message.getValue("matlab.custom.location"), getMatlabroot("R2018b")));
        project.getBuildWrappersList().add(this.buildWrapper);
        RunMatlabTestsBuilder.TapArtifact tap = new TapArtifact("mytap/report.tap");

        RunMatlabTestsBuilder.StmResultsArtifact stmResults = new StmResultsArtifact("mystm/results.mldatx");

        testBuilder.setTapArtifact(tap);
        testBuilder.setStmResultsArtifact(stmResults);

        project.getBuildersList().add(this.testBuilder);
        FreeStyleBuild build = project.scheduleBuild2(0).get();
        jenkins.assertLogContains("run-matlab-command", build);
        jenkins.assertLogContains("TAPPlugin", build);
        jenkins.assertLogContains("mytap/report.tap", build);
        jenkins.assertLogContains("TestManagerResultsPlugin", build);
        jenkins.assertLogContains("mystm/results.mldatx", build);
    }

    /*
     * Test to verify default test atrtifact file location.
     */

    @Test
    public void verifyDefaultArtifactLocation() throws Exception {
        this.buildWrapper.setMatlabBuildWrapperContent(
                new MatlabBuildWrapperContent(Message.getValue("matlab.custom.location"), getMatlabroot("R2017a")));
        project.getBuildWrappersList().add(this.buildWrapper);
        project.getBuildersList().add(this.testBuilder);
        HtmlPage page = jenkins.createWebClient().goTo("job/test0/configure");
        HtmlCheckBoxInput tapArtifact = page.getElementByName("tapArtifact");
        HtmlCheckBoxInput pdfReportArtifact = page.getElementByName("pdfReportArtifact");
        HtmlCheckBoxInput junitArtifact = page.getElementByName("junitArtifact");
        HtmlCheckBoxInput stmResultsArtifact = page.getElementByName("stmResultsArtifact");
        HtmlCheckBoxInput coberturaArtifact = page.getElementByName("coberturaArtifact");
        HtmlCheckBoxInput modelCoverageArtifact = page.getElementByName("modelCoverageArtifact");

        tapArtifact.click();
        pdfReportArtifact.click();
        junitArtifact.click();
        stmResultsArtifact.click();
        coberturaArtifact.click();
        modelCoverageArtifact.click();
        Thread.sleep(2000);

        WebAssert.assertTextPresent(page, "matlabTestArtifacts/taptestresults.tap");
        WebAssert.assertTextPresent(page, "matlabTestArtifacts/junittestresults.xml");
        WebAssert.assertTextPresent(page, "matlabTestArtifacts/testreport.pdf");
        WebAssert.assertTextPresent(page, "matlabTestArtifacts/simulinktestresults.mldatx");
        WebAssert.assertTextPresent(page, "matlabTestArtifacts/cobertura.xml");
        WebAssert.assertTextPresent(page, "matlabTestArtifacts/coberturamodelcoverage.xml");
    }

    /*
     * Test to verify text box shows up on sourceFolder option click and text is
     * empty.
     */

    @Test
    public void verifySourceFolderDefaultState() throws Exception {
        this.buildWrapper.setMatlabBuildWrapperContent(
                new MatlabBuildWrapperContent(Message.getValue("matlab.custom.location"), getMatlabroot("R2017a")));
        project.getBuildWrappersList().add(this.buildWrapper);
        project.getBuildersList().add(this.testBuilder);
        HtmlPage page = jenkins.createWebClient().goTo("job/test0/configure");
        HtmlCheckBoxInput sourceFolder = page.getElementByName("_.sourceFolder");
        sourceFolder.click();
        WebAssert.assertElementPresentByXPath(page, "//input[@name=\"_.srcFolderPath\"]");
        HtmlInput srcFolderPath = page.getElementByName("_.srcFolderPath");
        assertEquals("", srcFolderPath.getTextContent());
    }

    /*
     * Test to verify text box shows up on SelectBy option click and text is empty.
     */

    @Test
    public void verifySelectByFolderDefaultState() throws Exception {
        this.buildWrapper.setMatlabBuildWrapperContent(
                new MatlabBuildWrapperContent(Message.getValue("matlab.custom.location"), getMatlabroot("R2017a")));
        project.getBuildWrappersList().add(this.buildWrapper);
        project.getBuildersList().add(this.testBuilder);
        HtmlPage page = jenkins.createWebClient().goTo("job/test0/configure");
        HtmlCheckBoxInput sourceFolder = page.getElementByName("_.selectByFolder");
        sourceFolder.click();
        Thread.sleep(2000);
        WebAssert.assertElementPresentByXPath(page, "//input[@name=\"_.testFolders\"]");
        HtmlInput srcFolderPath = page.getElementByName("_.testFolders");
        assertEquals("", srcFolderPath.getTextContent());
    }

    /*
     * Test to verify text box shows up on SelectByTag option click and text is
     * empty.
     */

    @Test
    public void verifySelectByTagDefaultState() throws Exception {
        this.buildWrapper.setMatlabBuildWrapperContent(
                new MatlabBuildWrapperContent(Message.getValue("matlab.custom.location"), getMatlabroot("R2017a")));
        project.getBuildWrappersList().add(this.buildWrapper);
        project.getBuildersList().add(this.testBuilder);
        HtmlPage page = jenkins.createWebClient().goTo("job/test0/configure");
        HtmlCheckBoxInput sourceFolder = page.getElementByName("_.selectByTag");
        sourceFolder.click();
        Thread.sleep(2000);
        WebAssert.assertElementPresentByXPath(page, "//input[@name=\"_.testTag\"]");
        HtmlInput srcFolderPath = page.getElementByName("_.testTag");
        assertEquals("", srcFolderPath.getTextContent());
    }

    /*
     * Test to verify only specific test atrtifact are passed.
     */

    @Test
    public void verifyAllTestArtifactsParameters() throws Exception {
        this.buildWrapper.setMatlabBuildWrapperContent(
                new MatlabBuildWrapperContent(Message.getValue("matlab.custom.location"), getMatlabroot("R2018b")));
        project.getBuildWrappersList().add(this.buildWrapper);
        RunMatlabTestsBuilder.TapArtifact tap = new TapArtifact("mytap/report.tap");

        RunMatlabTestsBuilder.PdfArtifact pdf = new PdfArtifact("mypdf/report.pdf");

        RunMatlabTestsBuilder.JunitArtifact junit = new JunitArtifact("myjunit/report.xml");

        RunMatlabTestsBuilder.CoberturaArtifact cobertura = new CoberturaArtifact("mycobertura/report.xml");

        RunMatlabTestsBuilder.ModelCovArtifact modelCov = new ModelCovArtifact("mymodel/report.xml");

        RunMatlabTestsBuilder.StmResultsArtifact stmResults = new StmResultsArtifact("mystm/results.mldatx");

        testBuilder.setTapArtifact(tap);
        testBuilder.setPdfReportArtifact(pdf);
        testBuilder.setJunitArtifact(junit);
        testBuilder.setCoberturaArtifact(cobertura);
        testBuilder.setModelCoverageArtifact(modelCov);
        testBuilder.setStmResultsArtifact(stmResults);

        project.getBuildersList().add(this.testBuilder);
        FreeStyleBuild build = project.scheduleBuild2(0).get();
        jenkins.assertLogContains("run-matlab-command", build);
        jenkins.assertLogContains("\'PDFTestReport\',\'mypdf/report.pdf\'", build);
        jenkins.assertLogContains("\'TAPTestResults\',\'mytap/report.tap\'", build);
        jenkins.assertLogContains("\'JUnitTestResults\',\'myjunit/report.xml\'", build);
        jenkins.assertLogContains("\'SimulinkTestResults\',\'mystm/results.mldatx\'", build);
        jenkins.assertLogContains("\'CoberturaCodeCoverage\',\'mycobertura/report.xml\'", build);
        jenkins.assertLogContains("\'CoberturaModelCoverage\',\'mymodel/report.xml\'", build);

    }

    /*
     * Test to verify no parameters are sent in test runner when no artifacts are
     * selected.
     */

    @Test
    public void verifyEmptyParameters() throws Exception {
        this.buildWrapper.setMatlabBuildWrapperContent(
                new MatlabBuildWrapperContent(Message.getValue("matlab.custom.location"), getMatlabroot("R2018b")));
        project.getBuildWrappersList().add(this.buildWrapper);
        project.getBuildersList().add(this.testBuilder);
        FreeStyleBuild build = project.scheduleBuild2(0).get();
        jenkins.assertLogContains("run-matlab-command", build);
        jenkins.assertLogNotContains("'OutputDetail'", build);
        jenkins.assertLogNotContains("'PDFTestReport'", build);
        jenkins.assertLogNotContains("'Strict'", build);
        jenkins.assertLogNotContains("'SourceFolder'", build);
    }

    /*
     * Test to verify if appropriate MATALB runner file is copied in workspace.
     * 
     * NOTE: This test assumes there is no MATLAB installed and is not on System
     * Path.
     *
     */
    @Test
    public void verifyMATLABrunnerFileGeneratedForAutomaticOption() throws Exception {
        this.buildWrapper.setMatlabBuildWrapperContent(
                new MatlabBuildWrapperContent(Message.getValue("matlab.custom.location"), getMatlabroot("R2018b")));
        project.getBuildWrappersList().add(this.buildWrapper);
        project.getBuildersList().add(testBuilder);
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
        project.getBuildersList().add(testBuilder);
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

        matrixProject.getBuildersList().add(testBuilder);

        // Check for first matrix combination.

        Map<String, String> vals = new HashMap<String, String>();
        vals.put("VERSION", "R2018a");
        Combination c1 = new Combination(vals);
        MatrixRun build1 = matrixProject.scheduleBuild2(0).get().getRun(c1);

        jenkins.assertLogContains("run-matlab-command", build1);
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
        RunMatlabTestsBuilderTester tester = new RunMatlabTestsBuilderTester(matlabExecutorAbsolutePath, "-positive");

        matrixProject.getBuildersList().add(tester);
        MatrixBuild build = matrixProject.scheduleBuild2(0).get();

        jenkins.assertLogContains("Triggering", build);
        jenkins.assertLogContains("R2018a completed", build);
        jenkins.assertLogContains("R2018b completed", build);
        jenkins.assertBuildStatus(Result.SUCCESS, build);
    }

    /*
     * Test to verify if MATALB scratch file is not in workspace.
     */
    @Test
    public void verifyMATLABscratchFileGenerated() throws Exception {
        this.buildWrapper.setMatlabBuildWrapperContent(
                new MatlabBuildWrapperContent(Message.getValue("matlab.custom.location"), getMatlabroot("R2018b")));
        project.getBuildWrappersList().add(this.buildWrapper);
        project.getBuildersList().add(testBuilder);
        FreeStyleBuild build = project.scheduleBuild2(0).get();
        File matlabRunner = new File(build.getWorkspace() + File.separator + "runnerScript.m");
        Assert.assertFalse(matlabRunner.exists());
    }

    /*
     * Test to verify Use Parallel check box present.
     */
    @Test
    public void verifyUseParallelPresent() throws Exception {
        this.buildWrapper.setMatlabBuildWrapperContent(
                new MatlabBuildWrapperContent(Message.getValue("matlab.custom.location"), getMatlabroot("R2018b")));
        project.getBuildWrappersList().add(this.buildWrapper);
        project.getBuildersList().add(this.testBuilder);
        HtmlPage page = jenkins.createWebClient().goTo("job/test0/configure");
        WebAssert.assertElementPresentByXPath(page, "//input[@name=\"_.useParallel\"]");
    }

    /*
     * Test to verify Strict check box present.
     */

    @Test
    public void verifyStrictPresent() throws Exception {
        this.buildWrapper.setMatlabBuildWrapperContent(
                new MatlabBuildWrapperContent(Message.getValue("matlab.custom.location"), getMatlabroot("R2018b")));
        project.getBuildWrappersList().add(this.buildWrapper);
        project.getBuildersList().add(this.testBuilder);
        HtmlPage page = jenkins.createWebClient().goTo("job/test0/configure");
        WebAssert.assertElementPresentByXPath(page, "//input[@name=\"_.strict\"]");
    }

    /*
     * Test to verify Logging Level is present.
     */

    @Test
    public void verifyLoggingLevelPresent() throws Exception {
        this.buildWrapper.setMatlabBuildWrapperContent(
                new MatlabBuildWrapperContent(Message.getValue("matlab.custom.location"), getMatlabroot("R2018b")));
        project.getBuildWrappersList().add(this.buildWrapper);
        project.getBuildersList().add(this.testBuilder);
        HtmlPage page = jenkins.createWebClient().goTo("job/test0/configure");
        WebAssert.assertElementPresentByXPath(page, "//select[@name=\"_.loggingLevel\"]");
    }

    /*
     * Test to verify Output Detail is present.
     */

    @Test
    public void verifyOutputDetailPresent() throws Exception {
        this.buildWrapper.setMatlabBuildWrapperContent(
                new MatlabBuildWrapperContent(Message.getValue("matlab.custom.location"), getMatlabroot("R2018b")));
        project.getBuildWrappersList().add(this.buildWrapper);
        project.getBuildersList().add(this.testBuilder);
        HtmlPage page = jenkins.createWebClient().goTo("job/test0/configure");
        WebAssert.assertElementPresentByXPath(page, "//select[@name=\"_.outputDetail\"]");
    }

    /*
     * Test to verify Logging Level set to default
     */

    @Test
    public void verifyLoggingLevelSetToDefault() throws Exception {
        this.buildWrapper.setMatlabBuildWrapperContent(
                new MatlabBuildWrapperContent(Message.getValue("matlab.custom.location"), getMatlabroot("R2018b")));
        project.getBuildWrappersList().add(this.buildWrapper);
        project.getBuildersList().add(this.testBuilder);
        HtmlPage page = jenkins.createWebClient().goTo("job/test0/configure");
        HtmlSelect loggingLevel = page.getElementByName("_.loggingLevel");
        assertEquals("default", loggingLevel.getAttribute("value"));
    }

    /*
     * Test to verify Output Detail set to default
     */

    @Test
    public void verifyOutputDetailSetToDefault() throws Exception {
        this.buildWrapper.setMatlabBuildWrapperContent(
                new MatlabBuildWrapperContent(Message.getValue("matlab.custom.location"), getMatlabroot("R2018b")));
        project.getBuildWrappersList().add(this.buildWrapper);
        project.getBuildersList().add(this.testBuilder);
        HtmlPage page = jenkins.createWebClient().goTo("job/test0/configure");
        HtmlSelect outputDetail = page.getElementByName("_.outputDetail");
        assertEquals("default", outputDetail.getAttribute("value"));
    }

    /*
     * @Integ
     * Test To verify if Logging level is set correctly
     * 
     */

    public void verifyLoggingLevelSet() throws Exception {
        this.buildWrapper.setMatlabBuildWrapperContent(new MatlabBuildWrapperContent(
                Message.getValue("matlab.custom.location"), getMatlabroot("R2018b")));
        project.getBuildWrappersList().add(this.buildWrapper);
        Map<String, String> loggingLevel = new HashMap<String, String>();
        loggingLevel.put("None", "'LoggingLevel', 0");
        loggingLevel.put("Terse", "'LoggingLevel', 1");
        loggingLevel.put("Concise", "'LoggingLevel', 2");
        loggingLevel.put("Detailed", "'LoggingLevel', 3");
        loggingLevel.put("Verbose", "'LoggingLevel', 4");
        loggingLevel.forEach((key, val) -> {
            testBuilder.setLoggingLevel(key);
            project.getBuildersList().add(this.testBuilder);
            FreeStyleBuild build;
            try {
                build = project.scheduleBuild2(0).get();
                jenkins.assertLogContains(val, build);
            } catch (InterruptedException | ExecutionException | IOException e) {
                System.out.println("Build Failed, refer logs for details");
                e.printStackTrace();
            }
        });
    }

    /*
     * @Integ
     * Test To verify if Output Detail is set correctly
     * 
     */

    public void verifyOutputDetailSet() throws Exception {
        this.buildWrapper.setMatlabBuildWrapperContent(new MatlabBuildWrapperContent(
                Message.getValue("matlab.custom.location"), getMatlabroot("R2018b")));
        project.getBuildWrappersList().add(this.buildWrapper);
        testBuilder.setLoggingLevel("None");
        Map<String, String> outputDetail = new HashMap<String, String>();
        outputDetail.put("none", "'OutputDetail', 0");
        outputDetail.put("terse", "'OutputDetail', 1");
        outputDetail.put("concise", "'OutputDetail', 2");
        outputDetail.put("detailed", "'OutputDetail', 3");
        outputDetail.put("verbose", "'OutputDetail', 4");
        outputDetail.forEach((key, val) -> {
            testBuilder.setOutputDetail(key);
            project.getBuildersList().add(this.testBuilder);
            FreeStyleBuild build;
            try {
                build = project.scheduleBuild2(0).get();
                jenkins.assertLogContains(val, build);
            } catch (InterruptedException | ExecutionException | IOException e) {
                System.out.println("Build Failed, refer logs for details");
                e.printStackTrace();
            }
        });
    }

    /*
     * @Integ
     * Test To verify when Strict option set
     * 
     */

    public void verifyStrictSet() throws Exception {
        this.buildWrapper.setMatlabBuildWrapperContent(new MatlabBuildWrapperContent(
                Message.getValue("matlab.custom.location"), getMatlabroot("R2018b")));
        project.getBuildWrappersList().add(this.buildWrapper);
        testBuilder.setLoggingLevel("None");
        testBuilder.setStrict(true);
        project.getBuildersList().add(this.testBuilder);
        FreeStyleBuild build = project.scheduleBuild2(0).get();
        jenkins.assertLogContains("FailOnWarningsPlugin", build);

    }

    /*
     * @Integ
     * Test To verify when Strict option not set
     * 
     */

    public void verifyStrictNotSet() throws Exception {
        this.buildWrapper.setMatlabBuildWrapperContent(new MatlabBuildWrapperContent(
                Message.getValue("matlab.custom.location"), getMatlabroot("R2018b")));
        project.getBuildWrappersList().add(this.buildWrapper);
        testBuilder.setLoggingLevel("None");
        testBuilder.setStrict(false);
        project.getBuildersList().add(this.testBuilder);
        FreeStyleBuild build = project.scheduleBuild2(0).get();
        jenkins.assertLogNotContains("FailOnWarningsPlugin", build);

    }

    /*
     * @Integ
     * Test To verify when Run in Parallel option is set
     * 
     */

    public void verifyRunParallelSet() throws Exception {
        this.buildWrapper.setMatlabBuildWrapperContent(new MatlabBuildWrapperContent(
                Message.getValue("matlab.custom.location"), getMatlabroot("R2018b")));
        project.getBuildWrappersList().add(this.buildWrapper);
        testBuilder.setLoggingLevel("None");
        testBuilder.setUseParallel(true);
        project.getBuildersList().add(this.testBuilder);
        FreeStyleBuild build = project.scheduleBuild2(0).get();
        jenkins.assertLogContains("runInParallel", build);
    }

    /*
     * @Integ
     * Test To verify when Run in Parallel option is set
     * 
     */

    public void verifyRunParallelNotSet() throws Exception {
        this.buildWrapper.setMatlabBuildWrapperContent(new MatlabBuildWrapperContent(
                Message.getValue("matlab.custom.location"), getMatlabroot("R2018b")));
        project.getBuildWrappersList().add(this.buildWrapper);
        testBuilder.setLoggingLevel("None");
        testBuilder.setUseParallel(false);
        project.getBuildersList().add(this.testBuilder);
        FreeStyleBuild build = project.scheduleBuild2(0).get();
        jenkins.assertLogNotContains("runInParallel", build);
    }
}
