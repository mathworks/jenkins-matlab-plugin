package com.mathworks.ci;

/**
 * Copyright 2024, The MathWorks Inc.
 *
 */

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

import hudson.FilePath;
import hudson.model.Run;

import jenkins.model.RunAction2;

public class TestResultsViewAction implements RunAction2 {
    private transient Run<?, ?> build;
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
        
        totalCount = 0;
        passedCount = 0;
        failedCount = 0;
        incompleteCount = 0;
        notRunCount = 0;

         try{
            // Set test results counts
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
            totalCount = 0;
            passedCount = 0;
            failedCount = 0;
            incompleteCount = 0;
            notRunCount = totalCount;

            JSONArray testArtifact = (JSONArray) new JSONParser().parse(reader);
            Iterator<JSONArray> testArtifactIterator = testArtifact.iterator();

            while(testArtifactIterator.hasNext()){
                Object jsonTestSessionResults = testArtifactIterator.next();

                List<TestFile> testSessionResults = new ArrayList<>();
                Map<String, TestFile> map = new HashMap<>();

                if(jsonTestSessionResults instanceof JSONArray){
                    JSONArray jsonTestSessionResultsArray = (JSONArray) jsonTestSessionResults;
                    Iterator<JSONObject> testSessionResultsIterator = jsonTestSessionResultsArray.iterator();

                    while(testSessionResultsIterator.hasNext()){
                        JSONObject jsonTestCase = testSessionResultsIterator.next();
                        getTestSessionResults(testSessionResults, jsonTestCase, map);
                    }
                }
                else if(jsonTestSessionResults instanceof JSONObject) {
                    JSONObject jsonTestCase = (JSONObject) jsonTestSessionResults;
                    getTestSessionResults(testSessionResults, jsonTestCase, map);
                }

                testResults.add(testSessionResults);
            }
        }
        catch (Exception e) {
            throw new IOException(e.getLocalizedMessage());
        }

        return testResults;
    }

    private void getTestSessionResults(List<TestFile> testSessionResults, JSONObject jsonTestCase, Map<String, TestFile> map) throws IOException, InterruptedException {
        String baseFolder = jsonTestCase.get("BaseFolder").toString();
        JSONObject testCaseResult = (JSONObject) jsonTestCase.get("TestResult");

        // Not OS dependent
        String[] testNameSplit = testCaseResult.get("Name").toString().split("/");
        String testFileName = testNameSplit[0];
        String testCaseName = testNameSplit[1];

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

        Object diagnostics = ((JSONObject)testCaseResult.get("Details")).get("DiagnosticRecord");
        if(diagnostics instanceof JSONObject) {
            TestDiagnostics testDiagnostics = new TestDiagnostics();
            testDiagnostics.setEvent(((JSONObject)diagnostics).get("Event").toString());
            testDiagnostics.setReport(((JSONObject)diagnostics).get("Report").toString());
            testCase.getDiagnostics().add(testDiagnostics);
        }
        else if(diagnostics instanceof JSONArray && ((JSONArray)diagnostics).size() > 0) {
            Iterator<JSONObject> diagnosticsIterator = ((JSONArray)diagnostics).iterator();
            while(diagnosticsIterator.hasNext()) {
                JSONObject diagnosticItem = diagnosticsIterator.next();

                TestDiagnostics testDiagnostics = new TestDiagnostics();
                testDiagnostics.setEvent(diagnosticItem.get("Event").toString());
                testDiagnostics.setReport(diagnosticItem.get("Report").toString());
                testCase.getDiagnostics().add(testDiagnostics);
            }
        }

        testFile.incrementDuration(testCase.getDuration());
        testFile.updateStatus(testCase);
        testFile.getTestCases().add(testCase);
        updateCount(testCase);
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

    @Override
    public void onAttached(Run<?, ?> run) {
        this.build = run;
    }

    @Override
    public void onLoad(Run<?, ?> run) {
        onAttached(run);
    }

    public Run getBuild() {
        return build;
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