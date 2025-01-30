package com.mathworks.ci.pipeline;

/**
 * Copyright 2024, The MathWorks Inc.
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
import com.mathworks.ci.actions.RunMatlabCommandAction;
import com.mathworks.ci.parameters.CommandActionParameters;

@RunWith(MockitoJUnitRunner.class)
public class MatlabCommandStepExecutionUnitTest {
    @Mock
    StepContext context;
    @Mock
    MatlabActionFactory factory;
    @Mock
    RunMatlabCommandAction action;

    @Before
    public void setup() throws IOException, InterruptedException {
        when(factory.createAction(any(CommandActionParameters.class))).thenReturn(action);
    }

    @Test
    public void shouldHandleNullCases() throws Exception, IOException, InterruptedException, MatlabExecutionException {
        MatlabCommandStepExecution ex = new MatlabCommandStepExecution(
                factory,
                context,
                new RunMatlabCommandStep(null));

        ex.run();

        ArgumentCaptor<CommandActionParameters> captor = ArgumentCaptor.forClass(CommandActionParameters.class);
        verify(factory).createAction(captor.capture());

        CommandActionParameters params = captor.getValue();
        assertEquals("", params.getStartupOptions());
        assertEquals(null, params.getCommand());

        verify(action).run();
    }

    @Test
    public void shouldHandleMaximalCases()
            throws Exception, IOException, InterruptedException, MatlabExecutionException {
        RunMatlabCommandStep step = new RunMatlabCommandStep("mycommand");
        step.setStartupOptions("-nojvm -logfile file");

        MatlabCommandStepExecution ex = new MatlabCommandStepExecution(factory, context, step);

        ex.run();

        ArgumentCaptor<CommandActionParameters> captor = ArgumentCaptor.forClass(CommandActionParameters.class);
        verify(factory).createAction(captor.capture());

        CommandActionParameters params = captor.getValue();
        assertEquals("-nojvm -logfile file", params.getStartupOptions());
        assertEquals("mycommand", params.getCommand());

        verify(action).run();
    }

    @Test
    public void shouldHandleActionThrowing()
            throws Exception, IOException, InterruptedException, MatlabExecutionException {
        MatlabCommandStepExecution ex = new MatlabCommandStepExecution(
                factory,
                context,
                new RunMatlabCommandStep(null));

        doThrow(new MatlabExecutionException(12)).when(action).run();

        ex.run();

        verify(context).onFailure(any(MatlabExecutionException.class));
    }
}
