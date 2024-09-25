package com.mathworks.ci;

/**
 * Copyright 2024 The MathWorks, Inc.
 *
 */

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

import org.json.simple.parser.ParseException;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import com.mathworks.ci.freestyle.RunMatlabBuildBuilder;

import hudson.FilePath;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;

public class TestResultsViewActionTest {
    private FreeStyleProject project;
    private UseMatlabVersionBuildWrapper buildWrapper;
    private RunMatlabBuildBuilder scriptBuilder;

    private static String VERSION_INFO_XML_FILE = "VersionInfo.xml";

    public TestResultsViewActionTest(){
    }

    @Rule
    public JenkinsRule jenkins = new JenkinsRule();


    @Before
    public void testSetup() throws IOException {
        this.project = jenkins.createFreeStyleProject();
        this.scriptBuilder = new RunMatlabBuildBuilder();
        this.buildWrapper = new UseMatlabVersionBuildWrapper();
    }

    @After
    public void testTearDown() {
        this.project = null;
        this.scriptBuilder = null;
    }

    private String getMatlabroot(String version) throws URISyntaxException {
        String defaultVersionInfo = "versioninfo/R2017a/" + VERSION_INFO_XML_FILE;
        String userVersionInfo = "versioninfo/" + version + "/" + VERSION_INFO_XML_FILE;
        URL matlabRootURL = Optional.ofNullable(getResource(userVersionInfo))
                .orElseGet(() -> getResource(defaultVersionInfo));
        File matlabRoot = new File(matlabRootURL.toURI());
        return matlabRoot.getAbsolutePath().replace(File.separator + VERSION_INFO_XML_FILE, "")
                .replace("R2017a", version);
    }

    private URL getResource(String resource) {
        return TestResultsViewAction.class.getClassLoader().getResource(resource);
    }

    /**
     *  Verify if all test results are returned from artifact
     *
     */

    @Test
    public void verifyAllTestsReturned() throws ExecutionException, InterruptedException, URISyntaxException, IOException, ParseException {
        FreeStyleBuild build = getFreestyleBuild();
        final FilePath workspace = new FilePath(new File("", "workspace"));
        final String actionID = "abc123";
        final String targetFile = MatlabBuilderConstants.TEST_RESULTS_VIEW_ARTIFACT + actionID + ".json";
        FilePath artifactRoot = new FilePath(build.getRootDir());
        copyFileInWorkspace("testArtifacts/t1/" + MatlabBuilderConstants.TEST_RESULTS_VIEW_ARTIFACT + ".json",targetFile,artifactRoot);
        TestResultsViewAction ac = new TestResultsViewAction(build, workspace, actionID);
        List<List<TestFile>> ta = ac.getTestResults();
        int actualTestSessions = ta.size();
        Assert.assertEquals("Incorrect test sessions",2,actualTestSessions);
        int actualTestFiles1 = ta.get(0).size();
        Assert.assertEquals("Incorrect test files",1,actualTestFiles1);
        int actualTestFiles2 = ta.get(1).size();
        Assert.assertEquals("Incorrect test files",1,actualTestFiles2);
        int actualTestResults1 = ta.get(0).get(0).getTestCases().size();
        Assert.assertEquals("Incorrect test results",3,actualTestResults1);
        int actualTestResults2 = ta.get(1).get(0).getTestCases().size();
        Assert.assertEquals("Incorrect test results",1,actualTestResults2);
    }

    /**
     *  Verify if total test results count is correct
     *
     */

    @Test
    public void verifyTotalTestsCount() throws ExecutionException, InterruptedException, URISyntaxException, IOException, ParseException {
        FreeStyleBuild build = getFreestyleBuild();
        final FilePath workspace = new FilePath(new File("", "workspace"));
        final String actionID = "abc123";
        final String targetFile = MatlabBuilderConstants.TEST_RESULTS_VIEW_ARTIFACT + actionID + ".json";
        FilePath artifactRoot = new FilePath(build.getRootDir());
        copyFileInWorkspace("testArtifacts/t1/" + MatlabBuilderConstants.TEST_RESULTS_VIEW_ARTIFACT + ".json",targetFile,artifactRoot);
        TestResultsViewAction ac = new TestResultsViewAction(build, workspace, actionID);
        int actualCount = ac.getTotalCount();
        Assert.assertEquals("Incorrect total tests count",4,actualCount);
    }

