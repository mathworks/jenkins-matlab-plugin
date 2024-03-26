package com.mathworks.ci.actions;

/**
 * Copyright 2024, The MathWorks Inc.
 *
 */

import java.io.IOException;

import com.mathworks.ci.MatlabExecutionException;
import com.mathworks.ci.parameters.RunActionParameters;
import com.mathworks.ci.utilities.MatlabCommandRunner;

public class RunMatlabCommandAction {
    private RunActionParameters params; 
    private MatlabCommandRunner runner;

    public RunMatlabCommandAction(MatlabCommandRunner runner, RunActionParameters params) {
        this.runner = runner;
        this.params = params;
    }

    public RunMatlabCommandAction(RunActionParameters params) throws IOException, InterruptedException {
        this(new MatlabCommandRunner(params), params);
    }

    public void run() throws IOException, InterruptedException, MatlabExecutionException {
        try {
            runner.runMatlabCommand(this.params.getCommand());
        } catch (Exception e) {
            this.params.getTaskListener().getLogger()
                .println(e.getMessage());
            throw(e);
        } 
    }
}
