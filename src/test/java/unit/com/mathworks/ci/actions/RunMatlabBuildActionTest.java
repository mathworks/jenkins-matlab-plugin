package com.mathworks.ci.actions;

/**
 * Copyright 2024, The MathWorks Inc.
 *
 */

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;

import org.junit.Test;
import org.junit.Before;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

import org.mockito.Mock;
import org.mockito.InOrder;
import org.mockito.junit.MockitoJUnitRunner;
import static org.mockito.Mockito.*;

import hudson.FilePath;
import hudson.model.Run;
import hudson.model.TaskListener;

import com.mathworks.ci.BuildArtifactAction;
import com.mathworks.ci.BuildConsoleAnnotator;
import com.mathworks.ci.MatlabExecutionException;
import com.mathworks.ci.utilities.MatlabCommandRunner;
import com.mathworks.ci.parameters.BuildActionParameters;

@RunWith(MockitoJUnitRunner.class)
public class RunMatlabBuildActionTest {
    @Mock BuildActionParameters params;
    @Mock BuildConsoleAnnotator annotator;
    @Mock MatlabCommandRunner runner;
    @Mock TaskListener listener;
    @Mock PrintStream out;
    @Mock Run build;

    @Mock FilePath tempFolder;

    private boolean setup = false;
    private RunMatlabBuildAction action;

    @Before
    public void init() {
        if (!setup) {
            setup = true;
            action = new RunMatlabBuildAction(runner, annotator, params);

            when(runner.getTempFolder()).thenReturn(tempFolder);
            when(tempFolder.getRemote()).thenReturn("/path/less/traveled");

            when(params.getWorkspace()).thenReturn(tempFolder);

            when(params.getTaskListener()).thenReturn(listener);
            when(listener.getLogger()).thenReturn(out);

            when(params.getBuild()).thenReturn(build);
        }
    }

    @Test
    public void shouldCopyPluginsToTempDirectory() throws IOException, InterruptedException, MatlabExecutionException {
        action.run();

        String DEFAULT_PLUGIN = 
            "+ciplugins/+jenkins/getDefaultPlugins.m";
        String BUILD_REPORT_PLUGIN = 
            "+ciplugins/+jenkins/BuildReportPlugin.m";
        String TASK_RUN_PROGRESS_PLUGIN = 
            "+ciplugins/+jenkins/TaskRunProgressPlugin.m";

        InOrder inOrder = inOrder(runner);

        inOrder.verify(runner)
            .copyFileToTempFolder(DEFAULT_PLUGIN, DEFAULT_PLUGIN);
        inOrder.verify(runner)
            .copyFileToTempFolder(BUILD_REPORT_PLUGIN, BUILD_REPORT_PLUGIN);
        inOrder.verify(runner)
            .copyFileToTempFolder(TASK_RUN_PROGRESS_PLUGIN, TASK_RUN_PROGRESS_PLUGIN);
    }

    @Test
    public void shouldOverrideDefaultPlugins() throws IOException, InterruptedException, MatlabExecutionException {
        action.run();

        verify(runner).addEnvironmentVariable(
                "MW_MATLAB_BUILDTOOL_DEFAULT_PLUGINS_FCN_OVERRIDE",
                "ciplugins.jenkins.getDefaultPlugins");
    }

    @Test
    public void shouldUseCustomAnnotator() throws IOException, InterruptedException, MatlabExecutionException {
        action.run();

        verify(runner).redirectStdOut(annotator);
    }

    @Test
    public void shouldRunCorrectCommand() throws IOException, InterruptedException, MatlabExecutionException {
        action.run();

        verify(runner).runMatlabCommand("addpath('/path/less/traveled'); buildtool");
    }

    @Test
    public void shouldRunCommandWithTasksAndBuildOptions() throws IOException, InterruptedException, MatlabExecutionException {
        doReturn("dishes groceries").when(params).getTasks();
        doReturn("-continueOnFailure -skip dishes").when(params)
            .getBuildOptions();

        action.run();

        verify(runner).runMatlabCommand(
                "addpath('/path/less/traveled'); "
                + "buildtool dishes groceries "
                + "-continueOnFailure -skip dishes");
    }

    @Test
    public void shouldPrintAndRethrowMessage() throws IOException, InterruptedException, MatlabExecutionException {
        doThrow(new MatlabExecutionException(12)).when(runner).runMatlabCommand(anyString());

        try {
            action.run();
        } catch (MatlabExecutionException e) {
            verify(out).println(e.getMessage());
            assertEquals(12, e.getExitCode());
        };
    }

    @Test
    public void shouldNotAddActionIfNoBuildResult() throws IOException, InterruptedException, MatlabExecutionException {
        action.run();

        verify(build,  never()).addAction(any(BuildArtifactAction.class));
    }

    @Test
    public void shouldCopyBuildResultsToRootAndAddsAction() throws IOException, InterruptedException, MatlabExecutionException {
        File tmp = Files.createTempDirectory("temp").toFile();
        tmp.deleteOnExit();

        File matlab = new File(tmp, ".matlab");
        File json = new File(matlab, "buildArtifact.json");

        matlab.mkdirs();
        json.createNewFile();
        
        doReturn(new FilePath(tmp)).when(params).getWorkspace();
        doReturn(tmp).when(build).getRootDir();

        boolean runTimeException = false;
        try {
            action.run();
        } catch (RuntimeException e) {
            runTimeException = true;
        }

        // Should throw for invalid file
        assertTrue(runTimeException);
        // Should have deleted original file
        assertFalse(json.exists());
        // Should have copied file to root dir
        assertTrue(new File(tmp, "buildArtifact.json").exists());
    }
}
