package com.mathworks.ci.systemtests;

import com.mathworks.ci.MatlabBuildWrapperContent;
import com.mathworks.ci.MatlabInstallationAxis;
import com.mathworks.ci.Message;
import com.mathworks.ci.UseMatlabVersionBuildWrapper;
import com.mathworks.ci.freestyle.RunMatlabBuildBuilder;
import com.mathworks.ci.freestyle.RunMatlabCommandBuilder;
import com.mathworks.ci.freestyle.RunMatlabTestsBuilder;
import hudson.matrix.*;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import hudson.model.Run;
import hudson.slaves.DumbSlave;
import org.htmlunit.html.*;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.*;
import org.jvnet.hudson.test.ExtractResourceSCM;
import org.jvnet.hudson.test.JenkinsRule;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

public class TestResultVisualizationIT {
    private static JenkinsRule.WebClient jenkinsWebClient;

    @Rule
    public JenkinsRule jenkins = new JenkinsRule();


    @BeforeClass
    public static void checkMatlabRoot() {
        // Check if the MATLAB_ROOT environment variable is defined
        String matlabRoot = System.getenv("MATLAB_ROOT");
        Assume.assumeTrue("Not running tests as MATLAB_ROOT environment variable is not defined", matlabRoot != null && !matlabRoot.isEmpty());
    }

    @Before
    public void createJenkinsWebClient(){
        jenkinsWebClient = jenkins.createWebClient();
    }

    // Verify test results are shown in summary
    @Test
    public void verifyTestResultsSummary() throws Exception {
        FreeStyleProject project = jenkins.createFreeStyleProject();
        project.setScm(new ExtractResourceSCM(Utilities.getRunMATLABTestsData()));

        UseMatlabVersionBuildWrapper buildWrapper = new UseMatlabVersionBuildWrapper();
        buildWrapper.setMatlabBuildWrapperContent(new MatlabBuildWrapperContent(Message.getValue("matlab.custom.location"), Utilities.getMatlabRoot()));
        project.getBuildWrappersList().add(buildWrapper);

        // Run tests through Run Command step
        RunMatlabCommandBuilder scriptBuilder = new RunMatlabCommandBuilder();
        scriptBuilder.setMatlabCommand("runtests('IncludeSubfolders', true)");
        project.getBuildersList().add(scriptBuilder);

        // Run tests through Run Build step
        RunMatlabBuildBuilder buildtoolBuilder = new RunMatlabBuildBuilder();
        buildtoolBuilder.setTasks("test");
        project.getBuildersList().add(buildtoolBuilder);

        FreeStyleBuild build = project.scheduleBuild2(0).get();

        // Verify MATLAB Test Result summary
        String[] buildResultSummaries = getTestResultSummaryFromBuildStatusPage(build);
        List.of(buildResultSummaries ).forEach(summary -> {
            assertTrue(summary.contains("Tests run: 4"));
            assertTrue(summary.contains("Passed: 1"));
            assertTrue(summary.contains("Failed: 3"));
            assertTrue(summary.contains("Incomplete: 0"));
            assertTrue(summary.contains("Not Run: 0"));
        });
    }

    @Test
    public void verifyHyperlinkInSummary() throws Exception {
        FreeStyleProject project = jenkins.createFreeStyleProject();
        project.setScm(new ExtractResourceSCM(Utilities.getRunMATLABTestsData()));

        UseMatlabVersionBuildWrapper buildWrapper = new UseMatlabVersionBuildWrapper();
        buildWrapper.setMatlabBuildWrapperContent(new MatlabBuildWrapperContent(Message.getValue("matlab.custom.location"), Utilities.getMatlabRoot()));
        project.getBuildWrappersList().add(buildWrapper);

        // Run tests through Run Build step
        RunMatlabBuildBuilder buildtoolBuilder = new RunMatlabBuildBuilder();
        buildtoolBuilder.setTasks("test");
        project.getBuildersList().add(buildtoolBuilder);

        FreeStyleBuild build = project.scheduleBuild2(0).get();

        String hyperlinkInSummary = getHyperlinkFromBuildStatus(build);

        String hyperlinkOfTestResultsTab = getTestResultTabLinkFromSidePanel(build);

        assertTrue(hyperlinkOfTestResultsTab.contains(hyperlinkInSummary));
    }

