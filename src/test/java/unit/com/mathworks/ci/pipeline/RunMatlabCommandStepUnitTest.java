package com.mathworks.ci.pipeline;

/**
 * Copyright 2024, The MathWorks Inc.
 *
 */

import java.io.IOException;

import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import static org.mockito.Mockito.*;

import org.jenkinsci.plugins.workflow.steps.StepContext;

@RunWith(MockitoJUnitRunner.class)
public class RunMatlabCommandStepUnitTest {
    @Mock StepContext context;     

    @Test
    public void shouldHandleNullCase() throws Exception {
        RunMatlabCommandStep step = new RunMatlabCommandStep("SPEAK");
        MatlabCommandStepExecution ex = (MatlabCommandStepExecution)step.start(context);

        assertEquals("", ex.getParameters().getStartupOptions());
        assertEquals("SPEAK", ex.getParameters().getCommand());
    }

    @Test
    public void shouldHandleMaximalCase() throws Exception {
        RunMatlabCommandStep step = new RunMatlabCommandStep("ROLL OVER");
        step.setStartupOptions("-nojvm -logfile file");

        MatlabCommandStepExecution ex = (MatlabCommandStepExecution)step.start(context);

        assertEquals("-nojvm -logfile file", ex.getParameters().getStartupOptions());
        assertEquals("ROLL OVER", ex.getParameters().getCommand());
    }
}
