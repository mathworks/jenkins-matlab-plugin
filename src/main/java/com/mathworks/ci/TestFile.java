package com.mathworks.ci;

/**
 * Copyright 2024, The MathWorks Inc.
 *
 */

import java.util.List;
import java.util.ArrayList;

import org.apache.commons.lang.RandomStringUtils;

public class TestFile {
    private String filePath;
    private String name;
    private Double duration;
    private String status;
    private List<TestCase> testCases;

    private String id;

    public TestFile() {
        filePath = "";
        name = "";
        duration = 0.0;
        status = "NotRun";
        testCases = new ArrayList<TestCase>();

        id = RandomStringUtils.randomAlphanumeric(8);
    }

    public String getFilePath() {
        return this.filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }
    
    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Double getDuration() {
        return this.duration;
    }

    public void setDuration(Double duration) {
        this.duration = duration;
    }

    public String getStatus() {
        return this.status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public List<TestCase> getTestCases() {
        return this.testCases;
    }

    public void setTestCases(List<TestCase> testCases) {
        this.testCases = testCases;
    }

    public String getId() {
        // String id = this.filePath + "-" + this.name;
        // return id.replaceAll(File.separator, "-");
        return id;
    }

    public void incrementDuration(Double testCaseDuration) {
        this.duration += testCaseDuration;
    }

    public void updateStatus(TestCase testCase) {
        if (!status.equals("Failed")){
            if (testCase.getFailed()){
                status = "Failed";
            }
            else if (!status.equals("Incomplete")){
                if (testCase.getIncomplete()){
                    status = "Incomplete";
                }
                else if (testCase.getPassed()){
                    status = "Passed";
                }
            }
        }
    }

    public void addTestCase(TestCase testCase) {
        this.testCases.add(testCase);
    }
}
