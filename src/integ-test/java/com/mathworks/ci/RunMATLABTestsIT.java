package com.mathworks.ci;

import com.gargoylesoftware.htmlunit.*;
import com.gargoylesoftware.htmlunit.html.*;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import hudson.tasks.Builder;
import org.junit.*;
import hudson.matrix.Axis;
import hudson.matrix.AxisList;
import hudson.matrix.Combination;
import hudson.matrix.MatrixBuild;
import hudson.matrix.MatrixProject;
import hudson.matrix.MatrixRun;


import org.junit.rules.Timeout;
import org.jvnet.hudson.test.ExtractResourceSCM;
import org.jvnet.hudson.test.JenkinsRule;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import com.jcabi.xml.XML;
import com.jcabi.xml.XMLDocument;

import static org.junit.Assert.assertFalse;


public class RunMATLABTestsIT {

    private FreeStyleProject project;
    private UseMatlabVersionBuildWrapper buildWrapper;
    private RunMatlabTestsBuilder testBuilder;

    @Rule
    public Timeout timeout = Timeout.seconds(0);

    @Rule
    public JenkinsRule jenkins = new JenkinsRule();

    @Before
    public void testSetup() throws IOException {

        this.project = jenkins.createFreeStyleProject();
        this.testBuilder = new RunMatlabTestsBuilder();
        this.buildWrapper = new UseMatlabVersionBuildWrapper();
        testBuilder.setLoggingLevel("default");
        testBuilder.setOutputDetail("default");
    }

    @After
    public void testTearDown() {
        this.project = null;
        this.testBuilder = null;
    }

    /* Helper function to read XML file as string */
    public String xmlToString(String codegenReportPath) throws FileNotFoundException {
        File codeCoverageFile = new File(codegenReportPath);
        XML xml = new XMLDocument(codeCoverageFile);
        return xml.toString();
    }


//     @Test
//     public void verifyMATLABEmptyRootError() throws Exception{
//             project.getBuildWrappersList().add(this.buildWrapper);
//             HtmlPage configurePage = jenkins.createWebClient().goTo("job/test0/configure");
//             HtmlCheckBoxInput matlabver=configurePage.getElementByName("com-mathworks-ci-UseMatlabVersionBuildWrapper");
//             matlabver.setChecked(true);
//             WebAssert.assertTextPresent(configurePage, TestMessage.getValue("Builder.matlab.root.empty.error"));

//     }

    @Test
    public void verifyInvalidMATLABRootError() throws Exception{
        this.buildWrapper.setMatlabBuildWrapperContent(new MatlabBuildWrapperContent(Message.getValue("matlab.custom.location"), TestData.getPropValues("matlab.invalid.root.path")));
//        this.buildWrapper.setMatlabRootFolder(TestData.getPropValues("matlab.invalid.root.path"));
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
        this.buildWrapper.setMatlabBuildWrapperContent(new MatlabBuildWrapperContent(Message.getValue("matlab.custom.location"), "fake"));

//        this.buildWrapper.setMatlabRootFolder("fake");
        project.getBuildWrappersList().add(this.buildWrapper);
        project.getBuildersList().add(this.testBuilder);

        FreeStyleBuild build = project.scheduleBuild2(0).get();
        jenkins.assertLogContains(TestMessage.getValue("matlab.not.found.error"), build);
        jenkins.assertBuildStatus(Result.FAILURE, build);
    }

    @Test
    public void verifyJUnitFilePathInput() throws Exception{
        this.buildWrapper.setMatlabBuildWrapperContent(new MatlabBuildWrapperContent(Message.getValue("matlab.custom.location"), MatlabRootSetup.getMatlabRoot()));

//        this.buildWrapper.setMatlabRootFolder(getMatlabroot());
        project.getBuildWrappersList().add(this.buildWrapper);
        project.getBuildersList().add(this.testBuilder);
        HtmlPage page = jenkins.createWebClient().goTo("job/test0/configure");
        HtmlCheckBoxInput junitChkbx = page.getElementByName("junitArtifact");
        junitChkbx.setChecked(true);
        Thread.sleep(2000);
        HtmlTextInput junitFilePathInput=(HtmlTextInput) page.getElementByName("_.junitReportFilePath");
        Assert.assertEquals(TestData.getPropValues("junit.file.path"),junitFilePathInput.getValueAttribute());
    }

