package com.mathworks.ci.pipeline;

/**
 * Copyright 2024, The MathWorks Inc.
 *
 */

import java.io.IOException;

import org.junit.Test;
import org.junit.Before;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.MockitoJUnitRunner;
import static org.mockito.Mockito.*;

import org.jenkinsci.plugins.workflow.steps.StepContext;

import com.mathworks.ci.MatlabExecutionException;
import com.mathworks.ci.actions.MatlabActionFactory;
import com.mathworks.ci.actions.RunMatlabBuildAction;
import com.mathworks.ci.parameters.BuildActionParameters;

@RunWith(MockitoJUnitRunner.class)
public class MatlabBuildStepExecutionUnitTest {
    @Mock StepContext context;
    @Mock MatlabActionFactory factory;
    @Mock RunMatlabBuildAction action;
    
    @Before
    public void setup() throws IOException, InterruptedException {
        when(factory.createAction(any(BuildActionParameters.class))).thenReturn(action);
    }

    @Test
    public void shouldHandleNullCases() throws Exception, IOException, InterruptedException, MatlabExecutionException {
        MatlabBuildStepExecution ex = new MatlabBuildStepExecution(factory, context, null, null, null);

        ArgumentCaptor<BuildActionParameters> captor = ArgumentCaptor.forClass(BuildActionParameters.class);

        ex.run();

        verify(factory).createAction(captor.capture());

        BuildActionParameters params = captor.getValue();
        assertEquals(null, params.getStartupOptions());
        assertEquals(null, params.getTasks());
        assertEquals(null, params.getBuildOptions());

        verify(action).run();
    }

    @Test
    public void shouldHandleMaximalCases() throws Exception, IOException, InterruptedException, MatlabExecutionException {
        MatlabBuildStepExecution ex = new MatlabBuildStepExecution(factory, context, "mytask", "-nojvm -nodisplay", "-continueOnFailure");

        ex.run();

        ArgumentCaptor<BuildActionParameters> captor = ArgumentCaptor.forClass(BuildActionParameters.class);

        verify(factory).createAction(captor.capture());

        BuildActionParameters params = captor.getValue();
        assertEquals("-nojvm -nodisplay", params.getStartupOptions());
        assertEquals("mytask", params.getTasks());
        assertEquals("-continueOnFailure", params.getBuildOptions());

        verify(action).run();
    }

    @Test
    public void shouldHandleActionThrowing() throws Exception, IOException, InterruptedException, MatlabExecutionException {
        MatlabBuildStepExecution ex = new MatlabBuildStepExecution(factory, context, null, null, null);

        doThrow(new MatlabExecutionException(12)).when(action).run();

        ex.run();

        verify(context).onFailure(any(MatlabExecutionException.class));
    }
}
