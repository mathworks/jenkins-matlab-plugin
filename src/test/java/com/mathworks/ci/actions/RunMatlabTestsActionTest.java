package com.mathworks.ci.actions;

/**
 * Copyright 2024, The MathWorks Inc.
 */

import java.io.IOException;
import java.io.PrintStream;

import java.util.ArrayList;

import org.junit.Test;
import org.junit.Before;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;

import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.MockitoJUnitRunner;
import static org.mockito.Mockito.*;

import hudson.FilePath;
import hudson.model.TaskListener;

import com.mathworks.ci.MatlabBuilderConstants;
import com.mathworks.ci.MatlabExecutionException;
import com.mathworks.ci.utilities.MatlabCommandRunner;
import com.mathworks.ci.parameters.TestActionParameters;

@RunWith(MockitoJUnitRunner.class)
public class RunMatlabTestsActionTest {
    @Mock
    TestActionParameters params;
    @Mock
    MatlabCommandRunner runner;
    @Mock
    PrintStream out;
    @Mock
    TaskListener listener;
    @Mock
    FilePath tempFolder;

    private boolean setup = false;
    private RunMatlabTestsAction action;

    // Not using @BeforeClass to avoid static fields.
    @Before
    public void init() throws IOException, InterruptedException {
        if (!setup) {
            setup = true;
            action = new RunMatlabTestsAction(runner, params);

            when(runner.getTempFolder()).thenReturn(tempFolder);
            when(tempFolder.getRemote()).thenReturn("/gravel/path");
            when(runner.copyFileToTempFolder(anyString(), anyString()))
                    .thenReturn(tempFolder);
        }
    }

    @Test
    public void shouldCopyGenscriptToTempDir() throws IOException, InterruptedException, MatlabExecutionException {
        action.run();

        verify(runner).copyFileToTempFolder(
                MatlabBuilderConstants.MATLAB_SCRIPT_GENERATOR,
                "genscript.zip");
        verify(tempFolder).unzip(tempFolder);
    }

    @Test
    public void shouldAddTempFolderToPath() throws IOException, InterruptedException, MatlabExecutionException {
        action.run();

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(runner).runMatlabCommand(captor.capture());

        assertThat(captor.getValue(), containsString("addpath('/gravel/path')"));
    }

    @Test
    public void shouldReplaceParamsCorrectlyWhenAllNull()
            throws IOException, InterruptedException, MatlabExecutionException {
        // Keep parameters as null
        action.run();

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(runner).runMatlabCommand(captor.capture());

        assertThat(captor.getValue(), containsString("genscript('Test')"));
    }

    @Test
    public void shouldReplaceParamsCorrectlyWithFewNull()
            throws IOException, InterruptedException, MatlabExecutionException {
        // Set some params
        doReturn("results.xml").when(params).getTestResultsJUnit();
        doReturn("cov.xml").when(params).getCodeCoverageCobertura();
        doReturn("true").when(params).getStrict();
        doReturn("Default").when(params).getLoggingLevel();
        doReturn("Concise").when(params).getOutputDetail();

        ArrayList<String> sourceFolders = new ArrayList<String>();
        sourceFolders.add("src");
        sourceFolders.add("toolbox");
        doReturn(sourceFolders).when(params).getSourceFolder();

        action.run();

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(runner).runMatlabCommand(captor.capture());
        assertThat(captor.getValue(), containsString(
                "genscript('Test','JUnitTestResults','results.xml',"
                        + "'CoberturaCodeCoverage','cov.xml',"
                        + "'Strict',true,"
                        + "'LoggingLevel','Default',"
                        + "'OutputDetail','Concise',"
                        + "'SourceFolder',{'src','toolbox'})"));
    }

    @Test
    public void shouldReplaceParamsCorrectlyWithNoneNull()
            throws IOException, InterruptedException, MatlabExecutionException {
        // Set all params
        doReturn("results.pdf").when(params).getTestResultsPDF();
        doReturn("results.tap").when(params).getTestResultsTAP();
        doReturn("results.xml").when(params).getTestResultsJUnit();
        doReturn("cov.xml").when(params).getCodeCoverageCobertura();
        doReturn("results.sltest").when(params).getTestResultsSimulinkTest();
        doReturn("cov.model").when(params).getModelCoverageCobertura();
        doReturn("MyTag").when(params).getSelectByTag();
        doReturn("true").when(params).getUseParallel();
        doReturn("true").when(params).getStrict();
        doReturn("Default").when(params).getLoggingLevel();
        doReturn("Concise").when(params).getOutputDetail();

        ArrayList<String> sourceFolders = new ArrayList<String>();
        sourceFolders.add("src");
        sourceFolders.add("toolbox");
        doReturn(sourceFolders).when(params).getSourceFolder();
        doReturn(sourceFolders).when(params).getSelectByFolder();

        action.run();

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(runner).runMatlabCommand(captor.capture());
        assertThat(captor.getValue(), containsString(
                "genscript('Test',"
                        + "'PDFTestReport','results.pdf',"
                        + "'TAPTestResults','results.tap',"
                        + "'JUnitTestResults','results.xml',"
                        + "'CoberturaCodeCoverage','cov.xml',"
                        + "'SimulinkTestResults','results.sltest',"
                        + "'CoberturaModelCoverage','cov.model',"
                        + "'SelectByTag','MyTag',"
                        + "'UseParallel',true,"
                        + "'Strict',true,"
                        + "'LoggingLevel','Default',"
                        + "'OutputDetail','Concise',"
                        + "'SourceFolder',{'src','toolbox'},"
                        + "'SelectByFolder',{'src','toolbox'})"));
    }

    @Test
    public void printsAndRethrowsMessage() throws IOException, InterruptedException, MatlabExecutionException {
        when(params.getTaskListener()).thenReturn(listener);
        when(listener.getLogger()).thenReturn(out);

        doThrow(new MatlabExecutionException(12)).when(runner).runMatlabCommand(anyString());

        try {
            action.run();
        } catch (MatlabExecutionException e) {
            verify(out).println(e.getMessage());
            assertEquals(12, e.getExitCode());
        }
        ;
    }
}
