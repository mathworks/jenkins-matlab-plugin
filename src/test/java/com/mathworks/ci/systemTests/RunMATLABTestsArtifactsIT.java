package com.mathworks.ci.systemTests;

import com.jcabi.xml.XML;
import com.jcabi.xml.XMLDocument;
import com.mathworks.ci.MatlabBuildWrapperContent;
import com.mathworks.ci.Message;
import com.mathworks.ci.UseMatlabVersionBuildWrapper;
import com.mathworks.ci.freestyle.RunMatlabTestsBuilder;
import com.mathworks.ci.freestyle.options.SourceFolder;
import com.mathworks.ci.freestyle.options.SourceFolderPaths;
import hudson.FilePath;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import org.htmlunit.html.HtmlCheckBoxInput;
import org.htmlunit.html.HtmlPage;
import org.htmlunit.html.HtmlTextInput;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.*;
import org.junit.rules.Timeout;
import org.jvnet.hudson.test.ExtractResourceSCM;
import org.jvnet.hudson.test.JenkinsRule;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class RunMATLABTestsArtifactsIT {
    private FreeStyleProject project;
    private UseMatlabVersionBuildWrapper buildWrapper;
    private RunMatlabTestsBuilder testBuilder;

    @Rule
    public Timeout timeout = Timeout.seconds(0);

    @Rule
    public JenkinsRule jenkins = new JenkinsRule();

    @BeforeClass
    public static void checkMatlabRoot() {
        // Check if the MATLAB_ROOT environment variable is defined
        String matlabRoot = System.getenv("MATLAB_ROOT");
        Assume.assumeTrue("Not running tests as MATLAB_ROOT environment variable is not defined", matlabRoot != null && !matlabRoot.isEmpty());
    }

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

    @Test
    public void verifyJUnitFilePathInput() throws Exception{
        this.buildWrapper.setMatlabBuildWrapperContent(new MatlabBuildWrapperContent(Message.getValue("matlab.custom.location"), MatlabRootSetup.getMatlabRoot()));

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
        this.buildWrapper.setMatlabBuildWrapperContent(new MatlabBuildWrapperContent(Message.getValue("matlab.custom.location"), MatlabRootSetup.getMatlabRoot()));
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
        this.buildWrapper.setMatlabBuildWrapperContent(new MatlabBuildWrapperContent(Message.getValue("matlab.custom.location"), MatlabRootSetup.getMatlabRoot()));
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
        this.buildWrapper.setMatlabBuildWrapperContent(new MatlabBuildWrapperContent(Message.getValue("matlab.custom.location"), MatlabRootSetup.getMatlabRoot()));
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
        this.buildWrapper.setMatlabBuildWrapperContent(new MatlabBuildWrapperContent(Message.getValue("matlab.custom.location"), MatlabRootSetup.getMatlabRoot()));
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
    public void verifyCustomFilePathInputForArtifacts() throws Exception{
        this.buildWrapper.setMatlabBuildWrapperContent(new MatlabBuildWrapperContent(Message.getValue("matlab.custom.location"), MatlabRootSetup.getMatlabRoot()));
        project.getBuildWrappersList().add(this.buildWrapper);

        project.setScm(new ExtractResourceSCM(MatlabRootSetup.getRunMATLABTestsData()));


        RunMatlabTestsBuilder testingBuilder = new RunMatlabTestsBuilder();
        // Adding list of source folder
        List<SourceFolderPaths> list=new ArrayList<SourceFolderPaths>();
        list.add(new SourceFolderPaths("src"));
        testingBuilder.setSourceFolder(new SourceFolder(list));
        // Generate artifacts with custom inputs
        testingBuilder.setPdfReportArtifact(new RunMatlabTestsBuilder.PdfArtifact("TestArtifacts/pdfReport.pdf"));
        testingBuilder.setTapArtifact(new RunMatlabTestsBuilder.TapArtifact("TestArtifacts/tapResult.tap"));
        testingBuilder.setJunitArtifact(new RunMatlabTestsBuilder.JunitArtifact("TestArtifacts/junittestreport.xml"));
        testingBuilder.setStmResultsArtifact(new RunMatlabTestsBuilder.StmResultsArtifact("TestArtifacts/stmresult.mldatx"));
        testingBuilder.setCoberturaArtifact(new RunMatlabTestsBuilder.CoberturaArtifact("TestArtifacts/coberturaresult.xml"));
        testingBuilder.setModelCoverageArtifact(new RunMatlabTestsBuilder.ModelCovArtifact("TestArtifacts/mdlCovReport.xml"));

        project.getBuildersList().add(testingBuilder);
        FreeStyleBuild build = project.scheduleBuild2(0).get();

        jenkins.assertBuildStatus(Result.SUCCESS, build);
        jenkins.assertLogContains("TestArtifacts/stmresult.mldatx", build);
        jenkins.assertLogContains("TestArtifacts/mdlCovReport.xml", build);

        assertTrue(new FilePath(jenkins.getInstance().getWorkspaceFor(project), "TestArtifacts/pdfReport.pdf").exists());
        assertTrue(new FilePath(jenkins.getInstance().getWorkspaceFor(project), "TestArtifacts/tapResult.tap").exists());
        assertTrue(new FilePath(jenkins.getInstance().getWorkspaceFor(project), "TestArtifacts/junittestreport.xml").exists());
        assertTrue(new FilePath(jenkins.getInstance().getWorkspaceFor(project), "TestArtifacts/coberturaresult.xml").exists());
    }

    @Test
    public void verifyCustomeFilenamesForArtifactsPipeline() throws Exception {
        String script = "pipeline {\n" +
                "  agent any\n" +
                MatlabRootSetup.getEnvironmentDSL() + "\n" +
                "    stages{\n" +
                "        stage('Run MATLAB Command') {\n" +
                "            steps\n" +
                "            {\n" +
                "                unzip '" + MatlabRootSetup.getRunMATLABTestsData().getPath() + "'" + "\n" +
                "              runMATLABTests(sourceFolder:['src'], testResultsTAP: 'test-results/results.tap',\n" +
                "                             testResultsJUnit: 'test-results/results.xml',\n" +
                "                             testResultsSimulinkTest: 'test-results/results.mldatx',\n" +
                "                             codeCoverageCobertura: 'code-coverage/coverage.xml',\n" +
                "                             modelCoverageCobertura: 'model-coverage/coverage.xml')\n" +
                "            }\n" +
                "        }\n" +
                "    }\n" +
                "}";
        WorkflowRun build = getPipelineBuild(script);
        jenkins.assertBuildStatus(Result.SUCCESS,build);
    }

    @Test
    public void verifyCOverageReportDoesNotIncludeOtherSourceFolder() throws Exception {
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

    private WorkflowRun getPipelineBuild(String script) throws Exception{
        WorkflowJob project = jenkins.createProject(WorkflowJob.class);
        project.setDefinition(new CpsFlowDefinition(script,true));
        return project.scheduleBuild2(0).get();
    }

}
