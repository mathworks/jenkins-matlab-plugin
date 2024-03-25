package com.mathworks.ci.pipeline;

/**
 * Copyright 2020-2024 The MathWorks, Inc.
 *  
 */

import java.io.IOException;

import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.SynchronousNonBlockingStepExecution;

import hudson.model.Result;

import com.mathworks.ci.actions.MatlabActionFactory;
import com.mathworks.ci.actions.RunMatlabTestsAction;
import com.mathworks.ci.parameters.TestActionParameters;

public class MatlabRunTestsStepExecution extends SynchronousNonBlockingStepExecution<Void> {

    private static final long serialVersionUID = 6704588180717665100L;
    
    private TestActionParameters params;
    private MatlabActionFactory factory;

    public MatlabRunTestsStepExecution(MatlabActionFactory factory, StepContext context, TestActionParameters params) throws IOException, InterruptedException {
        super(context);

        this.params = params;
        this.factory = factory;
    }

    public MatlabRunTestsStepExecution(StepContext context, TestActionParameters params) throws IOException, InterruptedException {
        this(new MatlabActionFactory(), context, params);
    }

    public TestActionParameters getParameters() {
        return this.params;
    }

    @Override
    public Void run() throws Exception {
        RunMatlabTestsAction action = factory.createAction(params);
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
