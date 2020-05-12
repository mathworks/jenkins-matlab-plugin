package com.mathworks.ci;

/** 
 * Copyright 2019-2020 The MathWorks, Inc.  
 *  
 * MATLAB test run builder used to run all MATLAB & Simulink tests automatically and generate   
 * selected test artifacts. 
 *  
 */

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.annotation.Nonnull;
import org.apache.commons.io.FilenameUtils;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.StaplerRequest;
import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Launcher.ProcStarter;
import hudson.model.AbstractDescribableImpl;
import hudson.model.AbstractProject;
import hudson.model.Descriptor;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import jenkins.tasks.SimpleBuildStep;
import net.sf.json.JSONObject;

public class RunMatlabTestsBuilder extends Builder implements SimpleBuildStep, MatlabBuild {

    private int buildResult;
    private EnvVars env;
    private TapArtifact tapArtifact;
    private JunitArtifact junitArtifact;   
    private CoberturaArtifact coberturaArtifact;
    private StmResultsArtifact stmResultsArtifact; 
    private ModelCovArtifact modelCoverageArtifact; 
    private PdfArtifact pdfReportArtifact;
   

    @DataBoundConstructor
    public RunMatlabTestsBuilder() {

    }


    // Getter and Setters to access local members


    @DataBoundSetter
    public void setTapArtifact(TapArtifact tapArtifact) {
        this.tapArtifact = tapArtifact;
    } 
   
    @DataBoundSetter
    public void setJunitArtifact(JunitArtifact junitArtifact) {
        this.junitArtifact = junitArtifact;
    }
    
    @DataBoundSetter
    public void setCoberturaArtifact(CoberturaArtifact coberturaArtifact) {
        this.coberturaArtifact = coberturaArtifact;
    }
    
    @DataBoundSetter
    public void setStmResultsArtifact(StmResultsArtifact stmResultsArtifact) {
        this.stmResultsArtifact = stmResultsArtifact;
    }
    
    @DataBoundSetter
    public void setModelCoverageArtifact(ModelCovArtifact modelCoverageArtifact) {
        this.modelCoverageArtifact = modelCoverageArtifact;
    }   

    @DataBoundSetter
    public void setPdfReportArtifact(PdfArtifact pdfReportArtifact) {
        this.pdfReportArtifact = pdfReportArtifact;
    }
      
    
    public TapArtifact getTapArtifact() {
        return this.tapArtifact;
    }
        
    public JunitArtifact getJunitArtifact() {
        return this.junitArtifact;
    }
    
        
    public CoberturaArtifact getCoberturaArtifact() {
        return this.coberturaArtifact;
    }
          
    public StmResultsArtifact getStmResultsArtifact() {
        return this.stmResultsArtifact;
    } 
       
    public ModelCovArtifact getModelCoverageArtifact() {
        return this.modelCoverageArtifact;
    }
    
    public PdfArtifact getPdfReportArtifact() {
        return this.pdfReportArtifact;
    }
    
    private void setEnv(EnvVars env) {
        this.env = env;
    }

    private EnvVars getEnv() {
        return this.env;
    }

    @Symbol("RunMatlabTests")
    @Extension
    public static class RunMatlabTestsDescriptor extends BuildStepDescriptor<Builder> {

        // Overridden Method used to show the text under build dropdown
        @Override
        public String getDisplayName() {
            return Message.getBuilderDisplayName();
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
            save();
            return super.configure(req, formData);
        }

        /*
         * This is to identify which project type in jenkins this should be applicable.(non-Javadoc)
         * 
         * @see hudson.tasks.BuildStepDescriptor#isApplicable(java.lang.Class)
         * 
         * if it returns true then this build step will be applicable for all project type.
         */
        @Override
        public boolean isApplicable(
                @SuppressWarnings("rawtypes") Class<? extends AbstractProject> jobtype) {
            return true;
        }
    }

    @Override
    public void perform(@Nonnull Run<?, ?> build, @Nonnull FilePath workspace,
            @Nonnull Launcher launcher, @Nonnull TaskListener listener)
            throws InterruptedException, IOException {

        // Set the environment variable specific to the this build
        setEnv(build.getEnvironment(listener));

        // Invoke MATLAB command and transfer output to standard
        // Output Console

        buildResult = execMatlabCommand(workspace, launcher, listener, getEnv());

        if (buildResult != 0) {
            build.setResult(Result.FAILURE);
        }
    }

