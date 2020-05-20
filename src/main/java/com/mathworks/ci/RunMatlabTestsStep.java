package com.mathworks.ci;

/**
 * Copyright 2019-2020 The MathWorks, Inc.
 *  
 */

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.io.FilenameUtils;
import org.jenkinsci.plugins.workflow.steps.Step;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepDescriptor;
import org.jenkinsci.plugins.workflow.steps.StepExecution;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import com.google.common.collect.ImmutableSet;
import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Run;
import hudson.model.TaskListener;

public class RunMatlabTestsStep extends Step {
    
    private String testResultsPdf;
    private String testResultsTAP;
    private String testResultsJUnit;
    private String codeCoverageCobertura;
    private String testResultsSimulinkTest;
    private String modelCoverageCobertura;
    private static final String PDF_REPORT_PATH = "PDFReportPath";
    private static final String TAP_RESULTS_PATH = "TAPResultsPath";
    private static final String JUNIT_RESULTS_PATH = "JUnitResultsPath";
    private static final String COBERTURA_CODE_COVERAGE_PATH = "CoberturaCodeCoveragePath";
    private static final String STM_RESULTS_PATH = "SimulinkTestResultsPath";
    private static final String COBERTURA_MODEL_COVERAGE_PATH = "CoberturaModelCoveragePath";
    private static boolean COPY_SCRATCH_FILE = true;

    @DataBoundConstructor
    public RunMatlabTestsStep() {
        
    }
    
    public String getTestResultsTAP() {
        return testResultsTAP;
    }

    @DataBoundSetter
    public void setTestResultsTAP(String testResultsTAP) {
        this.testResultsTAP = testResultsTAP;
    }
    
    public String getTestResultsPdf() {
        return testResultsPdf;
    }

    @DataBoundSetter
    public void setTestResultsPdf(String testResultsPdf) {
        this.testResultsPdf = testResultsPdf;
    }

    public String getTestResultsJUnit() {
        return testResultsJUnit;
    }

    @DataBoundSetter
    public void setTestResultsJUnit(String testResultsJUnit) {
        this.testResultsJUnit = testResultsJUnit;
    }

    public String getCodeCoverageCobertura() {
        return codeCoverageCobertura;
    }

    @DataBoundSetter
    public void setCodeCoverageCobertura(String codeCoverageCobertura) {
        this.codeCoverageCobertura = codeCoverageCobertura;
    }

    public String getTestResultsSimulinkTest() {
        return testResultsSimulinkTest;
    }

    @DataBoundSetter
    public void setTestResultsSimulinkTest(String testResultsSimulinkTest) {
        this.testResultsSimulinkTest = testResultsSimulinkTest;
    }

    public String getModelCoverageCobertura() {
        return modelCoverageCobertura;
    }
    

    @DataBoundSetter
    public void setModelCoverageCobertura(String modelCoverageCobertura) {
        this.modelCoverageCobertura = modelCoverageCobertura;
    }


    @Override
    public StepExecution start(StepContext context) throws Exception {
       
        return new MatlabStepExecution(context,constructCommandForTest(getInputArgs()), COPY_SCRATCH_FILE);
    }
    
    @Extension
    public static class CommandStepDescriptor extends StepDescriptor {

        @Override
        public Set<? extends Class<?>> getRequiredContext() {
            return ImmutableSet.of(TaskListener.class, FilePath.class, Launcher.class,
                    EnvVars.class, Run.class);
        }

        @Override
        public String getFunctionName() {
            return Message.getValue("matlab.tests.build.step.name");
        }
    }
    
    public String constructCommandForTest(String inputArguments) {
        final String matlabFunctionName =
                FilenameUtils.removeExtension(MatlabBuilderConstants.MATLAB_TESTS_RUNNER_TARGET_FILE);
        final String runCommand = "exit(" + matlabFunctionName + "(" + inputArguments + "))";
        return runCommand;
    }

    
    private String getInputArgs() {
        final List<String> inputArgs = new ArrayList<>();
        final Map<String, String> args = getMatlabArgs();

        args.forEach((key, val) -> {
            if (val != null) {
                inputArgs.add("'" + key + "'" + "," + "'" + val + "'");
            }
        });

        if (inputArgs.isEmpty()) {
            return "";
        }

        return String.join(",", inputArgs);
    }
    
    private Map<String, String> getMatlabArgs() {
        final Map<String, String> args = new HashMap<String, String>();
        args.put(PDF_REPORT_PATH, getTestResultsPdf());
        args.put(TAP_RESULTS_PATH, getTestResultsTAP());
        args.put(JUNIT_RESULTS_PATH, getTestResultsJUnit());
        args.put(STM_RESULTS_PATH, getTestResultsSimulinkTest());
        args.put(COBERTURA_CODE_COVERAGE_PATH, getCodeCoverageCobertura());
        args.put(COBERTURA_MODEL_COVERAGE_PATH, getModelCoverageCobertura());
        return args;
    }
}
