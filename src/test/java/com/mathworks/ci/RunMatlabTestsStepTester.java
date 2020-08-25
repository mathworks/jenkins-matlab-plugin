package com.mathworks.ci;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

public class RunMatlabTestsStepTester extends RunMatlabTestsStep {


    @DataBoundConstructor
    public RunMatlabTestsStepTester() {

    }

    @Override
    public StepExecution start(StepContext context) throws Exception {
        Launcher launcher = context.get(Launcher.class);
        FilePath workspace = context.get(FilePath.class);

        // Copy Scratch file needed to run MATLAB tests in workspace
        FilePath targetWorkspace = new FilePath(launcher.getChannel(), workspace.getRemote());
        copyScratchFileInWorkspace(MatlabBuilderConstants.MATLAB_TESTS_RUNNER_RESOURCE,
                MatlabBuilderConstants.MATLAB_TESTS_RUNNER_TARGET_FILE, targetWorkspace);
        return new TestStepExecution(context, getInputArgs());
    }

    private void copyScratchFileInWorkspace(String sourceFile, String targetFile,
            FilePath targetWorkspace) throws IOException, InterruptedException {
        final ClassLoader classLoader = getClass().getClassLoader();
        FilePath targetFilePath = new FilePath(targetWorkspace, targetFile);
        InputStream in = classLoader.getResourceAsStream(sourceFile);
        targetFilePath.copyFrom(in);
        // set executable permission
        targetFilePath.chmod(0755);
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

    public Map<String,String> getInputArgs() {
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