    @Test
    public void verifyTAPTestFilePathInput() throws Exception{
        this.buildWrapper.setMatlabBuildWrapperContent(new MatlabBuildWrapperContent(Message.getValue("matlab.custom.location"), MatlabRootSetup.getMatlabRoot()));
//        this.buildWrapper.setMatlabRootFolder(getMatlabroot());
        project.getBuildWrappersList().add(this.buildWrapper);
        project.getBuildersList().add(this.testBuilder);
        HtmlPage page = jenkins.createWebClient().goTo("job/test0/configure");
        HtmlCheckBoxInput tapChkbx = page.getElementByName("tapArtifact");
        tapChkbx.setChecked(true);
        Thread.sleep(2000);
        HtmlTextInput tapFilePathInput=(HtmlTextInput) page.getElementByName("_.tapReportFilePath");
//        Assert.assertEquals(TestData.getPropValues("taptestresult.file.path"),tapFilePathInput.getValueAttribute());
//
    }

    @Test
    public void verifyPDFReportFilePathInput() throws Exception{
        this.buildWrapper.setMatlabBuildWrapperContent(new MatlabBuildWrapperContent(Message.getValue("matlab.custom.location"), MatlabRootSetup.getMatlabRoot()));
//        this.buildWrapper.setMatlabRootFolder(getMatlabroot());
        project.getBuildWrappersList().add(this.buildWrapper);
        project.getBuildersList().add(this.testBuilder);
        HtmlPage page = jenkins.createWebClient().goTo("job/test0/configure");
        HtmlCheckBoxInput pdfChkbx = page.getElementByName("pdfReportArtifact");
        pdfChkbx.setChecked(true);
        Thread.sleep(2000);
        HtmlTextInput PDFFilePathInput=(HtmlTextInput) page.getElementByName("_.pdfReportFilePath");
//        Assert.assertEquals(TestData.getPropValues("pdftestreport.file.path"),PDFFilePathInput.getValueAttribute());
    }

    @Test
    public void verifyCoberturaFilePathInput() throws Exception {
        this.buildWrapper.setMatlabBuildWrapperContent(new MatlabBuildWrapperContent(Message.getValue("matlab.custom.location"), MatlabRootSetup.getMatlabRoot()));
//        this.buildWrapper.setMatlabRootFolder(getMatlabroot());
        project.getBuildWrappersList().add(this.buildWrapper);
        project.getBuildersList().add(this.testBuilder);
        HtmlPage page = jenkins.createWebClient().goTo("job/test0/configure");
        HtmlCheckBoxInput coberturaChkBx = page.getElementByName("coberturaArtifact");
        coberturaChkBx.setChecked(true);
        Thread.sleep(2000);
        HtmlTextInput coberturaCodeCoverageFileInput=(HtmlTextInput) page.getElementByName("_.coberturaReportFilePath");
//        Assert.assertEquals(TestData.getPropValues("cobertura.file.path"),coberturaCodeCoverageFileInput.getValueAttribute());
    }


    @Test
    public void verifyModelCoverageFilePathInput() throws Exception {
        this.buildWrapper.setMatlabBuildWrapperContent(new MatlabBuildWrapperContent(Message.getValue("matlab.custom.location"), MatlabRootSetup.getMatlabRoot()));
//        this.buildWrapper.setMatlabRootFolder(getMatlabroot());
        project.getBuildWrappersList().add(this.buildWrapper);
        project.getBuildersList().add(this.testBuilder);
        HtmlPage page = jenkins.createWebClient().goTo("job/test0/configure");
        HtmlCheckBoxInput modelCoverageChkBx = page.getElementByName("modelCoverageArtifact");
        modelCoverageChkBx.setChecked(true);
        Thread.sleep(2000);
        HtmlTextInput coberturaModelCoverageFileInput=(HtmlTextInput) page.getElementByName("_.modelCoverageFilePath");
//        Assert.assertEquals(TestData.getPropValues("modelcoverage.file.path"),coberturaModelCoverageFileInput.getValueAttribute());
    }


    @Test
    public void verifySTMResultsFilePathInput() throws Exception {
        this.buildWrapper.setMatlabBuildWrapperContent(new MatlabBuildWrapperContent(Message.getValue("matlab.custom.location"), MatlabRootSetup.getMatlabRoot()));
//        this.buildWrapper.setMatlabRootFolder(getMatlabroot());
        project.getBuildWrappersList().add(this.buildWrapper);
        project.getBuildersList().add(this.testBuilder);
        HtmlPage page = jenkins.createWebClient().goTo("job/test0/configure");
        HtmlCheckBoxInput stmResultsChkBx = page.getElementByName("stmResultsArtifact");
        stmResultsChkBx.setChecked(true);
        Thread.sleep(2000);
        HtmlTextInput STMRFilePathInput=(HtmlTextInput) page.getElementByName("_.stmResultsFilePath");
        Assert.assertEquals(TestData.getPropValues("stmresults.file.path"),STMRFilePathInput.getValueAttribute());
    }

