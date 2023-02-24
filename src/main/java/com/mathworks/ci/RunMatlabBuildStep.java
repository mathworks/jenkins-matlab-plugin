package com.mathworks.ci;

/**
 * Copyright 2022 The MathWorks, Inc.
 *  
 */

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

public class RunMatlabBuildStep extends Step {

    private String tasks;

    @DataBoundConstructor
    public RunMatlabBuildStep() {
 
    }

    public String getTasks() {
        return Util.fixNull(tasks);
    }

    @DataBoundSetter
    public void setTasks(String tasks) {
        this.tasks = tasks;
    }

    @Override
    public StepExecution start(StepContext context) throws Exception {
        return new MatlabBuildStepExecution(context, getTasks());
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


