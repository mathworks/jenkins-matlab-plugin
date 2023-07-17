package com.mathworks.ci;

/**
 * Copyright 2020-2023 The MathWorks, Inc.
 *  
 */

import java.util.Set;
import org.jenkinsci.plugins.workflow.steps.Step;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepDescriptor;
import org.jenkinsci.plugins.workflow.steps.StepExecution;
import org.kohsuke.stapler.DataBoundConstructor;
import com.google.common.collect.ImmutableSet;
import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.Util;

public class RunMatlabCommandStep extends Step {
    
    private String command;
    private String startupOptions;

    @DataBoundConstructor
    public RunMatlabCommandStep(String command, String startupOptions) {
        this.command = command;
        this.startupOptions = startupOptions;
    }


    public String getCommand() {
        return this.command;
    }

    public String getStartupOptions() {
        return Util.fixNull(this.startupOptions);
    }

    @Override
    public StepExecution start(StepContext context) throws Exception {
        return new MatlabCommandStepExecution(context, getCommand(), getStartupOptions());
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