    @Test
    public void verifyPDFReportCustomFilePathInput() throws Exception {
        this.buildWrapper.setMatlabBuildWrapperContent(new MatlabBuildWrapperContent(Message.getValue("matlab.custom.location"), MatlabRootSetup.getMatlabRoot()));
//        this.buildWrapper.setMatlabRootFolder(getMatlabroot());
        project.getBuildWrappersList().add(this.buildWrapper);
        RunMatlabTestsBuilder testingBuilder = new RunMatlabTestsBuilder();
        testingBuilder.setPdfReportArtifact(new RunMatlabTestsBuilder.PdfArtifact("abc/xyz.pdf"));
        project.getBuildersList().add(testingBuilder);
        FreeStyleBuild build = project.scheduleBuild2(0).get();
        jenkins.assertBuildStatus(Result.SUCCESS, build);
        String build_log = jenkins.getLog(build);
        System.out.println();
        jenkins.assertLogContains("abc/xyz", build);
    }

    @Test
    public void verifyCustomFilePathInputForArtifacts() throws Exception{
        this.buildWrapper.setMatlabBuildWrapperContent(new MatlabBuildWrapperContent(Message.getValue("matlab.custom.location"), MatlabRootSetup.getMatlabRoot()));
//        this.buildWrapper.setMatlabRootFolder(getMatlabroot());
        project.getBuildWrappersList().add(this.buildWrapper);
        RunMatlabTestsBuilder testingBuilder = new RunMatlabTestsBuilder();
        testingBuilder.setJunitArtifact(new RunMatlabTestsBuilder.JunitArtifact("TestArtifacts/junittestreport.xml"));
        testingBuilder.setTapArtifact(new RunMatlabTestsBuilder.TapArtifact("TestArtifacts/tapResult.tap"));
        testingBuilder.setCoberturaArtifact(new RunMatlabTestsBuilder.CoberturaArtifact("TestArtifacts/coberturaresult.xml"));
        testingBuilder.setStmResultsArtifact(new RunMatlabTestsBuilder.StmResultsArtifact("TestArtifacts/stmresult.mldatx"));
        testingBuilder.setModelCoverageArtifact(new RunMatlabTestsBuilder.ModelCovArtifact("TestArtifacts/mdlCovReport.xml"));
        project.getBuildersList().add(testingBuilder);
        FreeStyleBuild build = project.scheduleBuild2(0).get();
        jenkins.assertBuildStatus(Result.SUCCESS, build);
        jenkins.assertLogContains("TestArtifacts/junittestreport.xml", build);
        jenkins.assertLogContains("TestArtifacts/tapResult.tap", build);
        jenkins.assertLogContains("TestArtifacts/coberturaresult.xml", build);
        jenkins.assertLogContains("TestArtifacts/stmresult.mldatx", build);
        jenkins.assertLogContains("TestArtifacts/mdlCovReport.xml", build);
    }

    @Test
    public void verifyExtForPdfReport() throws Exception {
        Assume.assumeFalse
                (System.getProperty("os.name").toLowerCase().startsWith("mac"));
        this.buildWrapper.setMatlabBuildWrapperContent(new MatlabBuildWrapperContent(Message.getValue("matlab.custom.location"), MatlabRootSetup.getMatlabRoot()));
//        this.buildWrapper.setMatlabRootFolder(getMatlabroot());
        project.getBuildWrappersList().add(this.buildWrapper);
        RunMatlabTestsBuilder testingBuilder = new RunMatlabTestsBuilder();
        testingBuilder.setPdfReportArtifact(new RunMatlabTestsBuilder.PdfArtifact("abc/xyz"));
        project.getBuildersList().add(testingBuilder);
        FreeStyleBuild build = project.scheduleBuild2(0).get();
        jenkins.assertBuildStatus(Result.FAILURE, build);
        jenkins.assertLogContains("Expected '.pdf'", build);
    }

