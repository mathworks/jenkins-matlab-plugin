package com.mathworks.ci;

public class BuildArtifactData {

    private String taskName;
    private String taskDuration;
    private boolean taskStatus;

    private String description;
    private boolean skipped;

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
        return this.skipped;
    }

    public void setTaskSkipped(boolean skipped) {
        this.skipped = skipped;
    }

    public boolean getTaskStatus() {
        return this.taskStatus;
    }

    public void setTaskStatus(boolean taskStatus) {
        this.taskStatus = taskStatus;
    }

    public String getDescription() {
        return this.description;
    }

    public void setTaskDescription(String description) {
        this.description = description;
    }
}
