package com.mathworks.ci;

import com.gargoylesoftware.htmlunit.*;
import com.gargoylesoftware.htmlunit.html.*;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import hudson.tasks.Builder;
import org.junit.*;
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


import org.jvnet.hudson.test.JenkinsRule;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.*;


public class RunMATLABTestsInteg {

    private FreeStyleProject project;
    private static String matlabExecutorAbsolutePath;
    private UseMatlabVersionBuildWrapper buildWrapper;
    private RunMatlabTestsBuilder testBuilder;
    private static String FileSeperator;
    private TestData values;
    private static String VERSION_INFO_XML_FILE = "VersionInfo.xml";
    private static URL url;

    @Rule
    public JenkinsRule jenkins = new JenkinsRule();

    @Before
    public void testSetup() throws IOException {

        this.project = jenkins.createFreeStyleProject();
        this.testBuilder = new RunMatlabTestsBuilder();
        this.buildWrapper = new UseMatlabVersionBuildWrapper();
    }
    @BeforeClass
   public static void classSetup() throws URISyntaxException, IOException {
        ClassLoader classLoader = MatlabBuilderTest.class.getClassLoader();
        if (!System.getProperty("os.name").startsWith("Win")) {
            FileSeperator = "/";
            url = classLoader.getResource("com/mathworks/ci/linux/bin/matlab.sh");
            try {
                matlabExecutorAbsolutePath = new File(url.toURI()).getAbsolutePath();
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



    @Test
    public void verifyMATLABEmptyRootError() throws Exception{
            project.getBuildWrappersList().add(this.buildWrapper);
            HtmlPage configurePage = jenkins.createWebClient().goTo("job/test0/configure");
            HtmlCheckBoxInput matlabver=configurePage.getElementByName("com-mathworks-ci-UseMatlabVersionBuildWrapper");
            matlabver.setChecked(true);
            WebAssert.assertTextPresent(configurePage, TestMessage.getValue("Builder.matlab.root.empty.error"));

    }

    @Test
    public void verifyInvalidMATLABRootError() throws Exception{
        //this.buildWrapper.setMatlabRootFolder(values.getPropValues("matlab.root.path"));
        this.buildWrapper.setMatlabRootFolder("/Applications/MATLAB_R2015b.app");
        project.getBuildWrappersList().add(this.buildWrapper);
        HtmlPage configurePage = jenkins.createWebClient().goTo("job/test0/configure");
        HtmlCheckBoxInput matlabver=configurePage.getElementByName("com-mathworks-ci-UseMatlabVersionBuildWrapper");
        matlabver.setChecked(true);
        WebAssert.assertTextPresent(configurePage, TestMessage.getValue("Builder.invalid.matlab.root.warning"));

    }

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
    
    @Test
    public void verifyBuilderFailsForInvalidMATLABPath() throws Exception {
        //this.buildWrapper.setMatlabRootFolder(values.getPropValues("matlab.root.path"));
        this.buildWrapper.setMatlabRootFolder("/Applications/MATLAB_R2015b.app");
        project.getBuildWrappersList().add(this.buildWrapper);
        project.getBuildersList().add(this.testBuilder);

        FreeStyleBuild build = project.scheduleBuild2(0).get();
        jenkins.assertBuildStatus(Result.FAILURE, build);
    }

    @Test
    public void verifyBuildFailureWhenMatlabException() throws Exception {
        this.buildWrapper.setMatlabRootFolder(getMatlabroot("R2018b"));
        project.getBuildWrappersList().add(this.buildWrapper);
        RunMatlabTestsBuilderTester tester =
                new RunMatlabTestsBuilderTester(matlabExecutorAbsolutePath, "-positiveFail");
        project.getBuildersList().add(tester);
        FreeStyleBuild build = project.scheduleBuild2(0).get();
        jenkins.assertBuildStatus(Result.FAILURE, build);
    }

    @Test
    public void verifyBuildPassWhenTestPass() throws Exception {
        this.buildWrapper.setMatlabRootFolder(getMatlabroot("R2018b"));
        project.getBuildWrappersList().add(this.buildWrapper);
        RunMatlabTestsBuilderTester tester =
                new RunMatlabTestsBuilderTester(matlabExecutorAbsolutePath, "-positive");
        project.getBuildersList().add(tester);
        FreeStyleBuild build = project.scheduleBuild2(0).get();
        jenkins.assertBuildStatus(Result.SUCCESS, build);
    }
/*
    @Test
    public void verifyRunTestAutomaticallyIsDefault() throws Exception {
        this.buildWrapper.setMatlabRootFolder(getMatlabroot("R2018b"));
        project.getBuildWrappersList().add(this.buildWrapper);
        //setAllTestArtifacts(true, testBuilder);
        project.getBuildersList().add(this.testBuilder);
        FreeStyleBuild build = project.scheduleBuild2(0).get();
        jenkins.assertLogContains("run_matlab_command", build);
        jenkins.assertLogContains("\'PDFReport\',true,\'TAPResults\',true,"
                + "\'JUnitResults\',true,\'SimulinkTestResults\',true,"
                + "\'CoberturaCodeCoverage\',true,\'CoberturaModelCoverage\',true", build);
    }*/

    /*
     * Test to verify no parameters are sent in runMatlabTests when no artifacts are selected.
     */

    @Test
    public void veriyEmptyParameters() throws Exception {
        this.buildWrapper.setMatlabRootFolder(getMatlabroot("R2018b"));
        project.getBuildWrappersList().add(this.buildWrapper);
        project.getBuildersList().add(this.testBuilder);
        FreeStyleBuild build = project.scheduleBuild2(0).get();
        jenkins.assertLogContains("run_matlab_command", build);
        jenkins.assertLogContains("exit(runMatlabTests())", build);
    }

    @Test
    public void verifyMATLABscratchFileGenerated() throws Exception {
        this.buildWrapper.setMatlabRootFolder(getMatlabroot("R2018b"));
        project.getBuildWrappersList().add(this.buildWrapper);
        //setAllTestArtifacts(false, testBuilder);
        project.getBuildersList().add(testBuilder);
        FreeStyleBuild build = project.scheduleBuild2(0).get();
        File matlabRunner = new File(build.getWorkspace() + File.separator + "runMatlabTests.m");
        Assert.assertTrue(matlabRunner.exists());
    }

    @Test
    public void verifyJUnitFilePathInput() throws Exception{
        this.buildWrapper.setMatlabRootFolder(getMatlabroot("R2015a"));
        project.getBuildWrappersList().add(this.buildWrapper);
        project.getBuildersList().add(this.testBuilder);
        HtmlPage page = jenkins.createWebClient().goTo("job/test0/configure");
        HtmlCheckBoxInput junitChkbx = page.getElementByName("junitArtifact");
        junitChkbx.setChecked(true);
        Thread.sleep(2000);
        HtmlTextInput junitFilePathInput=(HtmlTextInput) page.getElementByName("_.junitReportFilePath");
        Assert.assertEquals("matlabTestArtifacts/junittestresults.xml",junitFilePathInput.getValueAttribute());
        //WebAssert.assertTextPresent(page,"");
    }

    @Test
    public void verifyTAPTestFilePathInput() throws Exception{
        this.buildWrapper.setMatlabRootFolder(getMatlabroot("R2013b"));
        project.getBuildWrappersList().add(this.buildWrapper);
        project.getBuildersList().add(this.testBuilder);
        HtmlPage page = jenkins.createWebClient().goTo("job/test0/configure");
        HtmlCheckBoxInput tapChkbx = page.getElementByName("tapArtifact");
        tapChkbx.setChecked(true);
        Thread.sleep(2000);
        HtmlTextInput tapFilePathInput=(HtmlTextInput) page.getElementByName("_.tapReportFilePath");
        Assert.assertEquals("matlabTestArtifacts/taptestresults.tap",tapFilePathInput.getValueAttribute());

    }

    @Test
    public void verifyPDFReportFilePathInput() throws Exception{
        this.buildWrapper.setMatlabRootFolder(getMatlabroot("R2017a"));
        project.getBuildWrappersList().add(this.buildWrapper);
        project.getBuildersList().add(this.testBuilder);
        HtmlPage page = jenkins.createWebClient().goTo("job/test0/configure");
        HtmlCheckBoxInput pdfChkbx = page.getElementByName("pdfReportArtifact");
        pdfChkbx.setChecked(true);
        Thread.sleep(2000);
        HtmlTextInput PDFFilePathInput=(HtmlTextInput) page.getElementByName("_.pdfReportFilePath");
        Assert.assertEquals("matlabTestArtifacts/testreport.pdf",PDFFilePathInput.getValueAttribute());

    }

    @Test
    public void verifyCoberturaFilePathInput() throws Exception {
        this.buildWrapper.setMatlabRootFolder(getMatlabroot("R2017a"));
        project.getBuildWrappersList().add(this.buildWrapper);
        project.getBuildersList().add(this.testBuilder);
        HtmlPage page = jenkins.createWebClient().goTo("job/test0/configure");
        HtmlCheckBoxInput coberturaChkBx = page.getElementByName("coberturaArtifact");
        coberturaChkBx.setChecked(true);
        Thread.sleep(2000);
        //WebAssert.assertTextPresent(page,
                //TestMessage.getValue("Builder.matlab.cobertura.support.warning"));
        HtmlTextInput coberturaCodeCoverageFileInput=(HtmlTextInput) page.getElementByName("_.coberturaReportFilePath");
        Assert.assertEquals("matlabTestArtifacts/cobertura.xml",coberturaCodeCoverageFileInput.getValueAttribute());
    }


    @Test
    public void verifyModelCoverageFilePathInput() throws Exception {
        this.buildWrapper.setMatlabRootFolder(getMatlabroot("R2018a"));
        project.getBuildWrappersList().add(this.buildWrapper);
        project.getBuildersList().add(this.testBuilder);
        HtmlPage page = jenkins.createWebClient().goTo("job/test0/configure");
        HtmlCheckBoxInput modelCoverageChkBx = page.getElementByName("modelCoverageArtifact");
        modelCoverageChkBx.setChecked(true);
        Thread.sleep(2000);
        //WebAssert.assertTextPresent(page,
                //TestMessage.getValue("Builder.matlab.modelcoverage.support.warning"));
        HtmlTextInput coberturaModelCoverageFileInput=(HtmlTextInput) page.getElementByName("_.modelCoverageFilePath");
        Assert.assertEquals("matlabTestArtifacts/coberturamodelcoverage.xml",coberturaModelCoverageFileInput.getValueAttribute());
    }


    @Test
    public void verifySTMResultsFilePathInput() throws Exception {
        this.buildWrapper.setMatlabRootFolder(getMatlabroot("R2018b"));
        project.getBuildWrappersList().add(this.buildWrapper);
        project.getBuildersList().add(this.testBuilder);
        HtmlPage page = jenkins.createWebClient().goTo("job/test0/configure");
        HtmlCheckBoxInput stmResultsChkBx = page.getElementByName("stmResultsArtifact");
        stmResultsChkBx.setChecked(true);
        Thread.sleep(2000);
        //WebAssert.assertTextPresent(page,
                //TestMessage.getValue("Builder.matlab.exportstmresults.support.warning"));
        HtmlTextInput STMRFilePathInput=(HtmlTextInput) page.getElementByName("_.stmResultsFilePath");
        Assert.assertEquals("matlabTestArtifacts/simulinktestresults.mldatx",STMRFilePathInput.getValueAttribute());
    }

    @Test
    public void verifyCoberturaError() throws Exception {
        this.buildWrapper.setMatlabRootFolder(getMatlabroot("/fake/path"));
        project.getBuildWrappersList().add(this.buildWrapper);
        project.getBuildersList().add(this.testBuilder);
        HtmlPage page = jenkins.createWebClient().goTo("job/test0/configure");
        HtmlCheckBoxInput coberturaChkBx = page.getElementByName("coberturaArtifact");
        coberturaChkBx.setChecked(true);
        Thread.sleep(2000);
        String pageText = page.asText();
        String filteredPageText = pageText
                .replaceFirst(TestMessage.getValue("Builder.invalid.matlab.root.warning"), "");
        //Assert.assertTrue(filteredPageText
                //.contains(TestMessage.getValue("Builder.invalid.matlab.root.warning")));
    }


    /*
     * Test to verify if Matrix build fails when MATLAB is not available.
     */
    @Test
    public void verifyMatrixBuildFails() throws Exception {
        MatrixProject matrixProject = jenkins.createProject(MatrixProject.class);
        Axis axes = new Axis("VERSION", "R2018a", "R2018b");
        matrixProject.setAxes(new AxisList(axes));
        String matlabRoot = getMatlabroot("R2018b");
        this.buildWrapper.setMatlabRootFolder(matlabRoot.replace("R2018b", "$VERSION"));
        matrixProject.getBuildWrappersList().add(this.buildWrapper);


        matrixProject.getBuildersList().add(testBuilder);

        // Check for first matrix combination.

        Map<String, String> vals = new HashMap<String, String>();
        vals.put("VERSION", "R2018a");
        Combination c1 = new Combination(vals);
        MatrixRun build1 = matrixProject.scheduleBuild2(0).get().getRun(c1);

        //jenkins.assertLogContains("MATLAB_ROOT", build1);
        jenkins.assertBuildStatus(Result.FAILURE, build1);

        // Check for second Matrix combination

        Combination c2 = new Combination(vals);
        MatrixRun build2 = matrixProject.scheduleBuild2(0).get().getRun(c2);

        //jenkins.assertLogContains("MATLAB_ROOT", build2);
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
        this.buildWrapper.setMatlabRootFolder(matlabRoot.replace("R2018b", "$VERSION"));
        matrixProject.getBuildWrappersList().add(this.buildWrapper);
        RunMatlabTestsBuilderTester tester = new RunMatlabTestsBuilderTester(matlabExecutorAbsolutePath, "-positive");

        matrixProject.getBuildersList().add(tester);
        MatrixBuild build = matrixProject.scheduleBuild2(0).get();

        jenkins.assertLogContains("Triggering", build);
        jenkins.assertLogContains("R2018a completed", build);
        jenkins.assertLogContains("R2018b completed", build);
        jenkins.assertBuildStatus(Result.SUCCESS, build);
    }




}