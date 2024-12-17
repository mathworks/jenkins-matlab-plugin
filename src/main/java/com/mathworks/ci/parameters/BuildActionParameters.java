package com.mathworks.ci.parameters;

/**
 * Copyright 2024 The MathWorks, Inc.
 */

import java.io.IOException;
import hudson.FilePath;
import hudson.EnvVars;
import hudson.Launcher;
import hudson.model.Run;
import hudson.model.TaskListener;
import org.jenkinsci.plugins.workflow.steps.StepContext;

public class BuildActionParameters extends MatlabActionParameters {
    private String tasks;
    private String buildOptions;

    public BuildActionParameters(StepContext context, String startupOpts, String tasks, String buildOpts)
            throws IOException, InterruptedException {
        super(context, startupOpts);
        this.tasks = tasks;
        this.buildOptions = buildOpts;
    }

    public BuildActionParameters(Run<?, ?> build, FilePath workspace, EnvVars env, Launcher launcher,
            TaskListener listener, String startupOpts, String tasks, String buildOptions) {
        super(build, workspace, env, launcher, listener, startupOpts);
        this.tasks = tasks;
        this.buildOptions = buildOptions;
    }

    public String getTasks() {
        return tasks;
    }

    public String getBuildOptions() {
        return buildOptions;
    }
}
