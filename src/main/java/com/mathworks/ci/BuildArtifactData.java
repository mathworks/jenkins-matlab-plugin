package com.mathworks.ci;

/**
 * Copyright 2024 The MathWorks, Inc.
 */

public class BuildArtifactData {

    private String taskName;
    private String taskDuration;
    private boolean taskFailed;

    private String taskDescription;
    private boolean taskSkipped;
    private String skipReason;

    public BuildArtifactData() {
    }

    public String getTaskDuration() {
        return this.taskDuration;
    }

    public void setTaskDuration(String taskDuration) {
        this.taskDuration = taskDuration;
    }

    public String getTaskName() {
        return this.taskName;
    }

    public void setTaskName(String taskName) {
        this.taskName = taskName;
    }

    public boolean getTaskSkipped() {
        return this.taskSkipped;
    }

    public void setTaskSkipped(boolean taskSkipped) {
        this.taskSkipped = taskSkipped;
    }

    public String getSkipReason() {
        return (this.skipReason == null) ? "" : this.skipReason;
    }

    public void setSkipReason(String skipReason) {
        this.skipReason = skipReason;
    }

    public boolean getTaskFailed() {
        return this.taskFailed;
    }

    public void setTaskFailed(boolean taskFailed) {
        this.taskFailed = taskFailed;
    }

    public String getTaskDescription() {
        return this.taskDescription;
    }

    public void setTaskDescription(String taskDescription) {
        this.taskDescription = taskDescription;
    }
}