    @Test
    public void verifyContentInTestResultsTable() throws Exception{
        FreeStyleProject project = jenkins.createFreeStyleProject();
        project.setScm(new ExtractResourceSCM(Utilities.getRunMATLABTestsData()));

        UseMatlabVersionBuildWrapper buildWrapper = new UseMatlabVersionBuildWrapper();
        buildWrapper.setMatlabBuildWrapperContent(new MatlabBuildWrapperContent(Message.getValue("matlab.custom.location"), Utilities.getMatlabRoot()));
        project.getBuildWrappersList().add(buildWrapper);

        // Run tests through Run Build step
        RunMatlabBuildBuilder buildtoolBuilder = new RunMatlabBuildBuilder();
        buildtoolBuilder.setTasks("test");
        project.getBuildersList().add(buildtoolBuilder);

        FreeStyleBuild build = project.scheduleBuild2(0).get();

        // Verify button text
        String totalTestsButtonText = getButtonText("TOTAL", build);
        assertTrue(totalTestsButtonText.contains("4"));
        assertTrue(totalTestsButtonText.contains("Total Tests"));

        String passedButtonText = getButtonText("PASSED", build);
        assertTrue(passedButtonText.contains("1"));
        assertTrue(passedButtonText.contains("Passed"));

        String failedButtonText = getButtonText("FAILED", build);
        assertTrue(failedButtonText.contains("3"));
        assertTrue(failedButtonText.contains("Failed"));

        String incompleteButtonText = getButtonText("INCOMPLETE", build);
        assertTrue(incompleteButtonText.contains("0"));
        assertTrue(incompleteButtonText.contains("Incomplete"));

        String notRunButtonText = getButtonText("NOT_RUN", build);
        assertTrue(notRunButtonText.contains("0"));
        assertTrue(notRunButtonText.contains("Not Run"));

        // Verify the test filenames shown
        String[] expectedTestFiles = {" testMultiply ", " squareTest ", " testSquare ", " testSum "};
        String[] testFiles = getTestFilesInTable(build);
        assertEquals(testFiles, expectedTestFiles);

        // Verify Diagnostics are shown for a failed test.
        List<String[]> testsContentForAFile = getTestsInfoForATestFile("testMultiply", build);
        assertEquals(testsContentForAFile.size(),1);
        assertEquals(testsContentForAFile.get(0)[0].trim(), "testMultiplication"); // 'testMultiply has 'testMultiplication' test
        assertNotNull(testsContentForAFile.get(0)[1]); // Diagnostics cant be null as 'testMultiply' fails
        assertTrue(testsContentForAFile.get(0)[1].contains("testMultiply/testMultiplication"));
        assertNotNull(testsContentForAFile.get(0)[2]); // Duration cannot be null

        // Verify Diagnostics is empty for passed test
        List<String[]> testsContent= getTestsInfoForATestFile("testSum", build);
        assertEquals(testsContent.size(),1);
        assertEquals(testsContent.get(0)[0].trim(), "testAddition"); // 'testSum has 'testAddition' test
        assertEquals(testsContent.get(0)[1], ""); // No diagnostics as 'testAddition' passes
    }

