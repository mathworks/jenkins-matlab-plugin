package com.mathworks.ci.pipeline;

/**
 * Copyright 2020-2024 The MathWorks, Inc.
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

public class RunMatlabCommandStep extends Step implements Serializable {

    private static final long serialVersionUID = 1L;

    private String command;
    private String startupOptions = "";

    @DataBoundConstructor
    public RunMatlabCommandStep(String command) {
        this.command = command;
    }

    public String getCommand() {
        return this.command;
    }

    @DataBoundSetter
    public void setStartupOptions(String startupOptions) {
        this.startupOptions = startupOptions;
    }

    public String getStartupOptions() {
        return Util.fixNull(this.startupOptions);
    }

    @Override
    public StepExecution start(StepContext context) throws Exception {
        return new MatlabCommandStepExecution(context, this);
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
            return Message.getValue("matlab.command.build.step.name");
        }

        @Override
        public String getDisplayName() {
            return Message.getValue("matlab.command.step.display.name");
        }
    }
}
