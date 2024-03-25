package com.mathworks.ci.pipeline;

/**
 * Copyright 2023-2024 The MathWorks, Inc.
 *  
 */

import java.io.IOException;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.SynchronousNonBlockingStepExecution;
import hudson.model.Result;

import com.mathworks.ci.actions.MatlabActionFactory;
import com.mathworks.ci.parameters.RunActionParameters;
import com.mathworks.ci.actions.RunMatlabCommandAction;

public class MatlabCommandStepExecution extends SynchronousNonBlockingStepExecution<Void> {
    
    private static final long serialVersionUID = 1957239693658914450L;
    
    private String command;
    private String startupOptions;

    private RunActionParameters params;

    private MatlabActionFactory factory;

    public MatlabCommandStepExecution(MatlabActionFactory factory, StepContext context, String command, String startupOptions) throws IOException, InterruptedException {
        super(context);

        this.params = new RunActionParameters(context, startupOptions, command);
        this.factory = factory;
    }

    public MatlabCommandStepExecution(StepContext context, String command, String startupOptions) throws IOException, InterruptedException {
        this(new MatlabActionFactory(), context, command, startupOptions);
    }

    public RunActionParameters getParameters() {
        return this.params;
    }

    @Override
    public Void run() throws Exception {
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