    // Verify in matrix project
    @Test
    public void verifyTestResultsSummaryInMatrixProject() throws Exception {
        String matlabRoot = System.getenv("MATLAB_ROOT");
        String matlabRoot22b = System.getenv("MATLAB_ROOT_22b");
        Assume.assumeTrue("Not running tests as MATLAB_ROOT_22b environment variable is not defined", matlabRoot22b != null && !matlabRoot22b.isEmpty());

        Utilities.setMatlabInstallation("MATLAB_PATH_1", matlabRoot, jenkins);
        Utilities.setMatlabInstallation("MATLAB_PATH_22b", matlabRoot22b, jenkins);

        MatrixProject matrixProject = jenkins.createProject(MatrixProject.class);
        MatlabInstallationAxis MATLABAxis = new MatlabInstallationAxis(Arrays.asList("MATLAB_PATH_1", "MATLAB_PATH_22b"));
        matrixProject.setAxes(new AxisList(MATLABAxis));
        matrixProject.setScm(new ExtractResourceSCM(Utilities.getRunMATLABTestsData()));

        // Run tests through Run Build step
        RunMatlabBuildBuilder buildtoolBuilder = new RunMatlabBuildBuilder();
        buildtoolBuilder.setTasks("test");
        matrixProject.getBuildersList().add(buildtoolBuilder);

        MatrixBuild build = matrixProject.scheduleBuild2(0).get();

        Combination c = new Combination(new AxisList(new MatlabInstallationAxis(Arrays.asList("MATLAB_PATH_1"))), "MATLAB_PATH_1");
        MatrixRun run = build.getRun(c);
        String[] buildResultSummaries = getTestResultSummaryFromBuildStatusPage(run);
        List.of(buildResultSummaries).forEach(summary -> {
            assertTrue(summary.contains("Tests run: 4"));
            assertTrue(summary.contains("Passed: 1"));
            assertTrue(summary.contains("Failed: 3"));
            assertTrue(summary.contains("Incomplete: 0"));
            assertTrue(summary.contains("Not Run: 0"));
        });

        c = new Combination(new AxisList(new MatlabInstallationAxis(Arrays.asList("MATLAB_PATH_22b"))), "MATLAB_PATH_22b");
        run = build.getRun(c);
        String[] secondBuildResultSummary= getTestResultSummaryFromBuildStatusPage(run);
        assertEquals(secondBuildResultSummary.length,0); // As for R2022b the view is not generated
        jenkins.assertLogContains(matlabRoot22b,run);

        jenkins.assertBuildStatus(Result.FAILURE, run); // As the test task fails
    }

    // Verify in pipeline project
    @Test
    public void verifySummaryInDeclarativePipeline() throws Exception {
        String script = "pipeline {\n" +
                "  agent any\n" +
                Utilities.getEnvironmentDSL() + "\n" +
                "    stages{\n" +
                "        stage('Run MATLAB Command') {\n" +
                "            steps\n" +
                "            {\n" +
                addTestData() + "\n" +
                "              runMATLABBuild('test')"+
                "            }\n" +
                "        }\n" +
                "    }\n" +
                "}";
        WorkflowRun build = getPipelineBuild(script);

        // Verify MATLAB Test Result summary
        String[] buildResultSummaries = getTestResultSummaryFromBuildStatusPage(build);
        List.of(buildResultSummaries).forEach(summary -> {
            assertTrue(summary.contains("Tests run: 4"));
            assertTrue(summary.contains("Passed: 1"));
            assertTrue(summary.contains("Failed: 3"));
            assertTrue(summary.contains("Incomplete: 0"));
            assertTrue(summary.contains("Not Run: 0"));
        });
    }

    // Verify Master Slave
    @Test
    public void verifyPipelineOnSlave() throws Exception {
        DumbSlave s = jenkins.createOnlineSlave();
        String script ="node('!built-in') {" +
                Utilities.getEnvironmentScriptedPipeline() + "\n" +
                addTestData()+"\n" +
                "runMATLABBuild(tasks: 'test') }";

        WorkflowRun build = getPipelineBuild(script);

        // Verify MATLAB Test Result summary
        String[] BuildResultSummary= getTestResultSummaryFromBuildStatusPage(build);
        List.of(BuildResultSummary).forEach(summary -> {
            assertTrue(summary.contains("Tests run: 4"));
            assertTrue(summary.contains("Passed: 1"));
            assertTrue(summary.contains("Failed: 3"));
            assertTrue(summary.contains("Incomplete: 0"));
            assertTrue(summary.contains("Not Run: 0"));
        });

        jenkins.assertLogNotContains("Running on Jenkins", build);
    }


    private String[] getTestResultSummaryFromBuildStatusPage(Run<?, ?> build) throws IOException, SAXException {
        HtmlPage buildPage = jenkinsWebClient.getPage(build);
        List<HtmlElement> summaryElement =  buildPage.getByXPath("//*[starts-with(@id, 'matlabTestResults')]");
        return summaryElement.stream()
                .map(element -> (HtmlElement) element)
                .map(HtmlElement::getTextContent)
                .toArray(String[]::new);
    }

