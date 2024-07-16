package com.mathworks.ci.parameters;

import java.io.IOException;
import hudson.FilePath;
import hudson.EnvVars;
import hudson.Launcher;
import hudson.model.Run;
import hudson.model.TaskListener;
import org.jenkinsci.plugins.workflow.steps.StepContext;

/**
 * Copyright 2024 The MathWorks, Inc.
 *
 */

public class RunActionParameters extends MatlabActionParameters {
    private String command;

    public RunActionParameters(StepContext context, String startupOpts, String command) throws IOException, InterruptedException {
        super(context, startupOpts);
        this.command = command;
    }

    public RunActionParameters(Run<?, ?> build, FilePath workspace, EnvVars env, Launcher launcher, TaskListener listener, String startupOpts, String command) {
        super(build, workspace, env, launcher, listener, startupOpts);
        this.command = command;
    }

    public String getCommand() {
        return command;
    }
}
