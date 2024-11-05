package com.mathworks.ci;

/**
 * Copyright 2024, The MathWorks Inc.
 *
 * Class to store MATLAB test case information
 * 
 */

import java.util.List;
import java.util.ArrayList;

import org.apache.commons.lang.RandomStringUtils;

public class MatlabTestCase {
    private String name;
    private List<MatlabTestDiagnostics> diagnostics;
    private boolean passed;
    private boolean failed;
    private boolean incomplete;
    private String status;
    private Double duration;
    private String id;

    public MatlabTestCase() {
        this.name = "";
        this.diagnostics = new ArrayList<MatlabTestDiagnostics>();
        this.passed = false;
        this.failed = false;
        this.incomplete = false;
        this.status = MatlabBuilderConstants.NOT_RUN;
        this.duration = 0.0;
        this.id = RandomStringUtils.randomAlphanumeric(8);
    }
    
    public void updateStatus() {
        if (this.failed){
            this.status = MatlabBuilderConstants.FAILED;
        }
        else if (this.incomplete) {
            this.status = MatlabBuilderConstants.INCOMPLETE;
        }
        else if(this.passed) {
            this.status = MatlabBuilderConstants.PASSED;
        }
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<MatlabTestDiagnostics> getDiagnostics() {
        return this.diagnostics;
    }

    public void setDiagnostics(List<MatlabTestDiagnostics> diagnostics) {
        this.diagnostics = diagnostics;
    }

    public boolean getPassed() {
        return this.passed;
    }

    public void setPassed(boolean passed) {
        this.passed = passed;
    }

    public boolean getFailed() {
        return this.failed;
    }

    public void setFailed(boolean failed) {
        this.failed = failed;
    }

    public boolean getIncomplete() {
        return this.incomplete;
    }

    public void setIncomplete(boolean incomplete) {
        this.incomplete = incomplete;
    }

    public String getStatus() {
        return this.status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Double getDuration() {
        return this.duration;
    }

    public void setDuration(Double duration) {
        this.duration = duration;
    }

    public String getId() {
        return this.id;
    }
}