package com.mathworks.ci.pipeline;

/**
 * Copyright 2024, The MathWorks Inc.
 *
 */

import java.io.IOException;

import java.util.ArrayList;

import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import static org.mockito.Mockito.*;

import org.jenkinsci.plugins.workflow.steps.StepContext;

import com.mathworks.ci.parameters.TestActionParameters;

@RunWith(MockitoJUnitRunner.class)
public class RunMatlabTestsStepUnitTest {
    @Mock StepContext context;     

    @Test
    public void shouldHandleNullCase() throws Exception {
        RunMatlabTestsStep step = new RunMatlabTestsStep();
        MatlabRunTestsStepExecution ex = (MatlabRunTestsStepExecution)step.start(context);

        TestActionParameters params = ex.getParameters();
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
    }

    @Test
    public void shouldHandleMaximalCase() throws Exception {
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

        MatlabRunTestsStepExecution ex = (MatlabRunTestsStepExecution)step.start(context);

        TestActionParameters params = ex.getParameters();
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
    }
}
