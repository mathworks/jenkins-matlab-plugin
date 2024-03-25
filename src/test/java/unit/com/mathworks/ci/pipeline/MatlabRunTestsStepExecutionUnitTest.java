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
import com.mathworks.ci.actions.RunMatlabTestsAction;
import com.mathworks.ci.parameters.TestActionParameters;

@RunWith(MockitoJUnitRunner.class)
public class MatlabRunTestsStepExecutionUnitTest {
    @Mock StepContext context;
    @Mock MatlabActionFactory factory;
    @Mock RunMatlabTestsAction action;
    @Mock TestActionParameters params;
    
    @Before
    public void setup() throws IOException, InterruptedException {
        when(factory.createAction(any(TestActionParameters.class))).thenReturn(action);
    }

    @Test
    public void shouldHandleOnlyCase() throws Exception, IOException, InterruptedException, MatlabExecutionException {
        MatlabRunTestsStepExecution ex = new MatlabRunTestsStepExecution(factory, context, params);

        ArgumentCaptor<TestActionParameters> captor = ArgumentCaptor.forClass(TestActionParameters.class);

        ex.run();

        verify(factory).createAction(captor.capture());

        TestActionParameters params = captor.getValue();
        assertEquals(this.params, params);

        verify(action).run();
    }

    @Test
    public void shouldHandleActionThrowing() throws Exception, IOException, InterruptedException, MatlabExecutionException {
        MatlabRunTestsStepExecution ex = new MatlabRunTestsStepExecution(factory, context, params);

        doThrow(new MatlabExecutionException(12)).when(action).run();

        ex.run();

        verify(context).onFailure(any(MatlabExecutionException.class));
    }
}
