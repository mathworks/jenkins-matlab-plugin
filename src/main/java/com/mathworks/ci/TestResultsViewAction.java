package com.mathworks.ci;

import hudson.FilePath;
import jenkins.model.RunAction2;
import hudson.model.Run;
import hudson.model.TaskListener;

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
    private String id;
    private int totalCount;
    private int passedCount;
    private int failedCount;
    private int incompleteCount;
    private int notRunCount;
    public TaskListener listener;
    // private List<TestFile> artidactData;

    public TestResultsViewAction(Run<?, ?> build, String id, FilePath workspace, TaskListener listener) throws InterruptedException, IOException {
        this.build = build;
        this.id = id;
        
        // check again at last
        totalCount = 0;
        passedCount = 0;
        failedCount = 0;
        incompleteCount = 0;
        notRunCount = 0;
        this.workspace = workspace;
        this.listener = listener;

        try{
            getTestResults();
            // listener.getLogger().println("Hello after test results");
        } catch (InterruptedException | IOException e) {
            e.printStackTrace();
            throw e;
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    public List<TestFile> getTestResults() throws ParseException, InterruptedException, IOException {
        List<TestFile> testResults = new ArrayList<TestFile>();
        FilePath fl = new FilePath(new File(build.getRootDir().getAbsolutePath() + File.separator + MatlabBuilderConstants.TEST_RESULTS_VIEW_ARTIFACT + this.id + ".json"));
        try (InputStreamReader reader = new InputStreamReader(new FileInputStream(new File(fl.toURI())), "UTF-8")) {
            Object obj = new JSONParser().parse(reader);
            JSONObject jsonTestArtifact = (JSONObject) obj;
            JSONArray jsonTestResult = (JSONArray) jsonTestArtifact.get("TestResult");
            JSONArray jsonTestSuite = (JSONArray) jsonTestArtifact.get("TestSuite");

            Iterator<JSONObject> testResultIterator = jsonTestResult.iterator();
            Iterator<JSONObject> testSuiteIterator = jsonTestSuite.iterator();
            Map<String, TestFile> map = new HashMap<String, TestFile>();

            totalCount = jsonTestResult.size();
            passedCount = 0;
            failedCount = 0;
            incompleteCount = 0;
            notRunCount = totalCount;

            // add single element test case

            while(testResultIterator.hasNext()) {
                JSONObject test = testResultIterator.next();
                JSONObject suite = testSuiteIterator.next();

                // Not OS dependent
                String[] testNameSplit = test.get("Name").toString().split("/");
                String testFileName = testNameSplit[0];
                String testCaseName = testNameSplit[1];

                // String testCaseName = test.get("Name").toString().split("/")[1];
                // String testFileName;
                // if (suite.get("TestClass") instanceof String) {
                //     testFileName = (String) suite.get("TestClass");
                // }
                // else{
                //     testFileName = testCaseName;
                // }
                String baseFolder = (String) suite.get("BaseFolder");

                // handle same test file name
                TestFile testFile = map.get(baseFolder + File.separator + testFileName);
                if(testFile == null) {
                    testFile = new TestFile();
                    testFile.setName(testFileName);

                    map.put(baseFolder + File.separator + testFileName, testFile);
                    testResults.add(testFile);
                }

                // Calculate the relative path
                Path path1 = Paths.get(baseFolder);
                Path path2 = Paths.get(this.workspace.toURI());
                Path filePath = path2.relativize(path1);
                testFile.setFilePath(this.workspace.getName() + File.separator + filePath.toString());

                // re-evaluate casting if necessary or not
                TestCase testCase = new TestCase();
                testCase.setName(testCaseName);
                testCase.setPassed((boolean) test.get("Passed"));
                testCase.setFailed((boolean) test.get("Failed"));
                testCase.setIncomplete((boolean) test.get("Incomplete"));
                if (test.get("Duration") instanceof Long) {
                    testCase.setDuration(((Long) test.get("Duration")).doubleValue());
                } else if (test.get("Duration") instanceof Double) {
                    testCase.setDuration(((Double) test.get("Duration")));
                }
                testCase.updateStatus();

                // should we instead check for non-empty?
                Object diagnostics = ((JSONObject)test.get("Details")).get("DiagnosticRecord");
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
        }
        catch (Exception e) {
            // TODO: handle exception
            this.listener.getLogger().println(e.getMessage());
        }

        // for(TestFile testFile : testResults) {
        //     for(TestCase testCase : testFile.getTestCases()) {
        //         this.listener.getLogger().println(testCase.getName());
        //     }
        // }

        return testResults;
    }

    private void updateCount(TestCase testCase) {
        if (!testCase.getStatus().equals("NotRun")) {
            if (testCase.getPassed()) {
                passedCount += 1;
            }
            else if (testCase.getFailed()) {
                failedCount += 1;
            }
            else if (testCase.getIncomplete()) {
                incompleteCount += 1;
            }
            notRunCount -= 1;
        }
    }

    // private TestFile checkIfFileExists(List<TestFile> testResults, String fileName){
    //     for(int i = 0; i < testResults.size(); i++){
    //         if(testResults.get(i).getName().equals(fileName)){
    //             return testResults.get(i);
    //         }
    //     }

    //     return null;
    // }

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
        return "matlabTestResults" + this.id;
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

    public String getId() {
        return id;
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