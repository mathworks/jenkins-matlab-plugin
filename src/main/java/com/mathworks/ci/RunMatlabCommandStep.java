package com.mathworks.ci;

/**
 * Copyright 2020 The MathWorks, Inc.
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

public class RunMatlabCommandStep extends Step {

    private EnvVars env;
    private String command;

    @DataBoundConstructor
    public RunMatlabCommandStep(String command) {
        this.command = command;

    }


    public String getCommand() {
        return this.command;
    }

    private String getMatlabCommand() {
        return this.env == null ? getCommand() : this.env.expand(getCommand());
    }

    public void setEnv(EnvVars env) {
        this.env = env;
    }

    public EnvVars getEnv() {
        return this.env;
    }

    @Override
    public StepExecution start(StepContext context) throws Exception {
        return new MatlabStepExecution(context, getMatlabCommand());
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
    }
}


