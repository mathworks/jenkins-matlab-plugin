package com.mathworks.ci.pipeline;

/**
 * Copyright 2024, The MathWorks Inc.
 */

import java.io.IOException;
import java.util.ArrayList;

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
    @Mock
    StepContext context;
    @Mock
    MatlabActionFactory factory;
    @Mock
    RunMatlabTestsAction action;

    @Before
    public void setup() throws IOException, InterruptedException {
        when(factory.createAction(any(TestActionParameters.class))).thenReturn(action);
    }

    @Test
    public void shouldHandleNullCase() throws Exception, IOException, InterruptedException, MatlabExecutionException {
        RunMatlabTestsStep step = new RunMatlabTestsStep();
        MatlabRunTestsStepExecution ex = new MatlabRunTestsStepExecution(factory, context, step);

        ex.run();

        ArgumentCaptor<TestActionParameters> captor = ArgumentCaptor.forClass(TestActionParameters.class);
        verify(factory).createAction(captor.capture());

        TestActionParameters params = captor.getValue();
        assertEquals("", params.getStartupOptions());
        assertEquals(null, params.getTestResultsPDF());
        assertEquals(null, params.getTestResultsTAP());
        assertEquals(null, params.getTestResultsJUnit());
        assertEquals(null, params.getCodeCoverageCobertura());
        assertEquals(null, params.getTestResultsSimulinkTest());
        assertEquals(null, params.getModelCoverageCobertura());
        assertEquals(null, params.getSelectByTag());
        assertEquals(null, params.getLoggingLevel());
        assertEquals(null, params.getOutputDetail());
        assertEquals("false", params.getUseParallel());
        assertEquals("false", params.getStrict());
        assertEquals(null, params.getSourceFolder());
        assertEquals(null, params.getSelectByFolder());

        verify(action).run();
    }

    @Test
    public void shouldHandleMaximalCase()
            throws Exception, IOException, InterruptedException, MatlabExecutionException {
        RunMatlabTestsStep step = new RunMatlabTestsStep();
        step.setStartupOptions("-nojvm -logfile file");
        step.setTestResultsPDF("res.pdf");
        step.setTestResultsTAP("res.tap");
        step.setTestResultsJUnit("res.xml");
        step.setCodeCoverageCobertura("cov.xml");
        step.setTestResultsSimulinkTest("res.sltest");
        step.setModelCoverageCobertura("cov.model");
        step.setSelectByTag("MyTag");
        step.setLoggingLevel("Concise");
        step.setOutputDetail("Concise");
        step.setUseParallel(true);
        step.setStrict(true);

        ArrayList<String> folders = new ArrayList<String>();
        folders.add("src");
        folders.add("toolbox");

        step.setSourceFolder(folders);
        step.setSelectByFolder(folders);

        MatlabRunTestsStepExecution ex = new MatlabRunTestsStepExecution(factory, context, step);

        ex.run();

        ArgumentCaptor<TestActionParameters> captor = ArgumentCaptor.forClass(TestActionParameters.class);
        verify(factory).createAction(captor.capture());

        TestActionParameters params = captor.getValue();
        assertEquals("-nojvm -logfile file", params.getStartupOptions());
        assertEquals("res.pdf", params.getTestResultsPDF());
        assertEquals("res.tap", params.getTestResultsTAP());
        assertEquals("res.xml", params.getTestResultsJUnit());
        assertEquals("cov.xml", params.getCodeCoverageCobertura());
        assertEquals("res.sltest", params.getTestResultsSimulinkTest());
        assertEquals("cov.model", params.getModelCoverageCobertura());
        assertEquals("MyTag", params.getSelectByTag());
        assertEquals("Concise", params.getLoggingLevel());
        assertEquals("Concise", params.getOutputDetail());
        assertEquals("true", params.getUseParallel());
        assertEquals("true", params.getStrict());
        assertEquals(folders, params.getSourceFolder());
        assertEquals(folders, params.getSelectByFolder());

        verify(action).run();
    }

    @Test
    public void shouldHandleActionThrowing()
            throws Exception, IOException, InterruptedException, MatlabExecutionException {
        MatlabRunTestsStepExecution ex = new MatlabRunTestsStepExecution(factory, context, new RunMatlabTestsStep());

        doThrow(new MatlabExecutionException(12)).when(action).run();

        ex.run();

        verify(context).onFailure(any(MatlabExecutionException.class));
    }
}