    /**
     *  Verify if passed tests count is correct
     *
     */

    @Test
    public void verifyPassedTestsCount() throws ExecutionException, InterruptedException, URISyntaxException, IOException, ParseException {
        FreeStyleBuild build = getFreestyleBuild();
        final FilePath workspace = new FilePath(new File("", "workspace"));
        final String actionID = "abc123";
        final String targetFile = MatlabBuilderConstants.TEST_RESULTS_VIEW_ARTIFACT + actionID + ".json";
        FilePath artifactRoot = new FilePath(build.getRootDir());
        copyFileInWorkspace("testArtifacts/t1/" + MatlabBuilderConstants.TEST_RESULTS_VIEW_ARTIFACT + ".json",targetFile,artifactRoot);
        TestResultsViewAction ac = new TestResultsViewAction(build, workspace, actionID);
        int actualCount = ac.getPassedCount();
        Assert.assertEquals("Incorrect passed tests count",2,actualCount);
    }

    /**
     *  Verify if failed tests count is correct
     *
     */

    @Test
    public void verifyFailedTestsCount() throws ExecutionException, InterruptedException, URISyntaxException, IOException, ParseException {
        FreeStyleBuild build = getFreestyleBuild();
        final FilePath workspace = new FilePath(new File("", "workspace"));
        final String actionID = "abc123";
        final String targetFile = MatlabBuilderConstants.TEST_RESULTS_VIEW_ARTIFACT + actionID + ".json";
        FilePath artifactRoot = new FilePath(build.getRootDir());
        copyFileInWorkspace("testArtifacts/t1/" + MatlabBuilderConstants.TEST_RESULTS_VIEW_ARTIFACT + ".json",targetFile,artifactRoot);
        TestResultsViewAction ac = new TestResultsViewAction(build, workspace, actionID);
        int actualCount = ac.getFailedCount();
        Assert.assertEquals("Incorrect failed tests count",1,actualCount);
    }

    /**
     *  Verify if incomplete tests count is correct
     *
     */

    @Test
    public void verifyIncompleteTestsCount() throws ExecutionException, InterruptedException, URISyntaxException, IOException, ParseException {
        FreeStyleBuild build = getFreestyleBuild();
        final FilePath workspace = new FilePath(new File("", "workspace"));
        final String actionID = "abc123";
        final String targetFile = MatlabBuilderConstants.TEST_RESULTS_VIEW_ARTIFACT + actionID + ".json";
        FilePath artifactRoot = new FilePath(build.getRootDir());
        copyFileInWorkspace("testArtifacts" + File.separator + "t1" + File.separator + MatlabBuilderConstants.TEST_RESULTS_VIEW_ARTIFACT + ".json",targetFile,artifactRoot);
        TestResultsViewAction ac = new TestResultsViewAction(build, workspace, actionID);
        int actualCount = ac.getIncompleteCount();
        Assert.assertEquals("Incorrect incomplete tests count",0,actualCount);
    }

    /**
     *  Verify if not run tests count is correct
     *
     */

    @Test
    public void verifyNotRunTestsCount() throws ExecutionException, InterruptedException, URISyntaxException, IOException, ParseException {
        FreeStyleBuild build = getFreestyleBuild();
        final FilePath workspace = new FilePath(new File("", "workspace"));
        final String actionID = "abc123";
        final String targetFile = MatlabBuilderConstants.TEST_RESULTS_VIEW_ARTIFACT + actionID + ".json";
        FilePath artifactRoot = new FilePath(build.getRootDir());
        copyFileInWorkspace("testArtifacts/t1/" + MatlabBuilderConstants.TEST_RESULTS_VIEW_ARTIFACT + ".json",targetFile,artifactRoot);
        TestResultsViewAction ac = new TestResultsViewAction(build, workspace, actionID);
        int actualCount = ac.getNotRunCount();
        Assert.assertEquals("Incorrect not run tests count",1,actualCount);
    }

    /**
     *  Verify if test file path is correct
     *
     */

