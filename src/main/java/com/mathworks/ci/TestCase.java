package com.mathworks.ci;

/**
 * Copyright 2024, The MathWorks Inc.
 *
 */

import java.util.List;
import java.util.ArrayList;

import org.apache.commons.lang.RandomStringUtils;

public class TestCase {
    private String name;
    private List<TestDiagnostics> diagnostics;
    private boolean passed;
    private boolean failed;
    private boolean incomplete;
    private String status;
    private Double duration;
    private String id;

    public TestCase() {
        name = "";
        diagnostics = new ArrayList<TestDiagnostics>();
        passed = false;
        failed = false;
        incomplete = false;
        status = "NotRun";
        duration = 0.0;
        id = RandomStringUtils.randomAlphanumeric(8);
    }
    
    public void updateStatus() {
        if (failed){
            status = "Failed";
        }
        else if (incomplete) {
            status = "Incomplete";
        }
        else if(passed) {
            status = "Passed";
        }
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<TestDiagnostics> getDiagnostics() {
        return this.diagnostics;
    }

    public void setDiagnostics(List<TestDiagnostics> diagnostics) {
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
        return id;
    }
}