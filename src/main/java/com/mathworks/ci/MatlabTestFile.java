package com.mathworks.ci;

/**
 * Copyright 2025, The MathWorks Inc.
 *
 * Class to store MATLAB test file information
 * 
 */

import java.util.List;
import java.math.BigDecimal;
import java.util.ArrayList;

import org.apache.commons.lang.RandomStringUtils;

import com.mathworks.ci.TestResultsViewAction.TestStatus;

public class MatlabTestFile {
    private String path;
    private String name;
    private BigDecimal duration;
    private TestStatus status;
    private List<MatlabTestCase> matlabTestCases;
    private String id;

    public MatlabTestFile(String name) {
        this.name = name;
        this.path = "";
        this.duration = new BigDecimal("0.0");
        this.status = TestStatus.NOT_RUN;
        this.matlabTestCases = new ArrayList<MatlabTestCase>();
        this.id = RandomStringUtils.randomAlphanumeric(8);
    }

    private void incrementDuration(BigDecimal matlabTestCaseDuration) {
        this.duration = this.duration.add(matlabTestCaseDuration);
    }

    private void updateStatus(MatlabTestCase matlabTestCase) {
        if (!this.status.equals(TestStatus.FAILED)) {
            if (matlabTestCase.getStatus().equals(TestStatus.FAILED)){
                this.status = TestStatus.FAILED;
            }
            else if (!this.status.equals(TestStatus.INCOMPLETE)){
                if (matlabTestCase.getStatus().equals(TestStatus.INCOMPLETE)){
                    this.status = TestStatus.INCOMPLETE;
                }
                else if (matlabTestCase.getStatus().equals(TestStatus.PASSED)){
                    this.status = TestStatus.PASSED;
                }
            }
        }
    }

    public void addTestCase(MatlabTestCase matlabTestCase){
        this.incrementDuration(matlabTestCase.getDuration());
        this.updateStatus(matlabTestCase);
        this.getMatlabTestCases().add(matlabTestCase);
    }

    public String getPath() {
        return this.path;
    }

    public void setPath(String path) {
        this.path = path;
    }
    
    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public BigDecimal getDuration() {
        return this.duration;
    }

    public void setDuration(BigDecimal duration) {
        this.duration = duration;
    }

    public TestStatus getStatus() {
        return this.status;
    }

    public void setStatus(TestStatus status) {
        this.status = status;
    }

    public List<MatlabTestCase> getMatlabTestCases() {
        return this.matlabTestCases;
    }

    public void setMatlabTestCases(List<MatlabTestCase> matlabTestCases) {
        this.matlabTestCases = matlabTestCases;
    }

    public String getId() {
        return this.id;
    }
}