    private synchronized int execMatlabCommand(FilePath workspace, Launcher launcher,
            TaskListener listener, EnvVars envVars) throws IOException, InterruptedException {
        final String uniqueTmpFldrName = getUniqueNameForRunnerFile();
        ProcStarter matlabLauncher;
        try {
            matlabLauncher = getProcessToRunMatlabCommand(workspace, launcher, listener, envVars,
                    constructCommandForTest(getInputArguments()), uniqueTmpFldrName);

            // Copy MATLAB scratch file into the workspace.
            FilePath targetWorkspace = new FilePath(launcher.getChannel(), workspace.getRemote());
            copyFileInWorkspace(MatlabBuilderConstants.MATLAB_TESTS_RUNNER_RESOURCE,
                    MatlabBuilderConstants.MATLAB_TESTS_RUNNER_TARGET_FILE, targetWorkspace);
            return matlabLauncher.join();
        } catch (Exception e) {
            listener.getLogger().println(e.getMessage());
            return 1;
        } finally {
            // Cleanup the runner File from tmp directory
            FilePath matlabRunnerScript =
                    getFilePathForUniqueFolder(launcher, uniqueTmpFldrName, workspace);
            if (matlabRunnerScript.exists()) {
                matlabRunnerScript.deleteRecursive();
            }
        }
    }

    public String constructCommandForTest(String inputArguments) {
        final String matlabFunctionName =
                FilenameUtils.removeExtension(MatlabBuilderConstants.MATLAB_TESTS_RUNNER_TARGET_FILE);
        final String runCommand = "exit(" + matlabFunctionName + "(" + inputArguments + "))";
        return runCommand;
    }

    // Concatenate the input arguments
    private String getInputArguments() {

        final List<String> inputArgs = new ArrayList<String>();
        final List<Artifact> artifactList =
                new ArrayList<Artifact>(Arrays.asList(getPdfReportArtifact(), getTapArtifact(),
                        getJunitArtifact(), getStmResultsArtifact(), getCoberturaArtifact(),
                        getModelCoverageArtifact()));

        for (Artifact artifact : artifactList) {
            addInputArgs(artifact, inputArgs);
        }

        if (inputArgs.isEmpty()) {
            return "";
        }

        return String.join(",", inputArgs);
    }

    public void addInputArgs(Artifact artifactType, List<String> inputArgs) {
        if (artifactType != null) {
            artifactType.addFilePathArgTo(inputArgs);
        }
    }
    
    
    /*
     * Classes for each optional block in jelly file.This is restriction from Stapler architecture
     * when we use <f:optionalBlock> as it creates a object for each block in JSON. This could be
     * simplified by using inline=true attribute of <f:optionalBlock> however it has some abrupt UI
     * scrolling issue on click and also some esthetic issue like broken gray side bar appears.Some
     * discussion about this on Jenkins forum
     * https://groups.google.com/forum/#!searchin/jenkinsci-dev/OptionalBlock$20action$20class%
     * 7Csort:date/jenkinsci-dev/AFYHSG3NUEI/UsVJIKoE4B8J
     * 
     */
    public static final class PdfArtifact extends AbstractDescribableImpl<PdfArtifact> implements Artifact {
        private String pdfReportFilePath;

        @DataBoundConstructor
        public PdfArtifact() {

        }

        @DataBoundSetter
        public void setPdfReportFilePath(String pdfReportFilePath) {
            this.pdfReportFilePath = pdfReportFilePath;
        }

        public String getPdfReportFilePath() {
            return this.pdfReportFilePath;
        }
        
        @Override
        public void addFilePathArgTo(List<String> inputArgs) {
            inputArgs.add(MatlabBuilderConstants.PDF_REPORT_PATH + "," + "'"
                    + getPdfReportFilePath() + "'");
        }
        @Extension public static class DescriptorImpl extends Descriptor<PdfArtifact> {}
    }

