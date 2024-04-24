package com.mathworks.ci.actions;

/**
 * Copyright 2024, The MathWorks Inc.
 *
 */

import java.io.IOException;
import java.io.PrintStream;

import org.junit.Test;
import org.junit.Before;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import static org.mockito.Mockito.*;

import hudson.model.TaskListener;

import com.mathworks.ci.MatlabExecutionException;
import com.mathworks.ci.utilities.MatlabCommandRunner;
import com.mathworks.ci.parameters.RunActionParameters;

@RunWith(MockitoJUnitRunner.Silent.class)
public class RunMatlabCommandActionTest {
    @Mock RunActionParameters params;
    @Mock MatlabCommandRunner runner;
    @Mock PrintStream out;
    @Mock TaskListener listener;

    private boolean setup = false;
    private RunMatlabCommandAction action;

    // Not using @BeforeClass to avoid static fields.
    @Before
    public void init() {
        if (!setup) {
            setup = true;
            action = new RunMatlabCommandAction(runner, params);
        }
    }

    @Test
    public void runsGivenCommand() throws IOException, InterruptedException, MatlabExecutionException {
        when(params.getCommand()).thenReturn("Sit!");

        action.run();

        verify(runner).runMatlabCommand("Sit!");
    }

    @Test
    public void printsAndRethrowsMessage() throws IOException, InterruptedException, MatlabExecutionException {
        // Falsely flagged as unecessary >:(
        when(params.getTaskListener()).thenReturn(listener);
        when(listener.getLogger()).thenReturn(out);

        doThrow(new MatlabExecutionException(12)).when(runner).runMatlabCommand(anyString());

        try {
            action.run();
        } catch (MatlabExecutionException e) {
            verify(out).println(e.getMessage());
            assertEquals(12, e.getExitCode());
        };
    }
}
