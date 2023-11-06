package com.mathworks.ci;

public class BuildArtifactData {

    private String taskName;
    private String taskDuration;
    private String taskStatus;

    private String description;
    private String skipped;

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

    public String getTaskSkipped() {
        return this.skipped;
    }

    public void setTaskSkipped(String skipped) {
        this.skipped = skipped;
    }

    public String getTaskStatus() {
        return this.taskStatus;
    }

    public void setTaskStatus(String taskStatus) {
        this.taskStatus = taskStatus;
    }

    public String getDescription() {
        return this.description;
    }

    public void setTaskDescription(String description) {
        this.description = description;
    }
}
