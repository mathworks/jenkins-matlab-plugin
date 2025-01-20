package com.mathworks.ci.systemTests;

import com.mathworks.ci.*;
import com.mathworks.ci.freestyle.RunMatlabTestsBuilder;
import com.mathworks.ci.freestyle.options.SelectByFolder;
import com.mathworks.ci.freestyle.options.SourceFolder;
import com.mathworks.ci.freestyle.options.SourceFolderPaths;
import com.mathworks.ci.freestyle.options.TestFolders;
import hudson.matrix.*;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import hudson.slaves.DumbSlave;
import hudson.tasks.Builder;
import org.htmlunit.WebAssert;
import org.htmlunit.html.HtmlCheckBoxInput;
import org.htmlunit.html.HtmlInput;
import org.htmlunit.html.HtmlPage;
import org.htmlunit.html.HtmlSelect;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.*;
import org.junit.rules.Timeout;
import org.jvnet.hudson.test.ExtractResourceSCM;
import org.jvnet.hudson.test.JenkinsRule;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

import static org.junit.Assert.assertEquals;

public class RunMATLABTestsIT {
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
    public void verifyRunMATLABTestsWithAllInputs() throws Exception {
        this.buildWrapper.setMatlabBuildWrapperContent(new MatlabBuildWrapperContent(Message.getValue("matlab.custom.location"), Utilities.getMatlabRoot()));
        project.getBuildWrappersList().add(this.buildWrapper);
        project.setScm(new ExtractResourceSCM(Utilities.getRunMATLABTestsData()));

        RunMatlabTestsBuilder testingBuilder = new RunMatlabTestsBuilder();
        testingBuilder.setLoggingLevel("None");
        testingBuilder.setOutputDetail("None");

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

        jenkins.assertBuildStatus(Result.SUCCESS, build);
        jenkins.assertLogContains("addpath(genpath('src'));", build);
        // Based on the test folder and tag, 'testSquare' test should be run
        jenkins.assertLogContains("testSquare", build);
        jenkins.assertLogContains("HasTag('TestTag')", build);
    }

    @Test
    public void verifySourceFolderDefaultState() throws Exception {
        project.getBuildersList().add(this.testBuilder);
        HtmlPage page = jenkins.createWebClient().goTo("job/test0/configure");
        HtmlCheckBoxInput sourceFolder = page.getElementByName("_.sourceFolder");
        sourceFolder.click();
        WebAssert.assertElementPresentByXPath(page, "//input[@name=\"_.srcFolderPath\"]");
        HtmlInput srcFolderPath = page.getElementByName("_.srcFolderPath");
        assertEquals("", srcFolderPath.getTextContent());
    }

    @Test
    public void verifySelectByFolderDefaultState() throws Exception {
        project.getBuildersList().add(this.testBuilder);
        HtmlPage page = jenkins.createWebClient().goTo("job/test0/configure");
        HtmlCheckBoxInput sourceFolder = page.getElementByName("_.selectByFolder");
        sourceFolder.click();
        Thread.sleep(2000);
        WebAssert.assertElementPresentByXPath(page, "//input[@name=\"_.testFolders\"]");
        HtmlInput srcFolderPath = page.getElementByName("_.testFolders");
        assertEquals("", srcFolderPath.getTextContent());
    }

    @Test
    public void verifySelectByTagDefaultState() throws Exception {
        project.getBuildersList().add(this.testBuilder);
        HtmlPage page = jenkins.createWebClient().goTo("job/test0/configure");
        HtmlCheckBoxInput sourceFolder = page.getElementByName("_.selectByTag");
        sourceFolder.click();
        Thread.sleep(2000);
        WebAssert.assertElementPresentByXPath(page, "//input[@name=\"_.testTag\"]");
        HtmlInput srcFolderPath = page.getElementByName("_.testTag");
        assertEquals("", srcFolderPath.getTextContent());
    }

    @Test
    public void verifyEmptyParameters() throws Exception {
        this.buildWrapper.setMatlabBuildWrapperContent(
                new MatlabBuildWrapperContent(Message.getValue("matlab.custom.location"), Utilities.getMatlabRoot()));
        project.getBuildWrappersList().add(this.buildWrapper);
        project.getBuildersList().add(this.testBuilder);
        FreeStyleBuild build = project.scheduleBuild2(0).get();
        jenkins.assertLogContains("run-matlab-command", build);
        jenkins.assertLogNotContains("'OutputDetail'", build);
        jenkins.assertLogNotContains("'PDFTestReport'", build);
        jenkins.assertLogNotContains("'Strict'", build);
        jenkins.assertLogNotContains("'SourceFolder'", build);
    }
    @Test
    public void verifyMATLABscratchFileGenerated() throws Exception {
        this.buildWrapper.setMatlabBuildWrapperContent(
                new MatlabBuildWrapperContent(Message.getValue("matlab.custom.location"), Utilities.getMatlabRoot()));
        project.getBuildWrappersList().add(this.buildWrapper);
        project.getBuildersList().add(testBuilder);
        FreeStyleBuild build = project.scheduleBuild2(0).get();
        File matlabRunner = new File(build.getWorkspace() + File.separator + "runnerScript.m");
        Assert.assertFalse(matlabRunner.exists());
    }

