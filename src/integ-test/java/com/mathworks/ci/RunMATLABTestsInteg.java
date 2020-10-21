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
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.*;


public class RunMATLABTestsInteg {

    private FreeStyleProject project;
    private UseMatlabVersionBuildWrapper buildWrapper;
    private RunMatlabTestsBuilder testBuilder;


    @Rule
    public JenkinsRule jenkins = new JenkinsRule();

    @Before
    public void testSetup() throws IOException {

        this.project = jenkins.createFreeStyleProject();
        this.testBuilder = new RunMatlabTestsBuilder();
        this.buildWrapper = new UseMatlabVersionBuildWrapper();
    }

    @After
    public void testTearDown() {
        this.project = null;
        this.testBuilder = null;
    }

    private String getMatlabroot() throws URISyntaxException {
        String ML_version = TestData.getPropValues("matlab.version");
        String installed_path, MATLAB_ROOT;

        if (System.getProperty("os.name").startsWith("Win")) {
            installed_path = TestData.getPropValues("matlab.windows.installed.path");
            MATLAB_ROOT = installed_path + "\\" + ML_version;
            // Prints the root folder of MATLAB
            System.out.println(MATLAB_ROOT);
        }
        else{
            installed_path = TestData.getPropValues("matlab.linux.installed.path");
            MATLAB_ROOT = installed_path + "/" + ML_version;
            // Prints the root folder of MATLAB
            System.out.println(MATLAB_ROOT);
        }
        return MATLAB_ROOT;
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
        this.buildWrapper.setMatlabRootFolder(TestData.getPropValues("matlab.invalid.root.path"));
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
        this.buildWrapper.setMatlabRootFolder("fake");
        project.getBuildWrappersList().add(this.buildWrapper);
        project.getBuildersList().add(this.testBuilder);
        RunMatlabCommandBuilder tester =
                new RunMatlabCommandBuilder();
        project.getBuildersList().add(tester);

        FreeStyleBuild build = project.scheduleBuild2(0).get();
        jenkins.assertBuildStatus(Result.FAILURE, build);
    }

    @Test
    public void verifyJUnitFilePathInput() throws Exception{
        this.buildWrapper.setMatlabRootFolder(getMatlabroot());
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
        this.buildWrapper.setMatlabRootFolder(getMatlabroot());
        project.getBuildWrappersList().add(this.buildWrapper);
        project.getBuildersList().add(this.testBuilder);
        HtmlPage page = jenkins.createWebClient().goTo("job/test0/configure");
        HtmlCheckBoxInput tapChkbx = page.getElementByName("tapArtifact");
        tapChkbx.setChecked(true);
        Thread.sleep(2000);
        HtmlTextInput tapFilePathInput=(HtmlTextInput) page.getElementByName("_.tapReportFilePath");
        Assert.assertEquals(TestData.getPropValues("taptestresult.file.path"),tapFilePathInput.getValueAttribute());

    }

    @Test
    public void verifyPDFReportFilePathInput() throws Exception{
        this.buildWrapper.setMatlabRootFolder(getMatlabroot());
        project.getBuildWrappersList().add(this.buildWrapper);
        project.getBuildersList().add(this.testBuilder);
        HtmlPage page = jenkins.createWebClient().goTo("job/test0/configure");
        HtmlCheckBoxInput pdfChkbx = page.getElementByName("pdfReportArtifact");
        pdfChkbx.setChecked(true);
        Thread.sleep(2000);
        HtmlTextInput PDFFilePathInput=(HtmlTextInput) page.getElementByName("_.pdfReportFilePath");
        Assert.assertEquals(TestData.getPropValues("pdftestreport.file.path"),PDFFilePathInput.getValueAttribute());
    }

    @Test
    public void verifyCoberturaFilePathInput() throws Exception {
        this.buildWrapper.setMatlabRootFolder(getMatlabroot());
        project.getBuildWrappersList().add(this.buildWrapper);
        project.getBuildersList().add(this.testBuilder);
        HtmlPage page = jenkins.createWebClient().goTo("job/test0/configure");
        HtmlCheckBoxInput coberturaChkBx = page.getElementByName("coberturaArtifact");
        coberturaChkBx.setChecked(true);
        Thread.sleep(2000);
        HtmlTextInput coberturaCodeCoverageFileInput=(HtmlTextInput) page.getElementByName("_.coberturaReportFilePath");
        Assert.assertEquals(TestData.getPropValues("cobertura.file.path"),coberturaCodeCoverageFileInput.getValueAttribute());
    }


