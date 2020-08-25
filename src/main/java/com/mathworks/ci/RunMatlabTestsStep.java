package com.mathworks.ci;
/**
 * Copyright 2020 The MathWorks, Inc.
 *  
 */

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
    
    private String testResultsPDF;
    private String testResultsTAP;
    private String testResultsJUnit;
    private String codeCoverageCobertura;
    private String testResultsSimulinkTest;
    private String modelCoverageCobertura;
  

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
    
    public String getTestResultsPDF() {
        return testResultsPDF;
    }

    @DataBoundSetter
    public void setTestResultsPDF(String testResultsPDF) {
        this.testResultsPDF = testResultsPDF;
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
        return new MatlabRunTestsStepExecution(context, getMatlabArgs());
    }

    @Extension
    public static class RunTestsStepDescriptor extends StepDescriptor {

        @Override
        public Set<? extends Class<?>> getRequiredContext() {
            return ImmutableSet.of(TaskListener.class, FilePath.class, Launcher.class,
                    EnvVars.class, Run.class);
        }

        @Override
        public String getFunctionName() {
            return Message.getValue("matlab.tests.build.step.name");
        }
        
        @Override
        public String getDisplayName() {
            return Message.getValue("matlab.tests.step.display.name");
        }
    }
    
    private String getInputArgs() {
        final List<String> inputArgs = new ArrayList<>();
        final Map<String, String> args = getMatlabArgs();

        args.forEach((key, val) -> {
            if (val != null) {
                inputArgs.add("'" + key + "'" + "," + "'" + val.replaceAll("'", "''") + "'");
            }
        });

        if (inputArgs.isEmpty()) {
            return "";
        }

        return String.join(",", inputArgs);
    }
    
    private Map<String, String> getMatlabArgs() {
        final Map<String, String> args = new HashMap<String, String>();
        args.put("PDFReportPath", getTestResultsPDF());
        args.put("TAPResultsPath", getTestResultsTAP());
        args.put("JUnitResultsPath", getTestResultsJUnit());
        args.put("SimulinkTestResultsPath", getTestResultsSimulinkTest());
        args.put("CoberturaCodeCoveragePath", getCodeCoverageCobertura());
        args.put("CoberturaModelCoveragePath", getModelCoverageCobertura());
        return args;
    }
}
