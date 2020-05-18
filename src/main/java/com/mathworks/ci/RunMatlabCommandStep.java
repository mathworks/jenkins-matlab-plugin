package com.mathworks.ci;

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
    private String matlabCommand;
    private static boolean COPY_SCRATCH_FILE = false;

    @DataBoundConstructor
    public RunMatlabCommandStep(String command) {
        this.matlabCommand = command;

    }


    public String getMatlabCommand() {
        return this.matlabCommand;
    }

    private String getCommand() {
        return this.env == null ? getMatlabCommand() : this.env.expand(getMatlabCommand());
    }

    public void setEnv(EnvVars env) {
        this.env = env;
    }

    public EnvVars getEnv() {
        return this.env;
    }

    @Override
    public StepExecution start(StepContext context) throws Exception {
        return new MatlabStepExecution(context, getCommand(), COPY_SCRATCH_FILE);
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


