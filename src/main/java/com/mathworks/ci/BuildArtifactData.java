package com.mathworks.ci;

public class BuildArtifactData {

  private String taskName;
  private String taskDuration;
  private String taskStatus;

  private String description;

  /*public BuildArtifactData(String taskName, String taskDuration, String taskStatus, boolean skipped) {
    this.taskName = taskName;
    this.taskDuration = taskDuration;
    this.taskStatus = taskStatus;
    this.skipped = skipped;
  }*/

  public BuildArtifactData() {
  }


  public String getTaskDuration() {
    return this.taskDuration;
  }

  public String getTaskName() {
    return this.taskName;
  }

  public String getTaskStatus() {
    return this.taskStatus;
  }

  public String getDescription(){
    return this.description;
  }


  public void setTaskDuration(String taskDuration) {
    this.taskDuration = taskDuration;
  }

  public void setTaskName(String taskName) {
    this.taskName = taskName;
  }

  public void setTaskStatus(String taskStatus) {
    this.taskStatus = taskStatus;
  }

  public void setTaskDescription(String description) {
    this.description = description;
  }
}
