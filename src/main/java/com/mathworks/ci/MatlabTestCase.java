package com.mathworks.ci;

/**
 * Copyright 2025, The MathWorks Inc.
 *
 * Class to store MATLAB test case information
 * 
 */

import java.util.List;
import java.math.BigDecimal;
import java.util.ArrayList;

import org.apache.commons.lang.RandomStringUtils;

import com.mathworks.ci.TestResultsViewAction.TestStatus;

public class MatlabTestCase {
    private String name;
    private List<MatlabTestDiagnostics> diagnostics;
    private TestStatus status;
    private BigDecimal duration;
    private String id;

    public MatlabTestCase(String name) {
        this.name = name;
        this.diagnostics = new ArrayList<MatlabTestDiagnostics>();
        this.status = TestStatus.NOT_RUN;
        this.duration = new BigDecimal("0.0");
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

    public BigDecimal getDuration() {
        return this.duration;
    }

    public void setDuration(BigDecimal duration) {
        this.duration = duration;
    }

    public String getId() {
        return this.id;
    }
}