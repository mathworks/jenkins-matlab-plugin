package com.mathworks.ci;

/**
 * Copyright 2019-2020 The MathWorks, Inc.
 * 
 * MATLAB test run builder used to run all MATLAB & Simulink tests automatically and generate selected test artifacts.
 * 
 */


import java.io.File;
import java.io.IOException;
import java.io.InputStream;
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
import com.mathworks.ci.AddMatlabToPathBuildWrapper.MatlabBuildWrapperDescriptor;
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
import hudson.util.FormValidation.Kind;
import jenkins.model.Jenkins;
import jenkins.tasks.SimpleBuildStep;
import net.sf.json.JSONObject;

public class RunMatlabTestsBuilder extends Builder implements SimpleBuildStep {
    
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
    public static class MatlabTestDescriptor extends BuildStepDescriptor<Builder> {
        
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

        public FormValidation doCheckCoberturaChkBx(@QueryParameter boolean coberturaChkBx) {
            List<Function<String, FormValidation>> listOfCheckMethods =
                    new ArrayList<Function<String, FormValidation>>();
            if (coberturaChkBx) {
                listOfCheckMethods.add(chkCoberturaSupport);
            }
            return getFirstErrorOrWarning(listOfCheckMethods);
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
            return getFirstErrorOrWarning(listOfCheckMethods);
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
            return getFirstErrorOrWarning(listOfCheckMethods);
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
        
        public FormValidation getFirstErrorOrWarning(
                List<Function<String, FormValidation>> validations) {
            if (validations == null || validations.isEmpty())
                return FormValidation.ok();
            try {
                final String matlabRoot = Jenkins.getInstance()
                        .getDescriptorByType(MatlabBuildWrapperDescriptor.class).getMatlabRootFolder();
                for (Function<String, FormValidation> val : validations) {
                    FormValidation validationResult = val.apply(matlabRoot);
                    if (validationResult.kind.compareTo(Kind.ERROR) == 0
                            || validationResult.kind.compareTo(Kind.WARNING) == 0) {
                        return validationResult;
                    }
                }
            }catch (Exception e) {
                return FormValidation.warning(Message.getValue("Builder.invalid.matlab.root.warning"));
            }
           
            return FormValidation.ok();
        }
    }

    @Override
    public void perform(@Nonnull Run<?, ?> build, @Nonnull FilePath workspace,
            @Nonnull Launcher launcher, @Nonnull TaskListener listener)
            throws InterruptedException, IOException {
        //Set the environment variable specific to the this build
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
        ProcStarter matlabLauncher;
        try {
            // Get matlabroot set in wrapper class.
            String matlabRoot = envVars.get("matlabroot");
            matlabLauncher = launcher.launch().pwd(workspace).envs(envVars);
            FilePath targetWorkspace = new FilePath(launcher.getChannel(), workspace.getRemote());
            if (launcher.isUnix()) {
                matlabLauncher = launcher.launch().pwd(workspace).envs(envVars).cmds("./run_matlab_command.sh",constructCommandForTest(getInputArguments())).stdout(listener);
                // Copy runner .sh for linux platform in workspace.
                cpoyFileInWorkspace(MatlabBuilderConstants.SHELL_RUNNER_SCRIPT,"Builder.matlab.runner.script.target.file.linux.name", targetWorkspace);
            } else {
                launcher = launcher.decorateByPrefix("cmd.exe", "/C");
                matlabLauncher = launcher.launch().pwd(workspace).envs(envVars).cmds("run_matlab_command.bat","\"" + constructCommandForTest(getInputArguments()) + "\"").stdout(listener);
                // Copy runner.bat for Windows platform in workspace.
                cpoyFileInWorkspace(MatlabBuilderConstants.BAT_RUNNER_SCRIPT,"Builder.matlab.runner.script.target.file.windows.name", targetWorkspace);
            }

            // Copy MATLAB scratch file into the workspace.
            cpoyFileInWorkspace(MatlabBuilderConstants.MATLAB_RUNNER_RESOURCE,MatlabBuilderConstants.MATLAB_RUNNER_TARGET_FILE, targetWorkspace);
        } catch (Exception e) {
            listener.getLogger().println(e.getMessage());
            return 1;
        }
        return matlabLauncher.join();
    }
    
    public String constructCommandForTest(String inputArguments) {
        String runCommand;
        String matlabFunctionName = FilenameUtils.removeExtension(
                Message.getValue(MatlabBuilderConstants.MATLAB_RUNNER_TARGET_FILE));
        runCommand = "exit(" + matlabFunctionName + "(" + inputArguments + "))";
        return runCommand;
    }   
    
    private void cpoyFileInWorkspace(String matlabRunnerResourcePath, String matlabRunnerTarget,
            FilePath targetWorkspace) throws IOException, InterruptedException {
        final ClassLoader classLoader = getClass().getClassLoader();
        FilePath targetFile = new FilePath(targetWorkspace, Message.getValue(matlabRunnerTarget));
        InputStream in = classLoader.getResourceAsStream(matlabRunnerResourcePath);
        targetFile.copyFrom(in);
        // set executable permission to the file.
        targetFile.chmod(0755);
    }
    
    // Concatenate the input arguments
    private String getInputArguments() {
        String pdfReport = MatlabBuilderConstants.PDF_REPORT + "," + this.getPdfReportChkBx();
        String tapResults = MatlabBuilderConstants.TAP_RESULTS + "," + this.getTapChkBx();
        String junitResults = MatlabBuilderConstants.JUNIT_RESULTS + "," + this.getJunitChkBx();
        String stmResults = MatlabBuilderConstants.STM_RESULTS + "," + this.getStmResultsChkBx();
        String coberturaCodeCoverage = MatlabBuilderConstants.COBERTURA_CODE_COVERAGE + "," + this.getCoberturaChkBx();
        String coberturaModelCoverage = MatlabBuilderConstants.COBERTURA_MODEL_COVERAGE + "," + this.getModelCoverageChkBx();    
        String inputArgsToMatlabFcn = pdfReport + "," + tapResults + "," + junitResults + ","
                + stmResults + "," + coberturaCodeCoverage + "," + coberturaModelCoverage;
  
        return inputArgsToMatlabFcn;
    }
}
