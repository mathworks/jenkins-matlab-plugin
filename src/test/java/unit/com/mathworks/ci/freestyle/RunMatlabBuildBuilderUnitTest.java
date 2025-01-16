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
import com.mathworks.ci.freestyle.options.BuildOptions;
import com.mathworks.ci.MatlabExecutionException;
import com.mathworks.ci.actions.MatlabActionFactory;
import com.mathworks.ci.actions.RunMatlabBuildAction;
import com.mathworks.ci.parameters.BuildActionParameters;

@RunWith(MockitoJUnitRunner.class)
public class RunMatlabBuildBuilderUnitTest {
    @Mock
    MatlabActionFactory factory;

    @Mock
    RunMatlabBuildAction action;

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
        doReturn(action).when(factory).createAction(any(BuildActionParameters.class));
    }

    @Test
    public void shouldHandleNullCases() throws IOException, InterruptedException, MatlabExecutionException {
        RunMatlabBuildBuilder builder = new RunMatlabBuildBuilder(factory);

        builder.perform(build, workspace, launcher, listener);

        ArgumentCaptor<BuildActionParameters> captor = ArgumentCaptor.forClass(BuildActionParameters.class);
        verify(factory).createAction(captor.capture());

        BuildActionParameters actual = captor.getValue();

        assertEquals("", actual.getStartupOptions());
        assertEquals(null, actual.getTasks());
        assertEquals(null, actual.getBuildOptions());
        verify(action).run();
    }

    @Test
    public void shouldHandleMaximalCases() throws IOException, InterruptedException, MatlabExecutionException {
        RunMatlabBuildBuilder builder = new RunMatlabBuildBuilder(factory);
        builder.setTasks("laundry sweeping");
        builder.setBuildOptions(new BuildOptions("-continueOnFailure -skip laundry"));
        builder.setStartupOptions(new StartupOptions("-nojvm -logfile mylog"));

        builder.perform(build, workspace, launcher, listener);

        ArgumentCaptor<BuildActionParameters> captor = ArgumentCaptor.forClass(BuildActionParameters.class);
        verify(factory).createAction(captor.capture());

        BuildActionParameters actual = captor.getValue();

        assertEquals("-nojvm -logfile mylog", actual.getStartupOptions());
        assertEquals("laundry sweeping", actual.getTasks());
        assertEquals("-continueOnFailure -skip laundry", actual.getBuildOptions());
        verify(action).run();
    }

    @Test
    public void shouldMarkFailureWhenActionFails() throws IOException, InterruptedException, MatlabExecutionException {
        RunMatlabBuildBuilder builder = new RunMatlabBuildBuilder(factory);

        doThrow(new MatlabExecutionException(12)).when(action).run();

        builder.perform(build, workspace, launcher, listener);

        verify(build).setResult(Result.FAILURE);
    }
}