    @Test
    public void verifyBuildFailsForInvalidFilename() throws Exception {
        Assume.assumeTrue
                (System.getProperty("os.name").toLowerCase().startsWith("windows"));
        this.buildWrapper.setMatlabBuildWrapperContent(new MatlabBuildWrapperContent(Message.getValue("matlab.custom.location"), MatlabRootSetup.getMatlabRoot()));
//        this.buildWrapper.setMatlabRootFolder(getMatlabroot());
        project.getBuildWrappersList().add(this.buildWrapper);
        RunMatlabTestsBuilder testingBuilder = new RunMatlabTestsBuilder();
        testingBuilder.setPdfReportArtifact(new RunMatlabTestsBuilder.PdfArtifact("abc/?xyz.pdf"));
        project.getBuildersList().add(testingBuilder);
        FreeStyleBuild build = project.scheduleBuild2(0).get();
        jenkins.assertBuildStatus(Result.FAILURE, build);
        jenkins.assertLogContains("Invalid argument", build);
        jenkins.assertLogContains("Unable to write to file", build);
    }

    /*
     * Test to verify if Matrix build fails when MATLAB is not available.
     */
    @Test
    public void verifyMatrixBuildFails() throws Exception {
        MatrixProject matrixProject = jenkins.createProject(MatrixProject.class);
        Axis axes = new Axis("VERSION", "R2018a", "R2018b");
        matrixProject.setAxes(new AxisList(axes));
        String matlabRoot = TestData.getPropValues("matlab.invalid.root.path") + "VERSION";;
        this.buildWrapper.setMatlabBuildWrapperContent(new MatlabBuildWrapperContent(Message.getValue("matlab.custom.location"), matlabRoot));
//        this.buildWrapper.setMatlabRootFolder(matlabRoot.replace(TestData.getPropValues("matlab.version"), "$VERSION"));
        matrixProject.getBuildWrappersList().add(this.buildWrapper);


        matrixProject.getBuildersList().add(testBuilder);

        // Check for first matrix combination.

        Map<String, String> vals = new HashMap<String, String>();
        vals.put("VERSION", "R2018a");
        Combination c1 = new Combination(vals);
        MatrixRun build1 = matrixProject.scheduleBuild2(0).get().getRun(c1);


        jenkins.assertBuildStatus(Result.FAILURE, build1);

        // Check for second Matrix combination

        Combination c2 = new Combination(vals);
        MatrixRun build2 = matrixProject.scheduleBuild2(0).get().getRun(c2);

        jenkins.assertBuildStatus(Result.FAILURE, build2);
    }
    
    /*
     * Test to verify if Matrix build passes (mock MATLAB).
     */
    @Test
    public void verifyMatrixBuildPasses() throws Exception {
        MatrixProject matrixProject = jenkins.createProject(MatrixProject.class);
        Axis axes = new Axis("VERSION", TestData.getPropValues("matlab.version"), TestData.getPropValues("matlab.matrix.version"));
        matrixProject.setAxes(new AxisList(axes));
        String matlabRoot = MatlabRootSetup.getMatlabRoot();
        this.buildWrapper.setMatlabBuildWrapperContent(new MatlabBuildWrapperContent(TestData.getPropValues("matlab.custom.location"), matlabRoot.replace(TestData.getPropValues("matlab.version"), "$VERSION")));
        matrixProject.getBuildWrappersList().add(this.buildWrapper);

        testBuilder.setOutputDetail("None");
        testBuilder.setLoggingLevel("None");
        testBuilder.setStrict(true);
        testBuilder.setUseParallel(true);

        matrixProject.getBuildersList().add(testBuilder);
        MatrixBuild build = matrixProject.scheduleBuild2(0).get();
        List<MatrixRun> runs = build.getRuns();
        
        for (MatrixRun run : runs) {
            jenkins.assertLogContains("LoggingLevel', 0", run);
            jenkins.assertLogContains("OutputDetail', 0", run);
            jenkins.assertLogContains("FailOnWarningsPlugin", run);
            jenkins.assertLogContains("runInParallel", run);
        }


        jenkins.assertLogContains(TestData.getPropValues("matlab.version")+" completed", build);
        jenkins.assertLogContains(TestData.getPropValues("matlab.matrix.version")+" completed", build);
    }