    private String getHyperlinkFromBuildStatus(FreeStyleBuild build) throws IOException, SAXException {
        HtmlPage buildPage = jenkinsWebClient.getPage(build);
        HtmlElement summaryElement = (HtmlElement) buildPage.getByXPath("//*[starts-with(@id, 'matlabTestResults')]").get(0);
        HtmlAnchor anchor = summaryElement.getFirstByXPath(".//a");
        return anchor.getHrefAttribute();
    }

    private String getTestResultTabLinkFromSidePanel(FreeStyleBuild build) throws IOException, SAXException {
        HtmlPage buildPage = jenkinsWebClient.getPage(build);
        HtmlElement jenkinsSidePanelElement  = buildPage.getFirstByXPath("//*[@id='side-panel']/div");
        HtmlElement buildResultTab = (HtmlElement) jenkinsSidePanelElement.getChildNodes().get(5);
        HtmlAnchor href = (HtmlAnchor) buildResultTab.getChildNodes().get(0).getByXPath("//a[span[text()='MATLAB Test Results']]").get(0);
        return href.getHrefAttribute();
    }

    private String getButtonText(String buttonID, FreeStyleBuild build) throws IOException, SAXException {
        String pathToTestResultsTab = getHyperlinkFromBuildStatus(build);
        HtmlPage testResultsTabPage = jenkinsWebClient.getPage(build, pathToTestResultsTab);
        HtmlElement buttonElement = (HtmlElement) testResultsTabPage.getElementById(buttonID);
        return buttonElement.getTextContent();
    }

    private String[] getTestFilesInTable(FreeStyleBuild build) throws IOException, SAXException {
        String pathToTestResultsTab = getHyperlinkFromBuildStatus(build);
        HtmlPage testResultsTabPage = jenkinsWebClient.getPage(build, pathToTestResultsTab);

        HtmlTable tableBodyElement = (HtmlTable) testResultsTabPage.getElementById("testResultsTable");
        // Get all rows from the tbody of the table
        List<HtmlTableRow> rows = tableBodyElement.getBodies().get(0).getRows();

        // Get the test file name from table rows
        return rows.stream()
                .map(row -> (HtmlElement)row.getFirstByXPath(".//div"))
                .map(HtmlElement::getTextContent) // Extract the text content of the <div>
                .toArray(String[]::new);
    }

    private List<String[]> getTestsInfoForATestFile(String testFileName, FreeStyleBuild build) throws IOException, SAXException {
        String pathToTestResultsTab = getHyperlinkFromBuildStatus(build);
        HtmlPage testResultsTabPage = jenkinsWebClient.getPage(build, pathToTestResultsTab);

        HtmlTable mainTable = (HtmlTable) testResultsTabPage.getElementById("testResultsTable");
        HtmlTableBody innerTable = mainTable.getRowById(testFileName).getFirstByXPath(".//tbody[@id='matlabTestCasesTableBody']");
        // Get all rows from the inner table
        List<HtmlTableRow> rows = innerTable.getRows();
        // Use streams to extract test names, diagnostics, and durations
        List<String[]> testDetails = rows.stream()
                .map(row -> {
                    String testName = row.getCell(0).getTextContent(); // Get test name
                    String diagnostics = row.getCell(1).getTextContent(); // Get Diagnostics
                    String duration = row.getCell(2).getTextContent(); // Get Duration
                    return new String[]{testName, diagnostics, duration};
                })
                .collect(Collectors.toList());
        return testDetails;
    }

    private String addTestData() throws MalformedURLException {
        URL zipFile = Utilities.getRunMATLABTestsData();
        String path = "  unzip '" + zipFile.getPath() + "'" + "\n";
        return path;
    }

    private WorkflowRun getPipelineBuild(String script) throws Exception{
        WorkflowJob project = jenkins.createProject(WorkflowJob.class);
        project.setDefinition(new CpsFlowDefinition(script,true));
        return project.scheduleBuild2(0).get();
    }

}
