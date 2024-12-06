package com.mathworks.ci.pipeline;

/**
 * Copyright 2023-2024 The MathWorks, Inc.
 */

import java.io.IOException;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.SynchronousNonBlockingStepExecution;
import hudson.model.Result;

import com.mathworks.ci.actions.MatlabActionFactory;
import com.mathworks.ci.parameters.CommandActionParameters;
import com.mathworks.ci.actions.RunMatlabCommandAction;

public class MatlabCommandStepExecution extends SynchronousNonBlockingStepExecution<Void> {

    private static final long serialVersionUID = 1957239693658914450L;

    private MatlabActionFactory factory;
    private RunMatlabCommandStep step;

    public MatlabCommandStepExecution(MatlabActionFactory factory, StepContext context, RunMatlabCommandStep step)
            throws IOException, InterruptedException {
        super(context);

        this.factory = factory;
        this.step = step;
    }

    public MatlabCommandStepExecution(StepContext context, RunMatlabCommandStep step)
            throws IOException, InterruptedException {
        this(new MatlabActionFactory(), context, step);
    }

    @Override
    public Void run() throws Exception {
        CommandActionParameters params = new CommandActionParameters(
                getContext(),
                step.getStartupOptions(),
                step.getCommand());
        RunMatlabCommandAction action = factory.createAction(params);

        try {
            action.run();
        } catch (Exception e) {
            stop(e);
        }

        getContext().setResult(Result.SUCCESS);
        return null;
    }

    @Override
    public void stop(Throwable cause) throws Exception {
        getContext().onFailure(cause);
    }
}
