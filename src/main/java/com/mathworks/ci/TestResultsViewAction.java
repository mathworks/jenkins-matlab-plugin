package com.mathworks.ci;

import hudson.FilePath;
import jenkins.model.RunAction2;
import hudson.model.Run;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.annotation.CheckForNull;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class TestResultsViewAction implements RunAction2 {
    private Run<?, ?> build;
    private FilePath workspace;
    private String actionID;
    private int totalCount;
    private int passedCount;
    private int failedCount;
    private int incompleteCount;
    private int notRunCount;

    public TestResultsViewAction(Run<?, ?> build, FilePath workspace, String actionID) throws InterruptedException, IOException {
        this.build = build;
        this.workspace = workspace;
        this.actionID = actionID;
        
        // check again at last
        totalCount = 0;
        passedCount = 0;
        failedCount = 0;
        incompleteCount = 0;
        notRunCount = 0;

        try{
            getTestResults();
        } catch (InterruptedException | IOException e) {
            e.printStackTrace();
            throw e;
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    public List<List<TestFile>> getTestResults() throws ParseException, InterruptedException, IOException {
        List<List<TestFile>> testResults = new ArrayList<>();
        FilePath fl = new FilePath(new File(build.getRootDir().getAbsolutePath() + File.separator + MatlabBuilderConstants.TEST_RESULTS_VIEW_ARTIFACT + this.actionID + ".json"));
        try (InputStreamReader reader = new InputStreamReader(new FileInputStream(new File(fl.toURI())), "UTF-8")) {
            Object obj = new JSONParser().parse(reader);

            totalCount = 0;
            passedCount = 0;
            failedCount = 0;
            incompleteCount = 0;
            notRunCount = totalCount;

            JSONArray jsonTestArtifact = (JSONArray) obj;
            Iterator<JSONArray> testArtifactIterator = jsonTestArtifact.iterator();

            while(testArtifactIterator.hasNext()){
                JSONArray jsonTestSessionResults = testArtifactIterator.next();
                Iterator<JSONObject> testSessionIterator = jsonTestSessionResults.iterator();

                List<TestFile> testSessionResults = new ArrayList<>();
                Map<String, TestFile> map = new HashMap<>();
                while(testSessionIterator.hasNext()){
                    JSONObject jsonTestCase = testSessionIterator.next();
                    String baseFolder = jsonTestCase.get("BaseFolder").toString();
                    JSONObject testCaseResult = (JSONObject) jsonTestCase.get("TestResult");

                    // add single element test case

                    // Not OS dependent
                    String[] testNameSplit = testCaseResult.get("Name").toString().split("/");
                    String testFileName = testNameSplit[0];
                    String testCaseName = testNameSplit[1];

                    // handle same test file name
                    TestFile testFile = map.get(baseFolder + File.separator + testFileName);
                    if(testFile == null) {
                        testFile = new TestFile();
                        testFile.setName(testFileName);

                        map.put(baseFolder + File.separator + testFileName, testFile);
                        testSessionResults.add(testFile);
                    }

                    // Calculate the relative path
                    Path path1 = Paths.get(baseFolder);
                    Path path2 = Paths.get(this.workspace.toURI());
                    Path filePath = path2.relativize(path1);
                    testFile.setFilePath(this.workspace.getName() + File.separator + filePath.toString());

                    // re-evaluate casting if necessary or not
                    TestCase testCase = new TestCase();
                    testCase.setName(testCaseName);
                    testCase.setPassed((boolean) testCaseResult.get("Passed"));
                    testCase.setFailed((boolean) testCaseResult.get("Failed"));
                    testCase.setIncomplete((boolean) testCaseResult.get("Incomplete"));
                    if (testCaseResult.get("Duration") instanceof Long) {
                        testCase.setDuration(((Long) testCaseResult.get("Duration")).doubleValue());
                    } else if (testCaseResult.get("Duration") instanceof Double) {
                        testCase.setDuration(((Double) testCaseResult.get("Duration")));
                    }
                    testCase.updateStatus();

                    // should we instead check for non-empty?
                    Object diagnostics = ((JSONObject)testCaseResult.get("Details")).get("DiagnosticRecord");
                    if(diagnostics instanceof JSONObject) {
                        TestDiagnostics testDiagnostics = new TestDiagnostics();
                        testDiagnostics.setEvent(((JSONObject)diagnostics).get("Event").toString());
                        testDiagnostics.setReport(((JSONObject)diagnostics).get("Report").toString());
                        testCase.updateDiagnostics(testDiagnostics);
                    }
                    else if(diagnostics instanceof JSONArray && ((JSONArray)diagnostics).size() > 0) {
                        // diagnostics = (JSONArray)diagnostics;
                        Iterator<JSONObject> diagnosticsIterator = ((JSONArray)diagnostics).iterator();
                        while(diagnosticsIterator.hasNext()) {
                            JSONObject diagnosticItem = diagnosticsIterator.next();

                            TestDiagnostics testDiagnostics = new TestDiagnostics();
                            testDiagnostics.setEvent(diagnosticItem.get("Event").toString());
                            testDiagnostics.setReport(diagnosticItem.get("Report").toString());
                            testCase.updateDiagnostics(testDiagnostics);
                        }
                    }

                    testFile.incrementDuration(testCase.getDuration());
                    testFile.updateStatus(testCase);
                    testFile.addTestCase(testCase);
                    updateCount(testCase);
                }
                testResults.add(testSessionResults);
            }
        }
        catch (Exception e) {
            // TODO: handle exception
            // throw new IOException(e.getLocalizedMessage());
        }

        return testResults;
    }

    private void updateCount(TestCase testCase) {
        totalCount += 1;
        if (testCase.getStatus().equals("NotRun")) {
            notRunCount += 1;
        }
        else if (testCase.getPassed()) {
            passedCount += 1;
        }
        else if (testCase.getFailed()) {
            failedCount += 1;
        }
        else if (testCase.getIncomplete()) {
            incompleteCount += 1;
        }
    }

    private transient Run run;
    
    public Run getRun() {
        return run;
    }

    @Override
    public void onAttached(Run<?, ?> run) {
        this.run = run;
    }

    @Override
    public void onLoad(Run<?, ?> run) {
        this.run = run;
    }

    @CheckForNull
    @Override
    public String getDisplayName() {
        return "MATLAB Test Results";
    }

    @CheckForNull
    @Override
    public String getUrlName() {
        return "matlabTestResults" + this.actionID;
    }

    @CheckForNull
    @Override
    public String getIconFileName() {
        return "document.png";
    }

    public Run getOwner() {
        return this.build;
    }

    /**
     * @param owner the owner to set
     */
    public void setOwner(Run owner) {
        this.build = owner;
    }

    public FilePath getWorkspace() {
        return this.workspace;
    }

    public String getActionID() {
        return actionID;
    }

    public void setTotalCount(int totalCount) {
        this.totalCount = totalCount;
    }

    public int getTotalCount() {
        return this.totalCount;
    }

    public void setPassedCount(int passedCount) {
        this.passedCount = passedCount;
    }

    public int getPassedCount() {
        return this.passedCount;
    }

    public void setFailedCount(int failedCount) {
        this.failedCount = failedCount;
    }

    public int getFailedCount() {
        return this.failedCount;
    }

    public void setIncompleteCount(int incompleteCount) {
        this.incompleteCount = incompleteCount;
    }

    public int getIncompleteCount() {
        return this.incompleteCount;
    }

    public void setNotRunCount(int notRunCount) {
        this.notRunCount = notRunCount;
    }

    public int getNotRunCount() {
        return this.notRunCount;
    }
}