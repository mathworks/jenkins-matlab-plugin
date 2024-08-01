package com.mathworks.ci;

import org.apache.commons.lang.RandomStringUtils;

public class TestDiagnostics {
    private String event;
    private String report;

    private String id;

    public TestDiagnostics() {
        event = "";
        report = "";

        id = RandomStringUtils.randomAlphanumeric(8);
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
        // String id = this.filePath + "-" + this.name;
        // return id.replaceAll(File.separator, "-");
        return id;
    }

}
