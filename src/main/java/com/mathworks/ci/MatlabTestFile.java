package com.mathworks.ci;

/**
 * Copyright 2024, The MathWorks Inc.
 *
 * Class to store MATLAB test file information
 * 
 */

import java.util.List;
import java.util.ArrayList;

import org.apache.commons.lang.RandomStringUtils;

public class MatlabTestFile {
    private String path;
    private String name;
    private Double duration;
    private String status;
    private List<MatlabTestCase> matlabTestCases;
    private String id;

    public MatlabTestFile() {
        this.path = "";
        this.name = "";
        this.duration = 0.0;
        this.status = MatlabBuilderConstants.NOT_RUN;
        this.matlabTestCases = new ArrayList<MatlabTestCase>();
        this.id = RandomStringUtils.randomAlphanumeric(8);
    }

    public void incrementDuration(Double matlabTestCaseDuration) {
        this.duration += matlabTestCaseDuration;
    }

    public void updateStatus(MatlabTestCase matlabTestCase) {
        if (!this.status.equals(MatlabBuilderConstants.FAILED)) {
            if (matlabTestCase.getFailed()){
                this.status = MatlabBuilderConstants.FAILED;
            }
            else if (!this.status.equals(MatlabBuilderConstants.INCOMPLETE)){
                if (matlabTestCase.getIncomplete()){
                    this.status = MatlabBuilderConstants.INCOMPLETE;
                }
                else if (matlabTestCase.getPassed()){
                    this.status = MatlabBuilderConstants.PASSED;
                }
            }
        }
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