    @Test
    public void verifyTestFilePath() throws ExecutionException, InterruptedException, URISyntaxException, IOException, ParseException {
        FreeStyleBuild build = getFreestyleBuild();
        final FilePath workspace = new FilePath(new File("", "workspace"));
        final String actionID = "abc123";
        final String targetFile = MatlabBuilderConstants.TEST_RESULTS_VIEW_ARTIFACT + actionID + ".json";
        FilePath artifactRoot = new FilePath(build.getRootDir());
        copyFileInWorkspace("testArtifacts/t1/" + MatlabBuilderConstants.TEST_RESULTS_VIEW_ARTIFACT + ".json",targetFile,artifactRoot);
        TestResultsViewAction ac = new TestResultsViewAction(build, workspace, actionID);
        List<List<TestFile>> ta = ac.getTestResults();
        String actualPath1 = ta.get(0).get(0).getFilePath();
        Assert.assertEquals("Incorrect test file path","workspace\\visualization\\tests",actualPath1);
        String actualPath2 = ta.get(1).get(0).getFilePath();
        Assert.assertEquals("Incorrect test file path","workspace\\visualization\\duplicate tests",actualPath2);
    }

    /**
     *  Verify if test file name is correct
     *
     */

    @Test
    public void verifyTestFileName() throws ExecutionException, InterruptedException, URISyntaxException, IOException, ParseException {
        FreeStyleBuild build = getFreestyleBuild();
        final FilePath workspace = new FilePath(new File("", "workspace"));
        final String actionID = "abc123";
        final String targetFile = MatlabBuilderConstants.TEST_RESULTS_VIEW_ARTIFACT + actionID + ".json";
        FilePath artifactRoot = new FilePath(build.getRootDir());
        copyFileInWorkspace("testArtifacts/t1/" + MatlabBuilderConstants.TEST_RESULTS_VIEW_ARTIFACT + ".json",targetFile,artifactRoot);
        TestResultsViewAction ac = new TestResultsViewAction(build, workspace, actionID);
        List<List<TestFile>> ta = ac.getTestResults();
        String actualName1 = ta.get(0).get(0).getName();
        Assert.assertEquals("Incorrect test file name","TestExamples1",actualName1);
        String actualName2 = ta.get(1).get(0).getName();
        Assert.assertEquals("Incorrect test file name","TestExamples2",actualName2);
    }

    /**
     *  Verify if test file duration is correct
     *
     */

    @Test
    public void verifyTestFileDuration() throws ExecutionException, InterruptedException, URISyntaxException, IOException, ParseException {
        FreeStyleBuild build = getFreestyleBuild();
        final FilePath workspace = new FilePath(new File("", "workspace"));
        final String actionID = "abc123";
        final String targetFile = MatlabBuilderConstants.TEST_RESULTS_VIEW_ARTIFACT + actionID + ".json";
        FilePath artifactRoot = new FilePath(build.getRootDir());
        copyFileInWorkspace("testArtifacts/t1/" + MatlabBuilderConstants.TEST_RESULTS_VIEW_ARTIFACT + ".json",targetFile,artifactRoot);
        TestResultsViewAction ac = new TestResultsViewAction(build, workspace, actionID);
        List<List<TestFile>> ta = ac.getTestResults();
        Double actualDuration1 = ta.get(0).get(0).getDuration();
        Assert.assertEquals("Incorrect test file duration",(Double) 0.5,actualDuration1);
        Double actualDuration2 = ta.get(1).get(0).getDuration();
        Assert.assertEquals("Incorrect test file duration",(Double) 0.1,actualDuration2);
    }

    /**
     *  Verify if test file status is correct
     *
     */

    @Test
    public void verifyTestFileStatus() throws ExecutionException, InterruptedException, URISyntaxException, IOException, ParseException {
        FreeStyleBuild build = getFreestyleBuild();
        final FilePath workspace = new FilePath(new File("", "workspace"));
        final String actionID = "abc123";
        final String targetFile = MatlabBuilderConstants.TEST_RESULTS_VIEW_ARTIFACT + actionID + ".json";
        FilePath artifactRoot = new FilePath(build.getRootDir());
        copyFileInWorkspace("testArtifacts/t1/" + MatlabBuilderConstants.TEST_RESULTS_VIEW_ARTIFACT + ".json",targetFile,artifactRoot);
        TestResultsViewAction ac = new TestResultsViewAction(build, workspace, actionID);
        List<List<TestFile>> ta = ac.getTestResults();
        String actualStatus1 = ta.get(0).get(0).getStatus();
        Assert.assertEquals("Incorrect test file status","Failed",actualStatus1);
        String actualStatus2 = ta.get(1).get(0).getStatus();
        Assert.assertEquals("Incorrect test file status","Passed",actualStatus2);
    }

