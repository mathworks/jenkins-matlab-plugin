package com.mathworks.ci.freestyle;

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

import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.model.Result;

import com.mathworks.ci.freestyle.options.StartupOptions;
import com.mathworks.ci.MatlabExecutionException;
import com.mathworks.ci.actions.MatlabActionFactory;
import com.mathworks.ci.actions.RunMatlabCommandAction;
import com.mathworks.ci.parameters.CommandActionParameters;

@RunWith(MockitoJUnitRunner.class)
public class RunMatlabCommandBuilderUnitTest {
    @Mock
    MatlabActionFactory factory;

    @Mock
    RunMatlabCommandAction action;

    @Mock
    Run build;

    @Mock
    Launcher launcher;

    @Mock
    TaskListener listener;

    @Mock
    FilePath workspace;

    @Before
    public void setup() throws IOException, InterruptedException {
        doReturn(action).when(factory).createAction(any(CommandActionParameters.class));
    }

    @Test
    public void shouldHandleNullCases() throws IOException, InterruptedException, MatlabExecutionException {
        RunMatlabCommandBuilder builder = new RunMatlabCommandBuilder(factory);

        builder.perform(build, workspace, launcher, listener);

        ArgumentCaptor<CommandActionParameters> captor = ArgumentCaptor.forClass(CommandActionParameters.class);
        verify(factory).createAction(captor.capture());

        CommandActionParameters actual = captor.getValue();

        assertEquals("", actual.getStartupOptions());
        assertEquals(null, actual.getCommand());
        verify(action).run();
    }

    @Test
    public void shouldHandleMaximalCases() throws IOException, InterruptedException, MatlabExecutionException {
        RunMatlabCommandBuilder builder = new RunMatlabCommandBuilder(factory);
        builder.setMatlabCommand("SHAKE");
        builder.setStartupOptions(new StartupOptions("-nojvm -logfile mylog"));

        builder.perform(build, workspace, launcher, listener);

        ArgumentCaptor<CommandActionParameters> captor = ArgumentCaptor.forClass(CommandActionParameters.class);
        verify(factory).createAction(captor.capture());

        CommandActionParameters actual = captor.getValue();

        assertEquals("-nojvm -logfile mylog", actual.getStartupOptions());
        assertEquals("SHAKE", actual.getCommand());
        verify(action).run();
    }

    @Test
    public void shouldMarkFailureWhenActionFails() throws IOException, InterruptedException, MatlabExecutionException {
        RunMatlabCommandBuilder builder = new RunMatlabCommandBuilder(factory);

        doThrow(new MatlabExecutionException(12)).when(action).run();

        builder.perform(build, workspace, launcher, listener);

        verify(build).setResult(Result.FAILURE);
    }
}
