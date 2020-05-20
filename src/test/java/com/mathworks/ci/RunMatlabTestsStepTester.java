package com.mathworks.ci;

/**
 * Copyright 2019-2020 The MathWorks, Inc.
 *  
 */

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepDescriptor;
import org.jenkinsci.plugins.workflow.steps.StepExecution;
import org.kohsuke.stapler.DataBoundConstructor;
import com.google.common.collect.ImmutableSet;
import com.kenai.jffi.Array;
import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Run;
import hudson.model.TaskListener;

public class RunMatlabTestsStepTester extends RunMatlabTestsStep {
    
    
    @DataBoundConstructor
    public RunMatlabTestsStepTester() {
        
    }
    
    @Override
    public StepExecution start(StepContext context) throws Exception {
        
        return new TestStepExecution(context,constructCommandForTest(getInputArgs()), true);
    }
    
    @Extension
    public static class CommandStepTestDescriptor extends StepDescriptor {

        @Override
        public Set<? extends Class<?>> getRequiredContext() {
            return ImmutableSet.of(TaskListener.class, FilePath.class, Launcher.class,
                    EnvVars.class, Run.class);
        }

        @Override
        public String getFunctionName() {
            return "testMATLABTests";
        }
    }
    
    public String getInputArgs() {
        List<String> args = Arrays.asList(getTestResultsPdf(), getTestResultsTAP(),
                getTestResultsJUnit(), getTestResultsSimulinkTest(), getCodeCoverageCobertura(),
                getModelCoverageCobertura());

        return String.join(",", args);
    }

}
