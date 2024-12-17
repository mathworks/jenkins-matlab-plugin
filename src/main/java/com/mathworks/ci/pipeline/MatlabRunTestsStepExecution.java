package com.mathworks.ci.pipeline;

/**
 * Copyright 2020-2024 The MathWorks, Inc.
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

    private MatlabActionFactory factory;
    private RunMatlabTestsStep step;

    public MatlabRunTestsStepExecution(MatlabActionFactory factory, StepContext context, RunMatlabTestsStep step)
            throws IOException, InterruptedException {
        super(context);

        this.factory = factory;
        this.step = step;
    }

    public MatlabRunTestsStepExecution(StepContext context, RunMatlabTestsStep step)
            throws IOException, InterruptedException {
        this(new MatlabActionFactory(), context, step);
    }

    @Override
    public Void run() throws Exception {
        TestActionParameters params = new TestActionParameters(
                getContext(),
                step.getStartupOptions(),
                step.getTestResultsPDF(),
                step.getTestResultsTAP(),
                step.getTestResultsJUnit(),
                step.getCodeCoverageCobertura(),
                step.getTestResultsSimulinkTest(),
                step.getModelCoverageCobertura(),
                step.getSelectByTag(),
                step.getLoggingLevel(),
                step.getOutputDetail(),
                step.getUseParallel(),
                step.getStrict(),
                step.getSourceFolder(),
                step.getSelectByFolder());
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
