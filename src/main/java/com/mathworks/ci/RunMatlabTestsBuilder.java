package com.mathworks.ci;

/**
 * Copyright 2019-2020 The MathWorks, Inc.
 * 
 * MATLAB test run builder used to run all MATLAB & Simulink tests automatically and generate selected test artifacts.
 * 
 */


import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import javax.annotation.Nonnull;
import org.apache.commons.io.FilenameUtils;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import com.mathworks.ci.UseMatlabVersionBuildWrapper.UseMatlabVersionDescriptor;
import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Launcher.ProcStarter;
import hudson.model.AbstractProject;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.FormValidation;
import jenkins.model.Jenkins;
import jenkins.tasks.SimpleBuildStep;
import net.sf.json.JSONObject;

public class RunMatlabTestsBuilder extends Builder implements SimpleBuildStep,MatlabBuild {
    
    private int buildResult;
    private EnvVars env;
    private boolean tapChkBx;
    private boolean junitChkBx;
    private boolean coberturaChkBx;
    private boolean stmResultsChkBx;
    private boolean modelCoverageChkBx;
    private boolean pdfReportChkBx;

    @DataBoundConstructor
    public RunMatlabTestsBuilder() {


    }


    // Getter and Setters to access local members


    @DataBoundSetter
    public void setTapChkBx(boolean tapChkBx) {
        this.tapChkBx = tapChkBx;
    }

    @DataBoundSetter
    public void setJunitChkBx(boolean junitChkBx) {
        this.junitChkBx = junitChkBx;
    }

    @DataBoundSetter
    public void setCoberturaChkBx(boolean coberturaChkBx) {
        this.coberturaChkBx = coberturaChkBx;
    }
    
    @DataBoundSetter
    public void setStmResultsChkBx(boolean stmResultsChkBx) {
        this.stmResultsChkBx = stmResultsChkBx;
    }
    
    @DataBoundSetter
    public void setModelCoverageChkBx(boolean modelCoverageChkBx) {
        this.modelCoverageChkBx = modelCoverageChkBx;
    }
    
    @DataBoundSetter
    public void setPdfReportChkBx(boolean pdfReportChkBx) {
        this.pdfReportChkBx = pdfReportChkBx;
    }
            
    public boolean getTapChkBx() {
        return tapChkBx;
    }

    public boolean getJunitChkBx() {
        return junitChkBx;
    }

    public boolean getCoberturaChkBx() {
        return coberturaChkBx;
    }

    public boolean getStmResultsChkBx() {
        return stmResultsChkBx;
    }
            
    public boolean getModelCoverageChkBx() {
        return modelCoverageChkBx;
    }
    