    /*
     * Test to verify if tests are filtered bu   and by folder path
     */
    @Test
    public void verifyTestsFilterByFolderAndTag() throws Exception {
        this.buildWrapper.setMatlabBuildWrapperContent(new MatlabBuildWrapperContent(Message.getValue("matlab.custom.location"), MatlabRootSetup.getMatlabRoot()));
        project.getBuildWrappersList().add(this.buildWrapper);
        project.setScm(new ExtractResourceSCM(MatlabRootSetup.getRunMATLABTestsData()));

        RunMatlabTestsBuilder testingBuilder = new RunMatlabTestsBuilder();
        testingBuilder.setLoggingLevel("None");
        testingBuilder.setOutputDetail("None");
        testingBuilder.setUseParallel(true);
        testingBuilder.setStrict(true);
        // Adding list of source folder
        List<SourceFolderPaths> list=new ArrayList<SourceFolderPaths>();
        list.add(new SourceFolderPaths("src"));
        testingBuilder.setSourceFolder(new SourceFolder(list));

        // Adding list of test folder
        List<TestFolders> testFolders = new ArrayList<TestFolders>();
        testFolders.add(new TestFolders("test/TestSquare"));
        testingBuilder.setSelectByFolder(new SelectByFolder(testFolders));

        //Adding test tag
        testingBuilder.setSelectByTag(new RunMatlabTestsBuilder.SelectByTag("TestTag"));
        project.getBuildersList().add(testingBuilder);
        FreeStyleBuild build = project.scheduleBuild2(0).get();
        String build_log  = build.getLog();
        jenkins.assertBuildStatus(Result.SUCCESS, build);
        jenkins.assertLogContains("addpath(genpath('src'));", build);
        jenkins.assertLogContains("testSquare", build);
    }

    @Test
    public void verifyTestFilterWithSourceSelection() throws Exception {
        this.buildWrapper.setMatlabBuildWrapperContent(new MatlabBuildWrapperContent(Message.getValue("matlab.custom.location"), MatlabRootSetup.getMatlabRoot()));
        project.getBuildWrappersList().add(this.buildWrapper);
        project.setScm(new ExtractResourceSCM(MatlabRootSetup.getRunMATLABTestsData()));

        RunMatlabTestsBuilder testingBuilder = new RunMatlabTestsBuilder();
        // Adding list of source folder
        List<SourceFolderPaths> list=new ArrayList<SourceFolderPaths>();
        list.add(new SourceFolderPaths("src/multiplySrc"));
        testingBuilder.setSourceFolder(new SourceFolder(list));

        // Adding list of test folder
        List<TestFolders> testFolders = new ArrayList<TestFolders>();
        testFolders.add(new TestFolders("test/TestMultiply"));
        testingBuilder.setSelectByFolder(new SelectByFolder(testFolders));
        project.getBuildersList().add(testingBuilder);
        FreeStyleBuild build = project.scheduleBuild2(0).get();
        String build_log  = build.getLog();
        jenkins.assertBuildStatus(Result.SUCCESS, build);
        jenkins.assertLogContains("addpath(genpath('src/multiplySrc'));", build);
        jenkins.assertLogContains("Done testMultiply", build);
    }

    @Test
    public void verifyNoTestsAreRunForIncorrectTestFolderPath() throws Exception{
        this.buildWrapper.setMatlabBuildWrapperContent(new MatlabBuildWrapperContent(Message.getValue("matlab.custom.location"), MatlabRootSetup.getMatlabRoot()));
        project.getBuildWrappersList().add(this.buildWrapper);

        //set SCM
        project.setScm(new ExtractResourceSCM(MatlabRootSetup.getRunMATLABTestsData()));
        RunMatlabTestsBuilder testingBuilder = new RunMatlabTestsBuilder();

        // Adding list of source folder
        List<SourceFolderPaths> list=new ArrayList<SourceFolderPaths>();
        list.add(new SourceFolderPaths("src"));
        testingBuilder.setSourceFolder(new SourceFolder(list));

        // Adding list of test folder
        List<TestFolders> testFolders = new ArrayList<TestFolders>();
        testFolders.add(new TestFolders("test/incorrect/path"));
        testingBuilder.setSelectByFolder(new SelectByFolder(testFolders));
        project.getBuildersList().add(testingBuilder);
        FreeStyleBuild build = project.scheduleBuild2(0).get();
        String build_log  = build.getLog();
        jenkins.assertBuildStatus(Result.SUCCESS, build);
        jenkins.assertLogNotContains("Done", build);
    }