    @Test
    public void verifyOutputDetailSetToDefault() throws Exception {
        this.buildWrapper.setMatlabBuildWrapperContent(
                new MatlabBuildWrapperContent(Message.getValue("matlab.custom.location"), Utilities.getMatlabRoot()));
        project.getBuildWrappersList().add(this.buildWrapper);
        project.getBuildersList().add(this.testBuilder);
        HtmlPage page = jenkins.createWebClient().goTo("job/test0/configure");
        HtmlSelect outputDetail = page.getElementByName("_.outputDetail");
        assertEquals("default", outputDetail.getAttribute("value"));
    }

    @Test
    public void verifyMultipleSourceFolders() throws Exception {
        this.buildWrapper.setMatlabBuildWrapperContent(new MatlabBuildWrapperContent(Message.getValue("matlab.custom.location"), Utilities.getMatlabRoot()));
        project.getBuildWrappersList().add(this.buildWrapper);
        project.setScm(new ExtractResourceSCM(Utilities.getRunMATLABTestsData()));

        RunMatlabTestsBuilder testingBuilder = new RunMatlabTestsBuilder();
        testingBuilder.setLoggingLevel("None");
        testingBuilder.setOutputDetail("None");

        // Adding list of source folder
        List<SourceFolderPaths> list=new ArrayList<SourceFolderPaths>();
        list.add(new SourceFolderPaths("src"));
        list.add(new SourceFolderPaths("src1"));
        testingBuilder.setSourceFolder(new SourceFolder(list));

        project.getBuildersList().add(testingBuilder);

        FreeStyleBuild build = project.scheduleBuild2(0).get();

        jenkins.assertBuildStatus(Result.SUCCESS, build);
        jenkins.assertLogContains("addpath(genpath('src'));", build);
        jenkins.assertLogContains("addpath(genpath('src1'));", build);
    }

    @Test
    public void verifyMultipleTestFolders() throws Exception {
        this.buildWrapper.setMatlabBuildWrapperContent(new MatlabBuildWrapperContent(Message.getValue("matlab.custom.location"), Utilities.getMatlabRoot()));
        project.getBuildWrappersList().add(this.buildWrapper);
        project.setScm(new ExtractResourceSCM(Utilities.getRunMATLABTestsData()));

        RunMatlabTestsBuilder testingBuilder = new RunMatlabTestsBuilder();
//        testingBuilder.setLoggingLevel("None");
//        testingBuilder.setOutputDetail("None");

        // Adding list of source folder
        List<SourceFolderPaths> list=new ArrayList<SourceFolderPaths>();
        list.add(new SourceFolderPaths("src"));
        testingBuilder.setSourceFolder(new SourceFolder(list));

        // Adding list of test folder
        List<TestFolders> testFolders = new ArrayList<TestFolders>();
        testFolders.add(new TestFolders("test/TestSquare"));
        testFolders.add(new TestFolders("test/TestMultiply"));
        testingBuilder.setSelectByFolder(new SelectByFolder(testFolders));

        project.getBuildersList().add(testingBuilder);
        FreeStyleBuild build = project.scheduleBuild2(0).get();

        jenkins.assertBuildStatus(Result.SUCCESS, build);
        // Based on the test folder, 'testSquare'  and 'testMultiply' tests should be run
        jenkins.assertLogContains("testSquare", build);
        jenkins.assertLogContains("testMultiply", build);
    }

