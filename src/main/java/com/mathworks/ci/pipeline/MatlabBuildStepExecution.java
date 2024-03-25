package com.mathworks.ci.pipeline;

/**
 * Copyright 2022-2024 The MathWorks, Inc.
 *  
 */

import java.io.IOException;

import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.SynchronousNonBlockingStepExecution;
import hudson.model.Result;

import com.mathworks.ci.actions.MatlabActionFactory;
import com.mathworks.ci.actions.RunMatlabBuildAction;
import com.mathworks.ci.parameters.BuildActionParameters;

public class MatlabBuildStepExecution extends SynchronousNonBlockingStepExecution<Void> {
    
    private static final long serialVersionUID = 4771831219402275744L;

    private BuildActionParameters params;
    private MatlabActionFactory factory;
    public MatlabBuildStepExecution(MatlabActionFactory factory, StepContext context, String tasks, String startupOptions, String buildOptions) throws IOException, InterruptedException {
        super(context);

        this.params = new BuildActionParameters(context, startupOptions, tasks, buildOptions);
        this.factory = factory;
    }
    
    public MatlabBuildStepExecution(StepContext context, String tasks, String startupOptions, String buildOptions) throws IOException, InterruptedException {
        this(new MatlabActionFactory(), context, tasks, startupOptions, buildOptions);
    }

    public BuildActionParameters getParameters() {
        return this.params;
    }

    @Override
    public Void run() throws Exception {
        RunMatlabBuildAction action = factory.createAction(params);
        try {
            action.run();
        } catch (Exception e) {
            // throw an exception if return code is non-zero
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
