package com.mathworks.ci.actions;

/**
 * Copyright 2024, The MathWorks Inc.
 */

import java.io.IOException;

import com.mathworks.ci.BuildConsoleAnnotator;
import com.mathworks.ci.MatlabExecutionException;
import com.mathworks.ci.parameters.BuildActionParameters;
import com.mathworks.ci.utilities.MatlabCommandRunner;

import hudson.model.Run;

public class RunMatlabBuildAction extends MatlabAction {
    private BuildActionParameters params;

    public RunMatlabBuildAction(MatlabCommandRunner runner, BuildConsoleAnnotator annotator,
            BuildActionParameters params) {
        super(runner, annotator);
        this.params = params;
    }

    public RunMatlabBuildAction(BuildActionParameters params) throws IOException, InterruptedException {
        this(new MatlabCommandRunner(params),
                new BuildConsoleAnnotator(
                        params.getTaskListener().getLogger(),
                        params.getBuild().getCharset()),
                params);
    }

    public void run() throws IOException, InterruptedException, MatlabExecutionException {
        super.copyBuildPluginsToTemp();
        super.setBuildEnvVars();

        // Redirect output to the build annotator
        runner.redirectStdOut(annotator);

        // Prepare the build tool command
        // TODO: Devise better solution then prepending the command
        // here.
        String command = "addpath('"
                + runner.getTempFolder().getRemote()
                + "'); buildtool";

        if (params.getTasks() != null) {
            command += " " + params.getTasks();
        }

        if (params.getBuildOptions() != null) {
            command += " " + params.getBuildOptions();
        }

        try {
            runner.runMatlabCommand(command);
        } catch (Exception e) {
            this.params.getTaskListener().getLogger()
                    .println(e.getMessage());
            throw (e);
        } finally {
            annotator.forceEol();

            Run<?, ?> build = this.params.getBuild();
            super.teardownAction(build);
        }
    }
}