    @Test
    public void verifyTestsRunInMatrixProject() throws Exception {
        String matlabRoot = System.getenv("MATLAB_ROOT");
        String matlabRoot22b = System.getenv("MATLAB_ROOT_22b");
        Assume.assumeTrue("Not running tests as MATLAB_ROOT_22b environment variable is not defined", matlabRoot22b != null && !matlabRoot22b.isEmpty());

        Utilities.setMatlabInstallation("MATLAB_PATH_1", matlabRoot, jenkins);
        Utilities.setMatlabInstallation("MATLAB_PATH_22b", matlabRoot22b, jenkins);

        MatrixProject matrixProject = jenkins.createProject(MatrixProject.class);
        MatlabInstallationAxis MATLABAxis = new MatlabInstallationAxis(Arrays.asList("MATLAB_PATH_1", "MATLAB_PATH_22b"));
        matrixProject.setAxes(new AxisList(MATLABAxis));
        matrixProject.setScm(new ExtractResourceSCM(Utilities.getRunMATLABTestsData()));

        RunMatlabTestsBuilder testingBuilder = new RunMatlabTestsBuilder();

        // Adding list of source folder
        List<SourceFolderPaths> list=new ArrayList<SourceFolderPaths>();
        list.add(new SourceFolderPaths("src"));
        testingBuilder.setSourceFolder(new SourceFolder(list));

        // Adding list of test folder
        List<TestFolders> testFolders = new ArrayList<TestFolders>();
        testFolders.add(new TestFolders("test/TestSquare"));
        testFolders.add(new TestFolders("test/TestMultiply"));
        testingBuilder.setSelectByFolder(new SelectByFolder(testFolders));

        //Adding test tag
        testingBuilder.setSelectByTag(new RunMatlabTestsBuilder.SelectByTag("TestTag"));

        matrixProject.getBuildersList().add(testingBuilder);

        MatrixBuild build = matrixProject.scheduleBuild2(0).get();

        Combination c = new Combination(new AxisList(new MatlabInstallationAxis(Arrays.asList("MATLAB_PATH_1"))), "MATLAB_PATH_1");
        MatrixRun run = build.getRun(c);
        jenkins.assertLogContains("Running testMultiply",run);
        jenkins.assertLogContains("Running testSquare", run);
        jenkins.assertLogNotContains("Running squareTest", run);
        jenkins.assertBuildStatus(Result.SUCCESS,run);

        c = new Combination(new AxisList(new MatlabInstallationAxis(Arrays.asList("MATLAB_PATH_22b"))), "MATLAB_PATH_22b");
        run = build.getRun(c);
        jenkins.assertLogContains("Running testMultiply",run);
        jenkins.assertLogContains("Running testSquare", run);
        jenkins.assertLogNotContains("Running squareTest", run);
        jenkins.assertBuildStatus(Result.SUCCESS,run);
        jenkins.assertLogContains(matlabRoot22b,run);

        jenkins.assertBuildStatus(Result.SUCCESS, run);
    }

    /*
     * Test to verify if tests are filtered scripted pipeline
     */
    @Test
    public void verifyTestsAreFiltered() throws Exception{
        String script = "node {\n" +
                Utilities.getEnvironmentScriptedPipeline() + "\n" +
                addTestData()+"\n" +
                "runMATLABTests(sourceFolder:['src'], selectByFolder: ['test/TestMultiply', 'test/TestSquare'],  selectByTag: 'TestTag')\n" +
                "}";
        WorkflowRun build = getPipelineBuild(script);
        jenkins.assertLogContains("Running testMultiply",build);
        jenkins.assertLogContains("Running testSquare", build);
        jenkins.assertLogNotContains("Running squareTest", build);
        jenkins.assertBuildStatus(Result.SUCCESS,build);
    }

    /*
     * Test to verify if tests are filtered DSL pipeline
     */
    @Test
    public void verifyTestsAreFilteredDSL() throws Exception{
        String script = "pipeline {\n" +
                "agent any" + "\n" +
                Utilities.getEnvironmentDSL() + "\n" +
                "stages{" + "\n" +
                "stage('Run MATLAB Command') {\n" +
                "steps\n" +
                "{"+
                addTestData()+"\n" +
                "runMATLABTests(sourceFolder:['src'], selectByFolder: ['test/TestMultiply', 'test/TestSquare'], selectByTag: 'TestTag')\n" +
                "}" + "\n" +
                "}" + "\n" +
                "}" + "\n" +
                "}";
        WorkflowRun build = getPipelineBuild(script);
        jenkins.assertLogContains("Running testMultiply",build);
        jenkins.assertLogContains("Running testSquare", build);
        jenkins.assertLogNotContains("Running squareTest", build);
        jenkins.assertBuildStatus(Result.SUCCESS,build);
    }
    private WorkflowRun getPipelineBuild(String script) throws Exception{
        WorkflowJob project = jenkins.createProject(WorkflowJob.class);
        project.setDefinition(new CpsFlowDefinition(script,true));
        return project.scheduleBuild2(0).get();
    }

    @Test
    public void verifyOnslave() throws Exception {
        DumbSlave s = jenkins.createOnlineSlave();
        String script = "node('!built-in') {runMATLABTests(testResultsPDF:'myresult/result.pdf')}";
        WorkflowRun build = getPipelineBuild(script);

        jenkins.assertLogNotContains("Running on Jenkins", build);
    }

    @Test
    public void verifyCmdOptions() throws Exception {
        String script = "node {runMATLABTests(testResultsPDF:'myresult/result.pdf')}";
        WorkflowRun build = getPipelineBuild(script);

        jenkins.assertLogContains("setenv('MW_ORIG_WORKING_FOLDER',", build);
        jenkins.assertLogContains("run-matlab-command", build);
    }

    @Test
    public void verifyExceptionForNonZeroExitCode() throws Exception {
        String script = "node {\n" +
                Utilities.getEnvironmentScriptedPipeline() + "\n" +
                addTestData()+"\n" +
                "runMATLABTests()\n" +
                "}";
        WorkflowRun build = getPipelineBuild(script);
        jenkins.assertBuildStatus(Result.FAILURE, build);
        jenkins.assertLogContains(String.format(Message.getValue("matlab.execution.exception.prefix"), 1), build);
    }

    private String addTestData() throws MalformedURLException {
        URL zipFile = Utilities.getRunMATLABTestsData();
        String path = "  unzip '" + zipFile.getPath() + "'" + "\n";
        return path;
    }
}
