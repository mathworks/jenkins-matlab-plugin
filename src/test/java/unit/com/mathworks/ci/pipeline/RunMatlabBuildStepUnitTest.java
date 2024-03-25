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

import org.jenkinsci.plugins.workflow.steps.StepContext;

@RunWith(MockitoJUnitRunner.class)
public class RunMatlabBuildStepUnitTest {
    @Mock StepContext context;     

    @Test
    public void shouldHandleNullCase() throws Exception {
        RunMatlabBuildStep step = new RunMatlabBuildStep();
        MatlabBuildStepExecution ex = (MatlabBuildStepExecution)step.start(context);

        assertEquals("", ex.getParameters().getStartupOptions());
        assertEquals("", ex.getParameters().getTasks());
        assertEquals("", ex.getParameters().getBuildOptions());
    }

    @Test
    public void shouldHandleMaximalCase() throws Exception {
        RunMatlabBuildStep step = new RunMatlabBuildStep();
        step.setStartupOptions("-nojvm -logfile file");
        step.setTasks("vacuum bills");
        step.setBuildOptions("-continueOnFailure");

        MatlabBuildStepExecution ex = (MatlabBuildStepExecution)step.start(context);

        assertEquals("-nojvm -logfile file", ex.getParameters().getStartupOptions());
        assertEquals("vacuum bills", ex.getParameters().getTasks());
        assertEquals("-continueOnFailure", ex.getParameters().getBuildOptions());
    }
}
