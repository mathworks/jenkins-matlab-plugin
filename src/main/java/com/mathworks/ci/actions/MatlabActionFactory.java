package com.mathworks.ci.actions;

/**
 * Copyright 2024, The MathWorks Inc.
 */

import java.io.Serializable;
import java.io.IOException;
import com.mathworks.ci.parameters.*;

public class MatlabActionFactory implements Serializable {
    public RunMatlabCommandAction createAction(CommandActionParameters params)
            throws IOException, InterruptedException {
        return new RunMatlabCommandAction(params);
    }

    public RunMatlabBuildAction createAction(BuildActionParameters params) throws IOException, InterruptedException {
        return new RunMatlabBuildAction(params);
    }

    public RunMatlabTestsAction createAction(TestActionParameters params) throws IOException, InterruptedException {
        return new RunMatlabTestsAction(params);
    }
}
