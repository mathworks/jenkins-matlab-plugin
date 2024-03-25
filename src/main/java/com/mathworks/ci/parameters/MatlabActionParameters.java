package com.mathworks.ci.parameters;

import java.io.IOException;
import java.io.Serializable;
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

public class MatlabActionParameters implements Serializable {
    private transient Run build;
    private FilePath workspace;
    private EnvVars env;
    private transient Launcher launcher;
    private transient TaskListener listener;

    private String startupOptions;

    public MatlabActionParameters(StepContext context, String startupOpts) throws IOException, InterruptedException {
        this.build = context.get(Run.class);
        this.workspace = context.get(FilePath.class);
        this.env = context.get(EnvVars.class);
        this.launcher = context.get(Launcher.class);
        this.listener = context.get(TaskListener.class);
        this.startupOptions = startupOpts;
    }

    public MatlabActionParameters(Run build, FilePath workspace, EnvVars env, Launcher launcher, TaskListener listener, String startupOpts) {
        this.build = build;
        this.workspace = workspace;
        this.env = env;
        this.launcher = launcher;
        this.listener = listener;
        this.startupOptions = startupOpts;
    }

    public Run<?, ?> getBuild() {
        return build;
    }

    public FilePath getWorkspace() {
        return workspace;
    }

    public EnvVars getEnvVars() {
        return env;
    }

    public Launcher getLauncher() {
        return launcher;
    }

    public TaskListener getTaskListener() {
        return listener;
    }

    public String getStartupOptions() {
        return startupOptions;
    }
}
