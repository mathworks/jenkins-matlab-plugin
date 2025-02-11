package com.mathworks.ci;

/**
 * Copyright 2025 The MathWorks, Inc.
 *
 */

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
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

import com.mathworks.ci.MatlabBuildWrapperContent;
import com.mathworks.ci.MatlabBuilderConstants;
import com.mathworks.ci.MatlabTestDiagnostics;
import com.mathworks.ci.MatlabTestFile;
import com.mathworks.ci.TestResultsViewAction;
import com.mathworks.ci.TestResultsViewAction.*;
import com.mathworks.ci.UseMatlabVersionBuildWrapper;
import com.mathworks.ci.freestyle.RunMatlabBuildBuilder;

import hudson.FilePath;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;

public class TestResultsViewActionTest {
    private FreeStyleProject project;
    private UseMatlabVersionBuildWrapper buildWrapper;
    private RunMatlabBuildBuilder scriptBuilder;

    private static String VERSION_INFO_XML_FILE = "VersionInfo.xml";

    public TestResultsViewActionTest() {}

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
        final FilePath workspace = new FilePath(new File("C:", "workspace"));
        final String actionID = "abc123";
        final String targetFile = MatlabBuilderConstants.TEST_RESULTS_VIEW_ARTIFACT + actionID + ".json";
        FilePath artifactRoot = new FilePath(build.getRootDir());
        copyFileInWorkspace("testArtifacts/t1/windows/" + MatlabBuilderConstants.TEST_RESULTS_VIEW_ARTIFACT + ".json",targetFile,artifactRoot);
        TestResultsViewAction ac = new TestResultsViewAction(build, workspace, actionID);
        List<List<MatlabTestFile>> ta = ac.getTestResults();
        int actualTestSessions = ta.size();
        Assert.assertEquals("Incorrect test sessions",2,actualTestSessions);
        int actualTestFiles1 = ta.get(0).size();
        Assert.assertEquals("Incorrect test files",1,actualTestFiles1);
        int actualTestFiles2 = ta.get(1).size();
        Assert.assertEquals("Incorrect test files",1,actualTestFiles2);
        int actualTestResults1 = ta.get(0).get(0).getMatlabTestCases().size();
        Assert.assertEquals("Incorrect test results",9,actualTestResults1);
        int actualTestResults2 = ta.get(1).get(0).getMatlabTestCases().size();
        Assert.assertEquals("Incorrect test results",1,actualTestResults2);
    }

    /**
     *  Verify if total test results count is correct
     *
     */

    @Test
    public void verifyTotalTestsCount() throws ExecutionException, InterruptedException, URISyntaxException, IOException, ParseException {
        FreeStyleBuild build = getFreestyleBuild();
        final FilePath workspace = new FilePath(new File("C:", "workspace"));
        final String actionID = "abc123";
        final String targetFile = MatlabBuilderConstants.TEST_RESULTS_VIEW_ARTIFACT + actionID + ".json";
        FilePath artifactRoot = new FilePath(build.getRootDir());
        copyFileInWorkspace("testArtifacts/t1/windows/" + MatlabBuilderConstants.TEST_RESULTS_VIEW_ARTIFACT + ".json",targetFile,artifactRoot);
        TestResultsViewAction ac = new TestResultsViewAction(build, workspace, actionID);
        int actualCount = ac.getTotalCount();
        Assert.assertEquals("Incorrect total tests count",10,actualCount);
    }

    /**
     *  Verify if passed tests count is correct
     *
     */

    @Test
    public void verifyPassedTestsCount() throws ExecutionException, InterruptedException, URISyntaxException, IOException, ParseException {
        FreeStyleBuild build = getFreestyleBuild();
        final FilePath workspace = new FilePath(new File("C:", "workspace"));
        final String actionID = "abc123";
        final String targetFile = MatlabBuilderConstants.TEST_RESULTS_VIEW_ARTIFACT + actionID + ".json";
        FilePath artifactRoot = new FilePath(build.getRootDir());
        copyFileInWorkspace("testArtifacts/t1/windows/" + MatlabBuilderConstants.TEST_RESULTS_VIEW_ARTIFACT + ".json",targetFile,artifactRoot);
        TestResultsViewAction ac = new TestResultsViewAction(build, workspace, actionID);
        int actualCount = ac.getPassedCount();
        Assert.assertEquals("Incorrect passed tests count",4,actualCount);
    }

    /**
     *  Verify if failed tests count is correct
     *
     */

    @Test
    public void verifyFailedTestsCount() throws ExecutionException, InterruptedException, URISyntaxException, IOException, ParseException {
        FreeStyleBuild build = getFreestyleBuild();
        final FilePath workspace = new FilePath(new File("C:", "workspace"));
        final String actionID = "abc123";
        final String targetFile = MatlabBuilderConstants.TEST_RESULTS_VIEW_ARTIFACT + actionID + ".json";
        FilePath artifactRoot = new FilePath(build.getRootDir());
        copyFileInWorkspace("testArtifacts/t1/windows/" + MatlabBuilderConstants.TEST_RESULTS_VIEW_ARTIFACT + ".json",targetFile,artifactRoot);
        TestResultsViewAction ac = new TestResultsViewAction(build, workspace, actionID);
        int actualCount = ac.getFailedCount();
        Assert.assertEquals("Incorrect failed tests count",3,actualCount);
    }

    /**
     *  Verify if incomplete tests count is correct
     *
     */

    @Test
    public void verifyIncompleteTestsCount() throws ExecutionException, InterruptedException, URISyntaxException, IOException, ParseException {
        FreeStyleBuild build = getFreestyleBuild();
        final FilePath workspace = new FilePath(new File("C:", "workspace"));
        final String actionID = "abc123";
        final String targetFile = MatlabBuilderConstants.TEST_RESULTS_VIEW_ARTIFACT + actionID + ".json";
        FilePath artifactRoot = new FilePath(build.getRootDir());
        copyFileInWorkspace("testArtifacts/t1/windows/" + MatlabBuilderConstants.TEST_RESULTS_VIEW_ARTIFACT + ".json",targetFile,artifactRoot);
        TestResultsViewAction ac = new TestResultsViewAction(build, workspace, actionID);
        int actualCount = ac.getIncompleteCount();
        Assert.assertEquals("Incorrect incomplete tests count",2,actualCount);
    }

    /**
     *  Verify if not run tests count is correct
     *
     */

    @Test
    public void verifyNotRunTestsCount() throws ExecutionException, InterruptedException, URISyntaxException, IOException, ParseException {
        FreeStyleBuild build = getFreestyleBuild();
        final FilePath workspace = new FilePath(new File("C:\\workspace"));
        final String actionID = "abc123";
        final String targetFile = MatlabBuilderConstants.TEST_RESULTS_VIEW_ARTIFACT + actionID + ".json";
        FilePath artifactRoot = new FilePath(build.getRootDir());
        copyFileInWorkspace("testArtifacts/t1/windows/" + MatlabBuilderConstants.TEST_RESULTS_VIEW_ARTIFACT + ".json",targetFile,artifactRoot);
        TestResultsViewAction ac = new TestResultsViewAction(build, workspace, actionID);
        int actualCount = ac.getNotRunCount();
        Assert.assertEquals("Incorrect not run tests count",1,actualCount);
    }

    /**
     *  Verify if test file path is correct
     *
     */

    @Test
    public void verifyMatlabTestFilePath() throws ExecutionException, InterruptedException, URISyntaxException, IOException, ParseException {
        FreeStyleBuild build = getFreestyleBuild();
        final String actionID = "abc123";
        final String targetFile = MatlabBuilderConstants.TEST_RESULTS_VIEW_ARTIFACT + actionID + ".json";
        FilePath artifactRoot = new FilePath(build.getRootDir());

        String os = System.getProperty("os.name").toLowerCase();
        String testFolder = "testArtifacts/t1/";
        String workspaceParent = "";
        String expectedParentPath = "";
        if (os.contains("win")) {
            testFolder += "windows/";
            workspaceParent = "C:\\";
            expectedParentPath = "workspace\\visualization\\";
        } else if (os.contains("nix") || os.contains("nux") || os.contains("aix")) {
            testFolder += "linux/";
            workspaceParent = "/home/user/";
            expectedParentPath = "workspace/visualization/";
        } else if (os.contains("mac")) {
            testFolder += "mac/";
            workspaceParent = "/Users/username/";
            expectedParentPath = "workspace/visualization/";
        } else {
            throw new RuntimeException("Unsupported OS: " + os);
        }
        copyFileInWorkspace(testFolder + MatlabBuilderConstants.TEST_RESULTS_VIEW_ARTIFACT + ".json",targetFile,artifactRoot);
        final FilePath workspace = new FilePath(new File(workspaceParent + "workspace"));

        TestResultsViewAction ac = new TestResultsViewAction(build, workspace, actionID);
        List<List<MatlabTestFile>> ta = ac.getTestResults();
        String actualPath1 = ta.get(0).get(0).getPath();
        Assert.assertEquals("Incorrect test file path",expectedParentPath + "tests",actualPath1);
        String actualPath2 = ta.get(1).get(0).getPath();
        Assert.assertEquals("Incorrect test file path",expectedParentPath + "duplicate_tests",actualPath2);
    }

    /**
     *  Verify if test file name is correct
     *
     */

    @Test
    public void verifyMatlabTestFileName() throws ExecutionException, InterruptedException, URISyntaxException, IOException, ParseException {
        FreeStyleBuild build = getFreestyleBuild();
        final FilePath workspace = new FilePath(new File("C:", "workspace"));
        final String actionID = "abc123";
        final String targetFile = MatlabBuilderConstants.TEST_RESULTS_VIEW_ARTIFACT + actionID + ".json";
        FilePath artifactRoot = new FilePath(build.getRootDir());
        copyFileInWorkspace("testArtifacts/t1/windows/" + MatlabBuilderConstants.TEST_RESULTS_VIEW_ARTIFACT + ".json",targetFile,artifactRoot);
        TestResultsViewAction ac = new TestResultsViewAction(build, workspace, actionID);
        List<List<MatlabTestFile>> ta = ac.getTestResults();
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
    public void verifyMatlabTestFileDuration() throws ExecutionException, InterruptedException, URISyntaxException, IOException, ParseException {
        FreeStyleBuild build = getFreestyleBuild();
        final FilePath workspace = new FilePath(new File("C:", "workspace"));
        final String actionID = "abc123";
        final String targetFile = MatlabBuilderConstants.TEST_RESULTS_VIEW_ARTIFACT + actionID + ".json";
        FilePath artifactRoot = new FilePath(build.getRootDir());
        copyFileInWorkspace("testArtifacts/t1/windows/" + MatlabBuilderConstants.TEST_RESULTS_VIEW_ARTIFACT + ".json",targetFile,artifactRoot);
        TestResultsViewAction ac = new TestResultsViewAction(build, workspace, actionID);
        List<List<MatlabTestFile>> ta = ac.getTestResults();
        BigDecimal actualDuration1 = ta.get(0).get(0).getDuration();
        Assert.assertEquals("Incorrect test file duration",new BigDecimal("1.7"),actualDuration1);
        BigDecimal actualDuration2 = ta.get(1).get(0).getDuration();
        Assert.assertEquals("Incorrect test file duration",new BigDecimal("0.1"),actualDuration2);
    }

    /**
     *  Verify if test file status is correct
     *
     */

    @Test
    public void verifyMatlabTestFileStatus() throws ExecutionException, InterruptedException, URISyntaxException, IOException, ParseException {
        FreeStyleBuild build = getFreestyleBuild();
        final FilePath workspace = new FilePath(new File("C:", "workspace"));
        final String actionID = "abc123";
        final String targetFile = MatlabBuilderConstants.TEST_RESULTS_VIEW_ARTIFACT + actionID + ".json";
        FilePath artifactRoot = new FilePath(build.getRootDir());
        copyFileInWorkspace("testArtifacts/t1/windows/" + MatlabBuilderConstants.TEST_RESULTS_VIEW_ARTIFACT + ".json",targetFile,artifactRoot);
        TestResultsViewAction ac = new TestResultsViewAction(build, workspace, actionID);
        List<List<MatlabTestFile>> ta = ac.getTestResults();
        TestStatus actualStatus1 = ta.get(0).get(0).getStatus();
        Assert.assertEquals("Incorrect test file status",TestStatus.FAILED,actualStatus1);
        TestStatus actualStatus2 = ta.get(1).get(0).getStatus();
        Assert.assertEquals("Incorrect test file status",TestStatus.INCOMPLETE,actualStatus2);
    }

    /**
     *  Verify if test case name is correct
     *
     */

    @Test
    public void verifyMatlabTestCaseName() throws ExecutionException, InterruptedException, URISyntaxException, IOException, ParseException {
        FreeStyleBuild build = getFreestyleBuild();
        final FilePath workspace = new FilePath(new File("C:", "workspace"));
        final String actionID = "abc123";
        final String targetFile = MatlabBuilderConstants.TEST_RESULTS_VIEW_ARTIFACT + actionID + ".json";
        FilePath artifactRoot = new FilePath(build.getRootDir());
        copyFileInWorkspace("testArtifacts/t1/windows/" + MatlabBuilderConstants.TEST_RESULTS_VIEW_ARTIFACT + ".json",targetFile,artifactRoot);
        TestResultsViewAction ac = new TestResultsViewAction(build, workspace, actionID);
        List<List<MatlabTestFile>> ta = ac.getTestResults();
        String actualName1_1 = ta.get(0).get(0).getMatlabTestCases().get(0).getName();
        Assert.assertEquals("Incorrect test case name","testNonLeapYear",actualName1_1);
        String actualName1_5 = ta.get(0).get(0).getMatlabTestCases().get(4).getName();
        Assert.assertEquals("Incorrect test case name","testLeapYear",actualName1_5);
        String actualName1_8 = ta.get(0).get(0).getMatlabTestCases().get(7).getName();
        Assert.assertEquals("Incorrect test case name","testValidDateFormat",actualName1_8);
        String actualName1_9 = ta.get(0).get(0).getMatlabTestCases().get(8).getName();
        Assert.assertEquals("Incorrect test case name","testInvalidDateFormat",actualName1_9);
        String actualName2 = ta.get(1).get(0).getMatlabTestCases().get(0).getName();
        Assert.assertEquals("Incorrect test case name","testNonLeapYear",actualName2);
    }

    /**
     *  Verify if test case status is correct
     *
     */

    @Test
    public void verifyMatlabTestCaseStatus() throws ExecutionException, InterruptedException, URISyntaxException, IOException, ParseException {
        FreeStyleBuild build = getFreestyleBuild();
        final FilePath workspace = new FilePath(new File("C:", "workspace"));
        final String actionID = "abc123";
        final String targetFile = MatlabBuilderConstants.TEST_RESULTS_VIEW_ARTIFACT + actionID + ".json";
        FilePath artifactRoot = new FilePath(build.getRootDir());
        copyFileInWorkspace("testArtifacts/t1/windows/" + MatlabBuilderConstants.TEST_RESULTS_VIEW_ARTIFACT + ".json",targetFile,artifactRoot);
        TestResultsViewAction ac = new TestResultsViewAction(build, workspace, actionID);
        List<List<MatlabTestFile>> ta = ac.getTestResults();
        TestStatus actualStatus1_1 = ta.get(0).get(0).getMatlabTestCases().get(0).getStatus();
        Assert.assertEquals("Incorrect test case status",TestStatus.PASSED,actualStatus1_1);
        TestStatus actualStatus1_5 = ta.get(0).get(0).getMatlabTestCases().get(4).getStatus();
        Assert.assertEquals("Incorrect test case status",TestStatus.FAILED,actualStatus1_5);
        TestStatus actualStatus1_9 = ta.get(0).get(0).getMatlabTestCases().get(8).getStatus();
        Assert.assertEquals("Incorrect test case status",TestStatus.NOT_RUN,actualStatus1_9);
        TestStatus actualStatus2 = ta.get(1).get(0).getMatlabTestCases().get(0).getStatus();
        Assert.assertEquals("Incorrect test case status",TestStatus.INCOMPLETE,actualStatus2);
    }

    /**
     *  Verify if test case duration is correct
     *
     */

    @Test
    public void verifyMatlabTestCaseDuration() throws ExecutionException, InterruptedException, URISyntaxException, IOException, ParseException {
        FreeStyleBuild build = getFreestyleBuild();
        final FilePath workspace = new FilePath(new File("C:", "workspace"));
        final String actionID = "abc123";
        final String targetFile = MatlabBuilderConstants.TEST_RESULTS_VIEW_ARTIFACT + actionID + ".json";
        FilePath artifactRoot = new FilePath(build.getRootDir());
        copyFileInWorkspace("testArtifacts/t1/windows/" + MatlabBuilderConstants.TEST_RESULTS_VIEW_ARTIFACT + ".json",targetFile,artifactRoot);
        TestResultsViewAction ac = new TestResultsViewAction(build, workspace, actionID);
        List<List<MatlabTestFile>> ta = ac.getTestResults();
        BigDecimal actualDuration1_1 = ta.get(0).get(0).getMatlabTestCases().get(0).getDuration();
        Assert.assertEquals("Incorrect test case duration",new BigDecimal("0.1"),actualDuration1_1);
        BigDecimal actualDuration1_5 = ta.get(0).get(0).getMatlabTestCases().get(4).getDuration();
        Assert.assertEquals("Incorrect test case duration",new BigDecimal("0.4"),actualDuration1_5);
        BigDecimal actualDuration1_9 = ta.get(0).get(0).getMatlabTestCases().get(8).getDuration();
        Assert.assertEquals("Incorrect test case duration",new BigDecimal("0"),actualDuration1_9);
        BigDecimal actualDuration2 = ta.get(1).get(0).getMatlabTestCases().get(0).getDuration();
        Assert.assertEquals("Incorrect test case duration",new BigDecimal("0.1"),actualDuration2);
    }

    /**
     *  Verify if test case diagnostics is correct
     *
     */

    @Test
    public void verifyMatlabTestCaseDiagnostics() throws ExecutionException, InterruptedException, URISyntaxException, IOException, ParseException {
        FreeStyleBuild build = getFreestyleBuild();
        final FilePath workspace = new FilePath(new File("C:", "workspace"));
        final String actionID = "abc123";
        final String targetFile = MatlabBuilderConstants.TEST_RESULTS_VIEW_ARTIFACT + actionID + ".json";
        FilePath artifactRoot = new FilePath(build.getRootDir());
        copyFileInWorkspace("testArtifacts/t1/windows/" + MatlabBuilderConstants.TEST_RESULTS_VIEW_ARTIFACT + ".json",targetFile,artifactRoot);
        TestResultsViewAction ac = new TestResultsViewAction(build, workspace, actionID);
        List<List<MatlabTestFile>> ta = ac.getTestResults();
        
        MatlabTestDiagnostics diagnostics1 = ta.get(0).get(0).getMatlabTestCases().get(4).getDiagnostics().get(0);
        String actualDiagnosticsEvent1 = diagnostics1.getEvent();
        Assert.assertEquals("Incorrect test diagnostics event","SampleDiagnosticsEvent1",actualDiagnosticsEvent1);
        String actualDiagnosticsReport1 = diagnostics1.getReport();
        Assert.assertEquals("Incorrect test diagnostics report","SampleDiagnosticsReport1",actualDiagnosticsReport1);

        MatlabTestDiagnostics diagnostics2 = ta.get(1).get(0).getMatlabTestCases().get(0).getDiagnostics().get(0);
        String actualDiagnosticsEvent2 = diagnostics2.getEvent();
        Assert.assertEquals("Incorrect test diagnostics event","SampleDiagnosticsEvent2",actualDiagnosticsEvent2);
        String actualDiagnosticsReport2 = diagnostics2.getReport();
        Assert.assertEquals("Incorrect test diagnostics report","SampleDiagnosticsReport2",actualDiagnosticsReport2);
    }

    /**
     *  Verify if actionID is set correctly
     *
     */

    @Test
    public void verifyActionID() throws ExecutionException, InterruptedException, URISyntaxException, IOException, ParseException {
        FreeStyleBuild build = getFreestyleBuild();
        final FilePath workspace = new FilePath(new File("C:", "workspace"));
        final String actionID = "abc123";
        final String targetFile = MatlabBuilderConstants.TEST_RESULTS_VIEW_ARTIFACT + actionID + ".json";
        FilePath artifactRoot = new FilePath(build.getRootDir());
        copyFileInWorkspace("testArtifacts/t1/windows/" + MatlabBuilderConstants.TEST_RESULTS_VIEW_ARTIFACT + ".json",targetFile,artifactRoot);
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
