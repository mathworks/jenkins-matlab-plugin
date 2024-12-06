package com.mathworks.ci.pipeline;

/**
 * Copyright 2022-2024 The MathWorks, Inc.
 */

import java.io.Serializable;
import java.util.Set;
import org.jenkinsci.plugins.workflow.steps.Step;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepDescriptor;
import org.jenkinsci.plugins.workflow.steps.StepExecution;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import com.google.common.collect.ImmutableSet;
import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.Util;

import com.mathworks.ci.Message;

public class RunMatlabBuildStep extends Step implements Serializable {

    private static final long serialVersionUID = 1L;

    private String tasks;
    private String startupOptions;
    private String buildOptions;

    @DataBoundConstructor
    public RunMatlabBuildStep() {

    }

    public String getTasks() {
        return Util.fixNull(tasks);
    }

    public String getStartupOptions() {
        return Util.fixNull(startupOptions);
    }

    public String getBuildOptions() {
        return Util.fixNull(buildOptions);
    }

    @DataBoundSetter
    public void setTasks(String tasks) {
        this.tasks = tasks;
    }

    @DataBoundSetter
    public void setStartupOptions(String startupOptions) {
        this.startupOptions = startupOptions;
    }

    @DataBoundSetter
    public void setBuildOptions(String buildOptions) {
        this.buildOptions = buildOptions;
    }

    @Override
    public StepExecution start(StepContext context) throws Exception {
        return new MatlabBuildStepExecution(context, this);
    }

    @Extension
    public static class CommandStepDescriptor extends StepDescriptor {

        @Override
        public Set<? extends Class<?>> getRequiredContext() {
            return ImmutableSet.of(TaskListener.class, FilePath.class, Launcher.class,
                    EnvVars.class, Run.class);
        }

        @Override
        public String getFunctionName() {
            return Message.getValue("matlab.build.build.step.name");
        }

        @Override
        public String getDisplayName() {
            return Message.getValue("matlab.build.step.display.name");
        }
    }
}
