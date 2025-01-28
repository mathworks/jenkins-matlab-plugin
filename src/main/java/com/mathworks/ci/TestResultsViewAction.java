package com.mathworks.ci;

/**
 * Copyright 2025, The MathWorks Inc.
 *
 */

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
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

    public enum TestStatus {
        PASSED,
        FAILED,
        INCOMPLETE,
        NOT_RUN
    }

    public TestResultsViewAction(Run<?, ?> build, FilePath workspace, String actionID) throws InterruptedException, IOException {
        this.build = build;
        this.workspace = workspace;
        this.actionID = actionID;
        
        this.totalCount = 0;
        this.passedCount = 0;
        this.failedCount = 0;
        this.incompleteCount = 0;
        this.notRunCount = 0;

         try{
            // Set test results counts
             getTestResults();
         } catch (InterruptedException | IOException e) {
             throw e;
         } catch (ParseException e) {
             e.printStackTrace();
         }
    }

    public List<List<MatlabTestFile>> getTestResults() throws ParseException, InterruptedException, IOException {
        List<List<MatlabTestFile>> testResults = new ArrayList<>();
        FilePath fl = new FilePath(new File(build.getRootDir().getAbsolutePath(), MatlabBuilderConstants.TEST_RESULTS_VIEW_ARTIFACT + this.actionID + ".json"));
        try (InputStreamReader reader = new InputStreamReader(Files.newInputStream(Paths.get(fl.toURI())), StandardCharsets.UTF_8)) {
            this.totalCount = 0;
            this.passedCount = 0;
            this.failedCount = 0;
            this.incompleteCount = 0;
            this.notRunCount = totalCount;

            JSONArray testArtifact = (JSONArray) new JSONParser().parse(reader);
            Iterator<JSONArray> testArtifactIterator = testArtifact.iterator();

            while(testArtifactIterator.hasNext()){
                Object jsonTestSessionResults = testArtifactIterator.next();

                List<MatlabTestFile> testSessionResults = new ArrayList<>();
                Map<String, MatlabTestFile> map = new HashMap<>();

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
        } catch (Exception e) {
            throw new IOException(e.getLocalizedMessage());
        }

        return testResults;
    }

    private void getTestSessionResults(List<MatlabTestFile> testSessionResults, JSONObject jsonTestCase, Map<String, MatlabTestFile> map) throws IOException, InterruptedException {
        FilePath baseFolder = new FilePath(new File(jsonTestCase.get("BaseFolder").toString()));
        JSONObject matlabTestCaseResult = (JSONObject) jsonTestCase.get("TestResult");

        // Not OS dependent
        String[] testNameSplit = matlabTestCaseResult.get("Name").toString().split("/");
        String matlabTestFileName = testNameSplit[0];
        String matlabTestCaseName = testNameSplit[1];

        // Check if test's file was known or not
        MatlabTestFile matlabTestFile = map.get(baseFolder + File.separator + matlabTestFileName);
        if(matlabTestFile == null) {
            matlabTestFile = new MatlabTestFile(matlabTestFileName);

            map.put(baseFolder + File.separator + matlabTestFileName, matlabTestFile);
            testSessionResults.add(matlabTestFile);
        }

        // Calculate the relative path
        Path path1 = Paths.get(baseFolder.toURI());
        Path path2 = Paths.get(this.workspace.toURI());
        Path relPath = path2.relativize(path1);

        matlabTestFile.setPath(this.workspace.getName() + File.separator + relPath.toString());

        MatlabTestCase matlabTestCase = new MatlabTestCase(matlabTestCaseName);
        if (matlabTestCaseResult.get("Duration") instanceof Long) {
            matlabTestCase.setDuration(((Long) matlabTestCaseResult.get("Duration")).doubleValue());
        } else if (matlabTestCaseResult.get("Duration") instanceof Double) {
            matlabTestCase.setDuration(((Double) matlabTestCaseResult.get("Duration")));
        }

        if ((boolean) matlabTestCaseResult.get("Failed")){
            matlabTestCase.setStatus(TestStatus.FAILED);
        }
        else if ((boolean) matlabTestCaseResult.get("Incomplete")) {
            matlabTestCase.setStatus(TestStatus.INCOMPLETE);
        }
        else if((boolean) matlabTestCaseResult.get("Passed")) {
            matlabTestCase.setStatus(TestStatus.PASSED);
        }

        Object diagnostics = ((JSONObject)matlabTestCaseResult.get("Details")).get("DiagnosticRecord");
        if(diagnostics instanceof JSONObject) {
            MatlabTestDiagnostics matlabTestDiagnostics = new MatlabTestDiagnostics();
            matlabTestDiagnostics.setEvent(((JSONObject)diagnostics).get("Event").toString());
            matlabTestDiagnostics.setReport(((JSONObject)diagnostics).get("Report").toString());
            matlabTestCase.getDiagnostics().add(matlabTestDiagnostics);
        }
        else if(diagnostics instanceof JSONArray && ((JSONArray)diagnostics).size() > 0) {
            Iterator<JSONObject> diagnosticsIterator = ((JSONArray)diagnostics).iterator();
            while(diagnosticsIterator.hasNext()) {
                JSONObject diagnosticItem = diagnosticsIterator.next();

                MatlabTestDiagnostics matlabTestDiagnostics = new MatlabTestDiagnostics();
                matlabTestDiagnostics.setEvent(diagnosticItem.get("Event").toString());
                matlabTestDiagnostics.setReport(diagnosticItem.get("Report").toString());
                matlabTestCase.getDiagnostics().add(matlabTestDiagnostics);
            }
        }

        matlabTestFile.addTestCase(matlabTestCase);
        updateCount(matlabTestCase);
    }

    private void updateCount(MatlabTestCase matlabTestCase) {
        this.totalCount += 1;
        if (matlabTestCase.getStatus().equals(TestStatus.NOT_RUN)) {
            this.notRunCount += 1;
        }
        else if (matlabTestCase.getStatus().equals(TestStatus.PASSED)) {
            this.passedCount += 1;
        }
        else if (matlabTestCase.getStatus().equals(TestStatus.FAILED)) {
            this.failedCount += 1;
        }
        else if (matlabTestCase.getStatus().equals(TestStatus.INCOMPLETE)) {
            this.incompleteCount += 1;
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
        return this.build;
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
        return this.actionID;
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