    @Test
    public void verifyDependentTestsFail() throws Exception {
        this.buildWrapper.setMatlabBuildWrapperContent(new MatlabBuildWrapperContent(Message.getValue("matlab.custom.location"), MatlabRootSetup.getMatlabRoot()));
        project.getBuildWrappersList().add(this.buildWrapper);
        project.setScm(new ExtractResourceSCM(MatlabRootSetup.getRunMATLABTestsData()));
        RunMatlabTestsBuilder testingBuilder = new RunMatlabTestsBuilder();

        // Adding list of test folder
        List<TestFolders> testFolders = new ArrayList<TestFolders>();
        testFolders.add(new TestFolders("test/TestMultiply"));
        testingBuilder.setSelectByFolder(new SelectByFolder(testFolders));
        project.getBuildersList().add(testingBuilder);
        FreeStyleBuild build = project.scheduleBuild2(0).get();
        String build_log  = build.getLog();
        jenkins.assertBuildStatus(Result.FAILURE, build);
        jenkins.assertLogContains("testMultiplication",build);
    }

//    @Test
//    public void verifyArtifactsContainsFilteredTests() throws Exception{
//        this.buildWrapper.setMatlabBuildWrapperContent(new MatlabBuildWrapperContent(Message.getValue("matlab.custom.location"), getMatlabroot()));
//        project.getBuildWrappersList().add(this.buildWrapper);
//        project.setScm(get_GitSCM());
//        RunMatlabTestsBuilder testingBuilder = new RunMatlabTestsBuilder();
//
//    }

    @Test
    public void verifyCodeCoverageResultForFreeStyle() throws Exception {
        this.buildWrapper.setMatlabBuildWrapperContent(new MatlabBuildWrapperContent(Message.getValue("matlab.custom.location"), MatlabRootSetup.getMatlabRoot()));
        project.getBuildWrappersList().add(this.buildWrapper);

        project.setScm(new ExtractResourceSCM(MatlabRootSetup.getRunMATLABTestsData()));

        RunMatlabTestsBuilder testingBuilder = new RunMatlabTestsBuilder();
        testingBuilder.setCoberturaArtifact(new RunMatlabTestsBuilder.CoberturaArtifact("TestArtifacts/coberturaresult.xml"));
        project.getBuildersList().add(testingBuilder);

        FreeStyleBuild build = project.scheduleBuild2(0).get();

        String xmlString = xmlToString(build.getWorkspace() + "/TestArtifacts/coberturaresult.xml");
        assertFalse(xmlString.contains("+scriptgen"));
        assertFalse(xmlString.contains("genscript"));
        assertFalse(xmlString.contains("runner_"));
        jenkins.assertLogContains("testSquare", build);
        jenkins.assertBuildStatus(Result.FAILURE,build);
    }

    @Test
    public void verifyCodeCoverageResultForMatrix() throws Exception {
        MatrixProject matrixProject = jenkins.createProject(MatrixProject.class);
        Axis axes = new Axis("VERSION", TestData.getPropValues("matlab.version"), TestData.getPropValues("matlab.matrix.version"));
        matrixProject.setAxes(new AxisList(axes));
        String matlabRoot = MatlabRootSetup.getMatlabRoot();
        this.buildWrapper.setMatlabBuildWrapperContent(new MatlabBuildWrapperContent(Message.getValue("matlab.custom.location"), matlabRoot.replace(TestData.getPropValues("matlab.version"), "$VERSION")));

        matrixProject.setScm(new ExtractResourceSCM(MatlabRootSetup.getRunMATLABTestsData()));
        matrixProject.getBuildWrappersList().add(this.buildWrapper);
        RunMatlabTestsBuilder tester = new RunMatlabTestsBuilder();

        tester.setCoberturaArtifact(new RunMatlabTestsBuilder.CoberturaArtifact("TestArtifacts/coberturaresult.xml"));
        // Adding list of test folder
        List<TestFolders> testFolders = new ArrayList<TestFolders>();
        testFolders.add(new TestFolders("test/TestSum"));
        tester.setSelectByFolder(new SelectByFolder(testFolders));

        matrixProject.getBuildersList().add(tester);
        MatrixBuild build = matrixProject.scheduleBuild2(0).get();

        Map<String, String> vals = new HashMap<String, String>();
        vals.put("VERSION", TestData.getPropValues("matlab.version"));
        Combination c1 = new Combination(vals);
        MatrixRun build1 = build.getRun(c1);

        String xmlString = xmlToString(build1.getWorkspace().getRemote() + "/TestArtifacts/coberturaresult.xml");
        assertFalse(xmlString.contains("+scriptgen"));
        assertFalse(xmlString.contains("genscript"));
        assertFalse(xmlString.contains("runner_"));
        jenkins.assertLogContains("testSum", build1);
        jenkins.assertBuildStatus(Result.SUCCESS,build1);

        // Check for second Matrix combination
        vals.put("VERSION", TestData.getPropValues("matlab.matrix.version"));
        Combination c2 = new Combination(vals);
        MatrixRun build2 = build.getRun(c2);

        xmlString = xmlToString(build2.getWorkspace().getRemote() + "/TestArtifacts/coberturaresult.xml");
        assertFalse(xmlString.contains("+scriptgen"));
        assertFalse(xmlString.contains("genscript"));
        assertFalse(xmlString.contains("runner_"));
        jenkins.assertLogContains("testSum", build2);
        jenkins.assertBuildStatus(Result.SUCCESS, build2);
    }