    public static final class TapArtifact extends AbstractDescribableImpl<TapArtifact> implements Artifact {
        private String tapReportFilePath;

        @DataBoundConstructor
        public TapArtifact() {

        }

        @DataBoundSetter
        public void setTapReportFilePath(String tapReportFilePath) {
            this.tapReportFilePath = tapReportFilePath;
        }

        public String getTapReportFilePath() {
            return tapReportFilePath;
        }

        @Override
        public void addFilePathArgTo(List<String> inputArgs) {
            inputArgs.add(MatlabBuilderConstants.TAP_RESULTS_PATH + "," + "'"
                    + getTapReportFilePath() + "'");
        }
        @Extension public static class DescriptorImpl extends Descriptor<TapArtifact> {}
    }

    public static final class JunitArtifact extends AbstractDescribableImpl<JunitArtifact> implements Artifact {
        private String junitReportFilePath;

        @DataBoundConstructor
        public JunitArtifact() {

        }

        @DataBoundSetter
        public void setJunitReportFilePath(String junitReportFilePath) {
            this.junitReportFilePath = junitReportFilePath;
        }

        public String getJunitReportFilePath() {
            return this.junitReportFilePath;
        }

        @Override
        public void addFilePathArgTo(List<String> inputArgs) {
            inputArgs.add(MatlabBuilderConstants.JUNIT_RESULTS_PATH + "," + "'"
                    + getJunitReportFilePath() + "'");
        }
        @Extension public static class DescriptorImpl extends Descriptor<JunitArtifact> {}
    }

    public static final class CoberturaArtifact extends AbstractDescribableImpl<CoberturaArtifact> implements Artifact {
        private String coberturaReportFilePath;

        @DataBoundConstructor
        public CoberturaArtifact() {

        }

        @DataBoundSetter
        public void setCoberturaReportFilePath(String coberturaReportFilePath) {
            this.coberturaReportFilePath = coberturaReportFilePath;
        }

        public String getCoberturaReportFilePath() {
            return this.coberturaReportFilePath;
        }

        @Override
        public void addFilePathArgTo(List<String> inputArgs) {
            inputArgs.add(MatlabBuilderConstants.COBERTURA_CODE_COVERAGE_PATH + "," + "'"
                    + getCoberturaReportFilePath() + "'");
        }
        @Extension public static class DescriptorImpl extends Descriptor<CoberturaArtifact> {}
    }

    public static final class StmResultsArtifact extends AbstractDescribableImpl<StmResultsArtifact> implements Artifact {
        private String stmResultsFilePath;

        @DataBoundConstructor
        public StmResultsArtifact() {

        }

        @DataBoundSetter
        public void setStmResultsFilePath(String stmResultsFilePath) {
            this.stmResultsFilePath = stmResultsFilePath;
        }

        public String getStmResultsFilePath() {
            return stmResultsFilePath;
        }

        @Override
        public void addFilePathArgTo(List<String> inputArgs) {
            inputArgs.add(MatlabBuilderConstants.STM_RESULTS_PATH + "," + "'"
                    + getStmResultsFilePath() + "'");
        }
        @Extension public static class DescriptorImpl extends Descriptor<StmResultsArtifact> {}
    }

    public static final class ModelCovArtifact extends AbstractDescribableImpl<ModelCovArtifact> implements Artifact{
        private String modelCoverageFilePath;

        @DataBoundConstructor
        public ModelCovArtifact() {

        }

        @DataBoundSetter
        public void setModelCoverageFilePath(String modelCoverageFilePath) {
            this.modelCoverageFilePath = modelCoverageFilePath;
        }

        public String getModelCoverageFilePath() {
            return modelCoverageFilePath;
        }

        @Override
        public void addFilePathArgTo(List<String> inputArgs) {
            inputArgs.add(MatlabBuilderConstants.COBERTURA_MODEL_COVERAGE_PATH + "," + "'"
                    + getModelCoverageFilePath() + "'");
        }
        @Extension public static class DescriptorImpl extends Descriptor<ModelCovArtifact> {}
    }
  
    public interface Artifact{
        public void addFilePathArgTo(List<String> inputArgs);
    }
}