    /**
     *  Verify if test case name is correct
     *
     */

    @Test
    public void verifyTestCaseName() throws ExecutionException, InterruptedException, URISyntaxException, IOException, ParseException {
        FreeStyleBuild build = getFreestyleBuild();
        final FilePath workspace = new FilePath(new File("", "workspace"));
        final String actionID = "abc123";
        final String targetFile = MatlabBuilderConstants.TEST_RESULTS_VIEW_ARTIFACT + actionID + ".json";
        FilePath artifactRoot = new FilePath(build.getRootDir());
        copyFileInWorkspace("testArtifacts/t1/" + MatlabBuilderConstants.TEST_RESULTS_VIEW_ARTIFACT + ".json",targetFile,artifactRoot);
        TestResultsViewAction ac = new TestResultsViewAction(build, workspace, actionID);
        List<List<TestFile>> ta = ac.getTestResults();
        String actualName1_1 = ta.get(0).get(0).getTestCases().get(0).getName();
        Assert.assertEquals("Incorrect test case name","testNonLeapYear",actualName1_1);
        String actualName1_2 = ta.get(0).get(0).getTestCases().get(1).getName();
        Assert.assertEquals("Incorrect test case name","testLeapYear",actualName1_2);
        String actualName1_3 = ta.get(0).get(0).getTestCases().get(2).getName();
        Assert.assertEquals("Incorrect test case name","testInvalidDateFormat",actualName1_3);
        String actualName2 = ta.get(1).get(0).getTestCases().get(0).getName();
        Assert.assertEquals("Incorrect test case name","testNonLeapYear",actualName2);
    }

    /**
     *  Verify if test case status is correct
     *
     */

    @Test
    public void verifyTestCaseStatus() throws ExecutionException, InterruptedException, URISyntaxException, IOException, ParseException {
        FreeStyleBuild build = getFreestyleBuild();
        final FilePath workspace = new FilePath(new File("", "workspace"));
        final String actionID = "abc123";
        final String targetFile = MatlabBuilderConstants.TEST_RESULTS_VIEW_ARTIFACT + actionID + ".json";
        FilePath artifactRoot = new FilePath(build.getRootDir());
        copyFileInWorkspace("testArtifacts/t1/" + MatlabBuilderConstants.TEST_RESULTS_VIEW_ARTIFACT + ".json",targetFile,artifactRoot);
        TestResultsViewAction ac = new TestResultsViewAction(build, workspace, actionID);
        List<List<TestFile>> ta = ac.getTestResults();
        String actualStatus1_1 = ta.get(0).get(0).getTestCases().get(0).getStatus();
        Assert.assertEquals("Incorrect test case status","Passed",actualStatus1_1);
        String actualStatus1_2 = ta.get(0).get(0).getTestCases().get(1).getStatus();
        Assert.assertEquals("Incorrect test case status","Failed",actualStatus1_2);
        String actualStatus1_3 = ta.get(0).get(0).getTestCases().get(2).getStatus();
        Assert.assertEquals("Incorrect test case status","NotRun",actualStatus1_3);
        String actualStatus2 = ta.get(1).get(0).getTestCases().get(0).getStatus();
        Assert.assertEquals("Incorrect test case status","Passed",actualStatus2);
    }

    /**
     *  Verify if test case duration is correct
     *
     */