    @Test
    public void verifyLoggingLevelSetToNone() throws Exception {

        this.buildWrapper.setMatlabBuildWrapperContent(new MatlabBuildWrapperContent(
                Message.getValue("matlab.custom.location"), MatlabRootSetup.getMatlabRoot()));
        project.getBuildWrappersList().add(this.buildWrapper);
        testBuilder.setLoggingLevel("None");
        project.getBuildersList().add(this.testBuilder);
        FreeStyleBuild build;
        build = project.scheduleBuild2(0).get();
        jenkins.assertLogContains("LoggingLevel', 0", build);

    }

    @Test
    public void verifyLoggingLevelSetToTerse() throws Exception {

        this.buildWrapper.setMatlabBuildWrapperContent(new MatlabBuildWrapperContent(
                Message.getValue("matlab.custom.location"), MatlabRootSetup.getMatlabRoot()));
        project.getBuildWrappersList().add(this.buildWrapper);
        testBuilder.setLoggingLevel("Terse");
        project.getBuildersList().add(this.testBuilder);
        FreeStyleBuild build;
        build = project.scheduleBuild2(0).get();
        jenkins.assertLogContains("LoggingLevel', 1", build);

    }

    @Test
    public void verifyLoggingLevelSetToConcise() throws Exception {

        this.buildWrapper.setMatlabBuildWrapperContent(new MatlabBuildWrapperContent(
                Message.getValue("matlab.custom.location"), MatlabRootSetup.getMatlabRoot()));
        project.getBuildWrappersList().add(this.buildWrapper);
        testBuilder.setLoggingLevel("Concise");
        project.getBuildersList().add(this.testBuilder);
        FreeStyleBuild build;
        build = project.scheduleBuild2(0).get();
        jenkins.assertLogContains("LoggingLevel', 2", build);

    }

    @Test
    public void verifyLoggingLevelSetToDetailed() throws Exception {

        this.buildWrapper.setMatlabBuildWrapperContent(new MatlabBuildWrapperContent(
                Message.getValue("matlab.custom.location"), MatlabRootSetup.getMatlabRoot()));
        project.getBuildWrappersList().add(this.buildWrapper);
        testBuilder.setLoggingLevel("Detailed");
        project.getBuildersList().add(this.testBuilder);
        FreeStyleBuild build;
        build = project.scheduleBuild2(0).get();
        jenkins.assertLogContains("LoggingLevel', 3", build);

    }

    /*@Integ
     * Test To verify if Output Detail  is set correctly
     *
     */

    @Test
    public void verifyOutputDetailSetToNone() throws Exception {
        this.buildWrapper.setMatlabBuildWrapperContent(new MatlabBuildWrapperContent(
                Message.getValue("matlab.custom.location"), MatlabRootSetup.getMatlabRoot()));
        project.getBuildWrappersList().add(this.buildWrapper);
        testBuilder.setLoggingLevel("None");

        testBuilder.setOutputDetail("None");
        project.getBuildersList().add(this.testBuilder);
        FreeStyleBuild build;
        build = project.scheduleBuild2(0).get();
        jenkins.assertLogContains("'OutputDetail', 0", build);
    }

    @Test
    public void verifyOutputDetailSetToTerse() throws Exception {
        this.buildWrapper.setMatlabBuildWrapperContent(new MatlabBuildWrapperContent(
                Message.getValue("matlab.custom.location"), MatlabRootSetup.getMatlabRoot()));
        project.getBuildWrappersList().add(this.buildWrapper);
        testBuilder.setLoggingLevel("None");

        testBuilder.setOutputDetail("Terse");
        project.getBuildersList().add(this.testBuilder);
        FreeStyleBuild build;
        build = project.scheduleBuild2(0).get();
        jenkins.assertLogContains("'OutputDetail', 1", build);
    }

    @Test
    public void verifyOutputDetailSetToConcise() throws Exception {
        this.buildWrapper.setMatlabBuildWrapperContent(new MatlabBuildWrapperContent(
                Message.getValue("matlab.custom.location"), MatlabRootSetup.getMatlabRoot()));
        project.getBuildWrappersList().add(this.buildWrapper);
        testBuilder.setLoggingLevel("None");

        testBuilder.setOutputDetail("Concise");
        project.getBuildersList().add(this.testBuilder);
        FreeStyleBuild build;
        build = project.scheduleBuild2(0).get();
        jenkins.assertLogContains("'OutputDetail', 2", build);
    }