    @Test
    public void verifyModelCoverageFilePathInput() throws Exception {
        this.buildWrapper.setMatlabRootFolder(getMatlabroot());
        project.getBuildWrappersList().add(this.buildWrapper);
        project.getBuildersList().add(this.testBuilder);
        HtmlPage page = jenkins.createWebClient().goTo("job/test0/configure");
        HtmlCheckBoxInput modelCoverageChkBx = page.getElementByName("modelCoverageArtifact");
        modelCoverageChkBx.setChecked(true);
        Thread.sleep(2000);
        HtmlTextInput coberturaModelCoverageFileInput=(HtmlTextInput) page.getElementByName("_.modelCoverageFilePath");
        Assert.assertEquals(TestData.getPropValues("modelcoverage.file.path"),coberturaModelCoverageFileInput.getValueAttribute());
    }


    @Test
    public void verifySTMResultsFilePathInput() throws Exception {
        this.buildWrapper.setMatlabRootFolder(getMatlabroot());
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
        this.buildWrapper.setMatlabRootFolder(getMatlabroot());
        project.getBuildWrappersList().add(this.buildWrapper);
        RunMatlabTestsBuilder testingBuilder = new RunMatlabTestsBuilder();
        testingBuilder.setPdfReportArtifact(new RunMatlabTestsBuilder.PdfArtifact("abc/xyz.pdf"));
        project.getBuildersList().add(testingBuilder);
        FreeStyleBuild build = project.scheduleBuild2(0).get();
        String build_log = jenkins.getLog(build);
        jenkins.assertBuildStatus(Result.SUCCESS, build);
        jenkins.assertLogContains("abc/xyz", build);
    }

    @Test
    public void verifyCustomFilePathInputForArtifacts() throws Exception{
        this.buildWrapper.setMatlabRootFolder(getMatlabroot());
        project.getBuildWrappersList().add(this.buildWrapper);
        RunMatlabTestsBuilder testingBuilder = new RunMatlabTestsBuilder();
        testingBuilder.setJunitArtifact(new RunMatlabTestsBuilder.JunitArtifact("TestArtifacts/junittestreport.xml"));
        testingBuilder.setTapArtifact(new RunMatlabTestsBuilder.TapArtifact("TestArtifacts/tapResult.xml"));
        testingBuilder.setCoberturaArtifact(new RunMatlabTestsBuilder.CoberturaArtifact("TestArtifacts/coberturaresult.xml"));
        testingBuilder.setStmResultsArtifact(new RunMatlabTestsBuilder.StmResultsArtifact("TestArtifacts/stmresult.xml"));
        testingBuilder.setModelCoverageArtifact(new RunMatlabTestsBuilder.ModelCovArtifact("TestArtifacts/mdlCovReport.xml"));
        project.getBuildersList().add(testingBuilder);
        FreeStyleBuild build = project.scheduleBuild2(0).get();
        String build_log = jenkins.getLog(build);
        jenkins.assertBuildStatus(Result.SUCCESS, build);
        jenkins.assertLogContains("TestArtifacts/junittestreport.xml", build);
        jenkins.assertLogContains("TestArtifacts/tapResult.xml", build);
        jenkins.assertLogContains("TestArtifacts/coberturaresult.xml", build);
        jenkins.assertLogContains("TestArtifacts/stmresult.xml", build);
        jenkins.assertLogContains("TestArtifacts/mdlCovReport.xml", build);
    }

    @Test
    public void verifyExtForPdfReport() throws Exception {
        this.buildWrapper.setMatlabRootFolder(getMatlabroot());
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
        this.buildWrapper.setMatlabRootFolder(getMatlabroot());
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
        String matlabRoot = getMatlabroot();
        this.buildWrapper.setMatlabRootFolder(matlabRoot.replace(TestData.getPropValues("matlab.version"), "$VERSION"));
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
        Axis axes = new Axis("VERSION", "R2019b", "R2020a");
        matrixProject.setAxes(new AxisList(axes));
        String matlabRoot = getMatlabroot();
        this.buildWrapper.setMatlabRootFolder(matlabRoot.replace(TestData.getPropValues("matlab.version"), "$VERSION"));
        matrixProject.getBuildWrappersList().add(this.buildWrapper);
        RunMatlabTestsBuilder tester = new RunMatlabTestsBuilder();

        matrixProject.getBuildersList().add(tester);
        MatrixBuild build = matrixProject.scheduleBuild2(0).get();

        jenkins.assertLogContains("Triggering", build);
        jenkins.assertLogContains("R2019b completed", build);
        jenkins.assertLogContains("R2020a completed", build);
        jenkins.assertBuildStatus(Result.SUCCESS, build);
    }


}