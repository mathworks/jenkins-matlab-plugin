package com.mathworks.ci;

/**
 * Copyright 2022 The MathWorks, Inc.
 *  
 */

import java.util.Set;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepDescriptor;
import org.jenkinsci.plugins.workflow.steps.StepExecution;
import org.kohsuke.stapler.DataBoundConstructor;
import com.google.common.collect.ImmutableSet;
import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Run;
import hudson.model.TaskListener;

public class RunMatlabBuildStepTester extends RunMatlabBuildStep {
    @DataBoundConstructor
    public RunMatlabBuildStepTester() {

    }

    @Override
    public StepExecution start(StepContext context) throws Exception {   
        return new TestStepExecution(context,this.getTasks(), this.getStartupOptions());
    }
    
    @Extension
    public static class BuildStepTestDescriptor extends StepDescriptor {

        @Override
        public Set<? extends Class<?>> getRequiredContext() {
            return ImmutableSet.of(TaskListener.class, FilePath.class, Launcher.class,
                    EnvVars.class, Run.class);
        }

        @Override
        public String getFunctionName() {
            return "testMATLABBuild";
        }
    }

}