    public boolean getPdfReportChkBx() {
        return pdfReportChkBx;
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
        
        MatlabReleaseInfo rel;
        
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
        public boolean isApplicable(@SuppressWarnings("rawtypes") Class<? extends AbstractProject> jobtype) {
            return true;
        }
        
        
        /*
         * Validation for Test artifact generator checkBoxes
         */
        
        //Get the MATLAB root entered in build wrapper descriptor 
        
        

        public FormValidation doCheckCoberturaChkBx(@QueryParameter boolean coberturaChkBx) {
            List<Function<String, FormValidation>> listOfCheckMethods =
                    new ArrayList<Function<String, FormValidation>>();
            if (coberturaChkBx) {
                listOfCheckMethods.add(chkCoberturaSupport);
            }
            return FormValidationUtil.getFirstErrorOrWarning(listOfCheckMethods,getMatlabRoot());
        }

        Function<String, FormValidation> chkCoberturaSupport = (String matlabRoot) -> {
            FilePath matlabRootPath = new FilePath(new File(matlabRoot));
            rel = new MatlabReleaseInfo(matlabRootPath);
            final MatrixPatternResolver resolver = new MatrixPatternResolver(matlabRoot);
            if(!resolver.hasVariablePattern()) {
                try {
                    if (rel.verLessThan(MatlabBuilderConstants.BASE_MATLAB_VERSION_COBERTURA_SUPPORT)) {
                        return FormValidation
                                .warning(Message.getValue("Builder.matlab.cobertura.support.warning"));
                    }
                } catch (MatlabVersionNotFoundException e) {
                    return FormValidation.warning(Message.getValue("Builder.invalid.matlab.root.warning"));
                }
            }
            

            return FormValidation.ok();
        };
        
        public FormValidation doCheckModelCoverageChkBx(@QueryParameter boolean modelCoverageChkBx) {
            List<Function<String, FormValidation>> listOfCheckMethods =
                    new ArrayList<Function<String, FormValidation>>();
            if (modelCoverageChkBx) {
                listOfCheckMethods.add(chkModelCoverageSupport);
            }
            return FormValidationUtil.getFirstErrorOrWarning(listOfCheckMethods,getMatlabRoot());
        }
        
        Function<String, FormValidation> chkModelCoverageSupport = (String matlabRoot) -> {
            FilePath matlabRootPath = new FilePath(new File(matlabRoot));
            rel = new MatlabReleaseInfo(matlabRootPath);
            final MatrixPatternResolver resolver = new MatrixPatternResolver(matlabRoot);
            if(!resolver.hasVariablePattern()) {
                try {
                    if (rel.verLessThan(MatlabBuilderConstants.BASE_MATLAB_VERSION_MODELCOVERAGE_SUPPORT)) {
                        return FormValidation
                                .warning(Message.getValue("Builder.matlab.modelcoverage.support.warning"));
                    }
                } catch (MatlabVersionNotFoundException e) {
                    return FormValidation.warning(Message.getValue("Builder.invalid.matlab.root.warning"));
                }
            }
            
            
            return FormValidation.ok();
        };
        
        public FormValidation doCheckStmResultsChkBx(@QueryParameter boolean stmResultsChkBx) {
            List<Function<String, FormValidation>> listOfCheckMethods =
                    new ArrayList<Function<String, FormValidation>>();
            if (stmResultsChkBx) {
                listOfCheckMethods.add(chkSTMResultsSupport);
            }
            return FormValidationUtil.getFirstErrorOrWarning(listOfCheckMethods,getMatlabRoot());
        }
        
        Function<String, FormValidation> chkSTMResultsSupport = (String matlabRoot) -> {
            FilePath matlabRootPath = new FilePath(new File(matlabRoot));
            rel = new MatlabReleaseInfo(matlabRootPath);
            final MatrixPatternResolver resolver = new MatrixPatternResolver(matlabRoot);
            if(!resolver.hasVariablePattern()) {
                try {
                    if (rel.verLessThan(MatlabBuilderConstants.BASE_MATLAB_VERSION_EXPORTSTMRESULTS_SUPPORT)) {
                        return FormValidation
                                .warning(Message.getValue("Builder.matlab.exportstmresults.support.warning"));
                    }
                } catch (MatlabVersionNotFoundException e) {
                    return FormValidation.warning(Message.getValue("Builder.invalid.matlab.root.warning"));
                }
            }
            return FormValidation.ok();
        };
        
        //Method to get the MatlabRoot value from Build wrapper class.
        public static String getMatlabRoot() {
            try {
                return Jenkins.getInstance().getDescriptorByType(UseMatlabVersionDescriptor.class)
                        .getMatlabRootFolder();
            } catch (Exception e) {
                // For any exception during getMatlabRootFolder() operation, return matlabRoot as NULL.
                return null;
            }
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
                    constructCommandForTest(getInputArguments()),uniqueTmpFldrName);

            // Copy MATLAB scratch file into the workspace.
            FilePath targetWorkspace = new FilePath(launcher.getChannel(), workspace.getRemote());
            copyFileInWorkspace(MatlabBuilderConstants.MATLAB_RUNNER_RESOURCE,
                    MatlabBuilderConstants.MATLAB_RUNNER_TARGET_FILE, targetWorkspace);
            return matlabLauncher.join();
        } catch (Exception e) {
            listener.getLogger().println(e.getMessage());
            return 1;
        }finally {
            // Cleanup the runner File from tmp directory
            FilePath matlabRunnerScript = getNodeSpecificMatlabRunnerScript(launcher,uniqueTmpFldrName);
            if (matlabRunnerScript.isDirectory()) {
                matlabRunnerScript.deleteRecursive();
            }
        }
        
    }
    
    public String constructCommandForTest(String inputArguments) {
        final String matlabFunctionName = FilenameUtils.removeExtension(
                Message.getValue(MatlabBuilderConstants.MATLAB_RUNNER_TARGET_FILE));
        final String runCommand = "exit(" + matlabFunctionName + "(" + inputArguments + "))";
        return runCommand;
    }   
    
    // Concatenate the input arguments
    private String getInputArguments() {
        final String pdfReport = MatlabBuilderConstants.PDF_REPORT + "," + this.getPdfReportChkBx();
        final String tapResults = MatlabBuilderConstants.TAP_RESULTS + "," + this.getTapChkBx();
        final String junitResults = MatlabBuilderConstants.JUNIT_RESULTS + "," + this.getJunitChkBx();
        final String stmResults = MatlabBuilderConstants.STM_RESULTS + "," + this.getStmResultsChkBx();
        final String coberturaCodeCoverage = MatlabBuilderConstants.COBERTURA_CODE_COVERAGE + "," + this.getCoberturaChkBx();
        final String coberturaModelCoverage = MatlabBuilderConstants.COBERTURA_MODEL_COVERAGE + "," + this.getModelCoverageChkBx();    
        final String inputArgsToMatlabFcn = pdfReport + "," + tapResults + "," + junitResults + ","
                + stmResults + "," + coberturaCodeCoverage + "," + coberturaModelCoverage;
  
        return inputArgsToMatlabFcn;
    }
}