    @Test
    public void verifyTestCaseDuration() throws ExecutionException, InterruptedException, URISyntaxException, IOException, ParseException {
        FreeStyleBuild build = getFreestyleBuild();
        final FilePath workspace = new FilePath(new File("", "workspace"));
        final String actionID = "abc123";
        final String targetFile = MatlabBuilderConstants.TEST_RESULTS_VIEW_ARTIFACT + actionID + ".json";
        FilePath artifactRoot = new FilePath(build.getRootDir());
        copyFileInWorkspace("testArtifacts/t1/" + MatlabBuilderConstants.TEST_RESULTS_VIEW_ARTIFACT + ".json",targetFile,artifactRoot);
        TestResultsViewAction ac = new TestResultsViewAction(build, workspace, actionID);
        List<List<TestFile>> ta = ac.getTestResults();
        Double actualDuration1_1 = ta.get(0).get(0).getTestCases().get(0).getDuration();
        Assert.assertEquals("Incorrect test case duration",(Double) 0.1,actualDuration1_1);
        Double actualDuration1_2 = ta.get(0).get(0).getTestCases().get(1).getDuration();
        Assert.assertEquals("Incorrect test case duration",(Double) 0.4,actualDuration1_2);
        Double actualDuration1_3 = ta.get(0).get(0).getTestCases().get(2).getDuration();
        Assert.assertEquals("Incorrect test case duration",(Double) 0.0,actualDuration1_3);
        Double actualDuration2 = ta.get(1).get(0).getTestCases().get(0).getDuration();
        Assert.assertEquals("Incorrect test case duration",(Double) 0.1,actualDuration2);
    }

    /**
     *  Verify if test case diagnostics is correct
     *
     */

    @Test
    public void verifyTestCaseDiagnostics() throws ExecutionException, InterruptedException, URISyntaxException, IOException, ParseException {
        FreeStyleBuild build = getFreestyleBuild();
        final FilePath workspace = new FilePath(new File("", "workspace"));
        final String actionID = "abc123";
        final String targetFile = MatlabBuilderConstants.TEST_RESULTS_VIEW_ARTIFACT + actionID + ".json";
        FilePath artifactRoot = new FilePath(build.getRootDir());
        copyFileInWorkspace("testArtifacts/t1/" + MatlabBuilderConstants.TEST_RESULTS_VIEW_ARTIFACT + ".json",targetFile,artifactRoot);
        TestResultsViewAction ac = new TestResultsViewAction(build, workspace, actionID);
        List<List<TestFile>> ta = ac.getTestResults();
        TestDiagnostics diagnostics = ta.get(0).get(0).getTestCases().get(1).getDiagnostics().get(0);
        String actualDiagnosticsEvent = diagnostics.getEvent();
        Assert.assertEquals("Incorrect test diagnostics event","SampleDiagnosticsEvent",actualDiagnosticsEvent);
        String actualDiagnosticsReport = diagnostics.getReport();
        Assert.assertEquals("Incorrect test diagnostics report","SampleDiagnosticsReport",actualDiagnosticsReport);
    }

    /**
     *  Verify if actionID is set correctly
     *
     */

    @Test
    public void verifyActionID() throws ExecutionException, InterruptedException, URISyntaxException, IOException, ParseException {
        FreeStyleBuild build = getFreestyleBuild();
        final FilePath workspace = new FilePath(new File("", "workspace"));
        final String actionID = "abc123";
        final String targetFile = MatlabBuilderConstants.TEST_RESULTS_VIEW_ARTIFACT + actionID + ".json";
        FilePath artifactRoot = new FilePath(build.getRootDir());
        copyFileInWorkspace("testArtifacts/t1/" + MatlabBuilderConstants.TEST_RESULTS_VIEW_ARTIFACT + ".json",targetFile,artifactRoot);
        TestResultsViewAction ac = new TestResultsViewAction(build, workspace, actionID);
        String actualActionID = ac.getActionID();
        Assert.assertEquals("Incorrect action ID",actionID,actualActionID);
    }

    private void copyFileInWorkspace(String sourceFile, String targetFile, FilePath targetWorkspace)
            throws IOException, InterruptedException {
        final ClassLoader classLoader = getClass().getClassLoader();
        FilePath targetFilePath = new FilePath(targetWorkspace, targetFile);
        InputStream in = classLoader.getResourceAsStream(sourceFile);
        targetFilePath.copyFrom(in);
        // set executable permission
        targetFilePath.chmod(0777);
    }

    private FreeStyleBuild getFreestyleBuild() throws ExecutionException, InterruptedException, URISyntaxException {
        this.buildWrapper.setMatlabBuildWrapperContent(new MatlabBuildWrapperContent(Message.getValue("matlab.custom.location"), getMatlabroot("R2017a")));
        project.getBuildWrappersList().add(this.buildWrapper);
        scriptBuilder.setTasks("");
        project.getBuildersList().add(this.scriptBuilder);
        FreeStyleBuild build = project.scheduleBuild2(0).get();
        return build;
    }
}
