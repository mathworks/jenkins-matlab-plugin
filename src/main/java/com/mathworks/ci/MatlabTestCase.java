package com.mathworks.ci;

/**
 * Copyright 2025, The MathWorks Inc.
 *
 * Class to store MATLAB test case information
 * 
 */

import java.util.List;
import java.util.ArrayList;

import org.apache.commons.lang.RandomStringUtils;

import com.mathworks.ci.TestResultsViewAction.TestStatus;

public class MatlabTestCase {
    private String name;
    private List<MatlabTestDiagnostics> diagnostics;
    private TestStatus status;
    private Double duration;
    private String id;

    public MatlabTestCase(String name) {
        this.name = name;
        this.diagnostics = new ArrayList<MatlabTestDiagnostics>();
        this.status = TestStatus.NOT_RUN;
        this.duration = 0.0;
        this.id = RandomStringUtils.randomAlphanumeric(8);
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

    public TestStatus getStatus() {
        return this.status;
    }

    public void setStatus(TestStatus status) {
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