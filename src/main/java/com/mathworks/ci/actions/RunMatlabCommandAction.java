package com.mathworks.ci.actions;

/**
 * Copyright 2024, The MathWorks Inc.
 *
 */

import java.io.IOException;

import com.mathworks.ci.BuildConsoleAnnotator;
import com.mathworks.ci.MatlabBuilderConstants;
import com.mathworks.ci.MatlabExecutionException;
import com.mathworks.ci.parameters.CommandActionParameters;
import com.mathworks.ci.utilities.MatlabCommandRunner;

public class RunMatlabCommandAction extends RunMatlabAction {
    private CommandActionParameters params;

    public RunMatlabCommandAction(MatlabCommandRunner runner, BuildConsoleAnnotator annotator, CommandActionParameters params) {
        super(runner, annotator);
        this.params = params;
    }

    public RunMatlabCommandAction(CommandActionParameters params) throws IOException, InterruptedException {
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

        // Prepare MATLAB command
        String command = "addpath('" 
            + runner.getTempFolder().getRemote()
            + "');" + this.params.getCommand();

        try {
            runner.runMatlabCommand(command);
        } catch (Exception e) {
            this.params.getTaskListener().getLogger()
                .println(e.getMessage());
            throw(e);
        } finally {
            annotator.forceEol();

            // Handle build result
            super.moveArtifactToBuildRoot(this.params.getBuild(), MatlabBuilderConstants.BUILD_ARTIFACT);
        }
    }
}
