package com.mathworks.ci;

/**
 * Copyright 2025, The MathWorks Inc.
 *
 * Class to store MATLAB test diagnostics information
 * 
 */

import org.apache.commons.lang.RandomStringUtils;

public class MatlabTestDiagnostics {
    private String event;
    private String report;
    private String id;

    public MatlabTestDiagnostics() {
        this.event = "";
        this.report = "";
        this.id = RandomStringUtils.randomAlphanumeric(8);
    }

    public String getEvent() {
        return this.event;
    }

    public void setEvent(String event) {
        this.event = event;
    }

    public String getReport() {
        return this.report;
    }

    public void setReport(String report) {
        this.report = report;
    }

    public String getId() {
        return this.id;
    }
}
