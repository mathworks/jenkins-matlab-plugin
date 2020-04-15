package com.mathworks.ci;

/**
 * Copyright 2019-2020 The MathWorks, Inc.
 * 
 * MATLAB test run builder used to run all MATLAB & Simulink tests automatically and generate
 * selected test artifacts.
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

public class RunMatlabTestsBuilder extends Builder implements SimpleBuildStep, MatlabBuild {

    private int buildResult;
    private EnvVars env;
    private boolean tapChkBx;
    private String tapChkBxFilePath;
    private boolean junitChkBx;
    private String junitChkBxFilePath;
    private boolean coberturaChkBx;
    private String coberturaChkBxFilePath;
    private boolean stmResultsChkBx;
    private String stmResultsChkBxFilePath;
    private boolean modelCoverageChkBx;
    private String modelCoverageChkBxFilePath;
    private boolean pdfReportChkBx;
    private String pdfReportFilePath;
    private List<String> inputArgs = new ArrayList<String>();

    @DataBoundConstructor
    public RunMatlabTestsBuilder() {


    }


    // Getter and Setters to access local members


    @DataBoundSetter
    public void setTapChkBx(boolean tapChkBx) {
        this.tapChkBx = tapChkBx;
    }
    
    @DataBoundSetter
    public void setTapChkBxFilePath(String tapChkBxFilePath) {
        this.tapChkBxFilePath = tapChkBxFilePath;
    }

    @DataBoundSetter
    public void setJunitChkBx(boolean junitChkBx) {
        this.junitChkBx = junitChkBx;
    }
    
    @DataBoundSetter
    public void setJunitChkBxFilePath(String junitChkBxFilePath) {
        this.junitChkBxFilePath = junitChkBxFilePath;
    }

    @DataBoundSetter
    public void setCoberturaChkBx(boolean coberturaChkBx) {
        this.coberturaChkBx = coberturaChkBx;
    }
    
    @DataBoundSetter
    public void setCoberturaChkBxFilePath(String coberturaChkBxFilePath) {
        this.coberturaChkBxFilePath = coberturaChkBxFilePath;
    }

    @DataBoundSetter
    public void setStmResultsChkBx(boolean stmResultsChkBx) {
        this.stmResultsChkBx = stmResultsChkBx;
    }
    
    @DataBoundSetter
    public void setStmResultsChkBxFilePath(String stmResultsChkBxFilePath) {
        this.stmResultsChkBxFilePath = stmResultsChkBxFilePath;
    }

    @DataBoundSetter
    public void setModelCoverageChkBx(boolean modelCoverageChkBx) {
        this.modelCoverageChkBx = modelCoverageChkBx;
    }
    
    @DataBoundSetter
    public void setModelCoverageChkBxFilePath(String modelCoverageChkBxFilePath) {
        this.modelCoverageChkBxFilePath = modelCoverageChkBxFilePath;
    }

    @DataBoundSetter
    public void setPdfReportChkBx(boolean pdfReportChkBx) {
        this.pdfReportChkBx = pdfReportChkBx;
    }
    
    @DataBoundSetter
    public void setPdfReportFilePath(String pdfReportFilePath) {
        this.pdfReportFilePath = pdfReportFilePath;
    }


    public boolean getTapChkBx() {
        return tapChkBx;
    }
    
    public String getTapChkBxFilePath() {
        return tapChkBxFilePath;
    }

    public boolean getJunitChkBx() {
        return junitChkBx;
    }
    
    public String getJunitChkBxFilePath() {
        return junitChkBxFilePath;
    }

    public boolean getCoberturaChkBx() {
        return coberturaChkBx;
    }
    
    public String getCoberturaChkBxFilePath() {
        return coberturaChkBxFilePath;
    }

    public boolean getStmResultsChkBx() {
        return stmResultsChkBx;
    }
    
    public String getStmResultsChkBxFilePath() {
        return stmResultsChkBxFilePath;
    }

    public boolean getModelCoverageChkBx() {
        return modelCoverageChkBx;
    }
    
    public String getModelCoverageChkBxFilePath() {
        return modelCoverageChkBxFilePath;
    }

    public boolean getPdfReportChkBx() {
        return pdfReportChkBx;
    }
    
    public String getPdfReportFilePath() {
        return this.pdfReportFilePath;
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
        public boolean isApplicable(
                @SuppressWarnings("rawtypes") Class<? extends AbstractProject> jobtype) {
            return true;
        }


        /*
         * Validation for Test artifact generator checkBoxes
         */

        // Get the MATLAB root entered in build wrapper descriptor



        public FormValidation doCheckCoberturaChkBx(@QueryParameter boolean coberturaChkBx) {
            List<Function<String, FormValidation>> listOfCheckMethods =
                    new ArrayList<Function<String, FormValidation>>();
            if (coberturaChkBx) {
                listOfCheckMethods.add(chkCoberturaSupport);
            }
            return FormValidationUtil.getFirstErrorOrWarning(listOfCheckMethods, getMatlabRoot());
        }

        Function<String, FormValidation> chkCoberturaSupport = (String matlabRoot) -> {
            FilePath matlabRootPath = new FilePath(new File(matlabRoot));
            rel = new MatlabReleaseInfo(matlabRootPath);
            final MatrixPatternResolver resolver = new MatrixPatternResolver(matlabRoot);
            if (!resolver.hasVariablePattern()) {
                try {
                    if (rel.verLessThan(
                            MatlabBuilderConstants.BASE_MATLAB_VERSION_COBERTURA_SUPPORT)) {
                        return FormValidation.warning(
                                Message.getValue("Builder.matlab.cobertura.support.warning"));
                    }
                } catch (MatlabVersionNotFoundException e) {
                    return FormValidation
                            .warning(Message.getValue("Builder.invalid.matlab.root.warning"));
                }
            }


            return FormValidation.ok();
        };

        public FormValidation doCheckModelCoverageChkBx(
                @QueryParameter boolean modelCoverageChkBx) {
            List<Function<String, FormValidation>> listOfCheckMethods =
                    new ArrayList<Function<String, FormValidation>>();
            if (modelCoverageChkBx) {
                listOfCheckMethods.add(chkModelCoverageSupport);
            }
            return FormValidationUtil.getFirstErrorOrWarning(listOfCheckMethods, getMatlabRoot());
        }

        Function<String, FormValidation> chkModelCoverageSupport = (String matlabRoot) -> {
            FilePath matlabRootPath = new FilePath(new File(matlabRoot));
            rel = new MatlabReleaseInfo(matlabRootPath);
            final MatrixPatternResolver resolver = new MatrixPatternResolver(matlabRoot);
            if (!resolver.hasVariablePattern()) {
                try {
                    if (rel.verLessThan(
                            MatlabBuilderConstants.BASE_MATLAB_VERSION_MODELCOVERAGE_SUPPORT)) {
                        return FormValidation.warning(
                                Message.getValue("Builder.matlab.modelcoverage.support.warning"));
                    }
                } catch (MatlabVersionNotFoundException e) {
                    return FormValidation
                            .warning(Message.getValue("Builder.invalid.matlab.root.warning"));
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
            return FormValidationUtil.getFirstErrorOrWarning(listOfCheckMethods, getMatlabRoot());
        }

        Function<String, FormValidation> chkSTMResultsSupport = (String matlabRoot) -> {
            FilePath matlabRootPath = new FilePath(new File(matlabRoot));
            rel = new MatlabReleaseInfo(matlabRootPath);
            final MatrixPatternResolver resolver = new MatrixPatternResolver(matlabRoot);
            if (!resolver.hasVariablePattern()) {
                try {
                    if (rel.verLessThan(
                            MatlabBuilderConstants.BASE_MATLAB_VERSION_EXPORTSTMRESULTS_SUPPORT)) {
                        return FormValidation.warning(Message
                                .getValue("Builder.matlab.exportstmresults.support.warning"));
                    }
                } catch (MatlabVersionNotFoundException e) {
                    return FormValidation
                            .warning(Message.getValue("Builder.invalid.matlab.root.warning"));
                }
            }
            return FormValidation.ok();
        };

        // Method to get the MatlabRoot value from Build wrapper class.
        public static String getMatlabRoot() {
            try {
                return Jenkins.getInstance().getDescriptorByType(UseMatlabVersionDescriptor.class)
                        .getMatlabRootFolder();
            } catch (Exception e) {
                // For any exception during getMatlabRootFolder() operation, return matlabRoot as
                // NULL.
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

        addInputArgs(MatlabBuilderConstants.PDF_REPORT, getPdfReportChkBx(),
                MatlabBuilderConstants.PDF_REPORT_PATH, getPdfReportFilePath());

        addInputArgs(MatlabBuilderConstants.TAP_RESULTS, getTapChkBx(),
                MatlabBuilderConstants.TAP_RESULTS_PATH, getTapChkBxFilePath());

        addInputArgs(MatlabBuilderConstants.JUNIT_RESULTS, getJunitChkBx(),
                MatlabBuilderConstants.JUNIT_RESULTS_PATH, getJunitChkBxFilePath());

        addInputArgs(MatlabBuilderConstants.STM_RESULTS, getStmResultsChkBx(),
                MatlabBuilderConstants.STM_RESULTS_PATH, getStmResultsChkBxFilePath());

        addInputArgs(MatlabBuilderConstants.COBERTURA_CODE_COVERAGE, getCoberturaChkBx(),
                MatlabBuilderConstants.COBERTURA_CODE_COVERAGE_PATH, getCoberturaChkBxFilePath());

        addInputArgs(MatlabBuilderConstants.COBERTURA_MODEL_COVERAGE, getModelCoverageChkBx(),
                MatlabBuilderConstants.COBERTURA_MODEL_COVERAGE_PATH,
                getModelCoverageChkBxFilePath());

        if (inputArgs.isEmpty()) {
            return "";
        }

        return String.join(",", inputArgs);
    }

    private void addInputArgs(String chkbxName, boolean chkBxValue, String reportName,
            String reportPath) {
        if (chkBxValue) {
            inputArgs.add(chkbxName + "," + chkBxValue);
            if (!reportPath.isEmpty()) {
                inputArgs.add(reportName + "," + "'" + reportPath + "'");
            }
        }
    }
}