    @Test
    public void verifyOutputDetailSetToDetailed() throws Exception {
        this.buildWrapper.setMatlabBuildWrapperContent(new MatlabBuildWrapperContent(
                Message.getValue("matlab.custom.location"), MatlabRootSetup.getMatlabRoot()));
        project.getBuildWrappersList().add(this.buildWrapper);
        testBuilder.setLoggingLevel("None");

        testBuilder.setOutputDetail("Detailed");
        project.getBuildersList().add(this.testBuilder);
        FreeStyleBuild build;
        build = project.scheduleBuild2(0).get();
        jenkins.assertLogContains("'OutputDetail', 3", build);
    }


    /*@Integ
     * Test To verify when Strict option set
     *
     */

    @Test
    public void verifyStrictSet() throws Exception {
        this.buildWrapper.setMatlabBuildWrapperContent(new MatlabBuildWrapperContent(
                Message.getValue("matlab.custom.location"), MatlabRootSetup.getMatlabRoot()));
        project.getBuildWrappersList().add(this.buildWrapper);
        testBuilder.setLoggingLevel("None");
        testBuilder.setStrict(true);
        project.getBuildersList().add(this.testBuilder);
        FreeStyleBuild build = project.scheduleBuild2(0).get();
        jenkins.assertLogContains("FailOnWarningsPlugin", build);

    }

    /*@Integ
     * Test To verify when Strict option not set
     *
     */

    @Test
    public void verifyStrictNotSet() throws Exception {
        this.buildWrapper.setMatlabBuildWrapperContent(new MatlabBuildWrapperContent(
                Message.getValue("matlab.custom.location"), MatlabRootSetup.getMatlabRoot()));
        project.getBuildWrappersList().add(this.buildWrapper);
        testBuilder.setLoggingLevel("None");
        testBuilder.setStrict(false);
        project.getBuildersList().add(this.testBuilder);
        FreeStyleBuild build = project.scheduleBuild2(0).get();
        jenkins.assertLogNotContains("FailOnWarningsPlugin", build);

    }

    /*@Integ
     * Test To verify when Run in Parallel option is set
     *
     */

    @Test
    public void verifyRunParallelSet() throws Exception {
         this.buildWrapper.setMatlabBuildWrapperContent(new MatlabBuildWrapperContent(
                 Message.getValue("matlab.custom.location"), MatlabRootSetup.getMatlabRoot()));
         project.getBuildWrappersList().add(this.buildWrapper);
        testBuilder.setLoggingLevel("None");
        testBuilder.setUseParallel(true);
        project.getBuildersList().add(this.testBuilder);
        FreeStyleBuild build = project.scheduleBuild2(0).get();
        jenkins.assertLogContains("runInParallel", build);
    }

//    @Test
//    public void junitversion(){
//        System.out.println(Version.id());
//    }

    /*@Integ
     * Test To verify when Run in Parallel option is set
     *
     */

    @Test
    public void verifyRunParallelNotSet() throws Exception {
        this.buildWrapper.setMatlabBuildWrapperContent(new MatlabBuildWrapperContent(
                Message.getValue("matlab.custom.location"), MatlabRootSetup.getMatlabRoot()));
        project.getBuildWrappersList().add(this.buildWrapper);
        testBuilder.setLoggingLevel("None");
        testBuilder.setUseParallel(false);
        project.getBuildersList().add(this.testBuilder);
        FreeStyleBuild build = project.scheduleBuild2(0).get();
        jenkins.assertLogNotContains("runInParallel", build);
    }

    @Test
    public void testWithWarningFailsWithStrict() throws Exception{
        this.buildWrapper.setMatlabBuildWrapperContent(new MatlabBuildWrapperContent(
                Message.getValue("matlab.custom.location"), MatlabRootSetup.getMatlabRoot()));
        project.getBuildWrappersList().add(this.buildWrapper);
        project.setScm(new ExtractResourceSCM(MatlabRootSetup.getTestOnWarningData()));
        testBuilder.setStrict(true);

        List<SourceFolderPaths> list=new ArrayList<SourceFolderPaths>();
        list.add(new SourceFolderPaths("src"));
        testBuilder.setSourceFolder(new SourceFolder(list));
        project.getBuildersList().add(this.testBuilder);
        FreeStyleBuild build = project.scheduleBuild2(0).get();
        jenkins.assertLogContains("FailOnWarningsPlugin", build);
        jenkins.assertBuildStatus(Result.FAILURE, build);
    }


}
