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
    private TapChkBx tapChkBx;
    private JunitChkBx junitChkBx;   
    private CoberturaChkBx coberturaChkBx;
    private StmResultsChkBx stmResultsChkBx; 
    private ModelCovChkBx modelCoverageChkBx; 
    private PdfChkBx pdfReportChkBx;
    private String tapReportFilePath;
    private String pdfReportFilePath;
    private String junitReportFilePath;
    private String coberturaReportFilePath;
    private String stmResultsFilePath;
    private String modelCoverageFilePath;
   
    private List<String> inputArgs = new ArrayList<String>();

    @DataBoundConstructor
    public RunMatlabTestsBuilder() {

    }


    // Getter and Setters to access local members


    @DataBoundSetter
    public void setTapChkBx(TapChkBx tapChkBx) {
        this.tapChkBx = tapChkBx;
        this.tapReportFilePath = this.tapChkBx.getTapReportFilePath();
    } 
   
    @DataBoundSetter
    public void setJunitChkBx(JunitChkBx junitChkBx) {
        this.junitChkBx = junitChkBx;
        this.junitReportFilePath = this.junitChkBx.getJunitReportFilePath();
    }
    
    @DataBoundSetter
    public void setCoberturaChkBx(CoberturaChkBx coberturaChkBx) {
        this.coberturaChkBx = coberturaChkBx;
        this.coberturaReportFilePath = this.coberturaChkBx.getCoberturaReportFilePath();
    }
    
    @DataBoundSetter
    public void setStmResultsChkBx(StmResultsChkBx stmResultsChkBx) {
        this.stmResultsChkBx = stmResultsChkBx;
        this.stmResultsFilePath = this.stmResultsChkBx.getStmResultsFilePath();
    }
    
    @DataBoundSetter
    public void setModelCoverageChkBx(ModelCovChkBx modelCoverageChkBx) {
        this.modelCoverageChkBx = modelCoverageChkBx;
        this.modelCoverageFilePath = this.modelCoverageChkBx.getModelCoverageFilePath();
    }   

    @DataBoundSetter
    public void setPdfReportChkBx(PdfChkBx pdfReportChkBx) {
        this.pdfReportChkBx = pdfReportChkBx;
        this.pdfReportFilePath = this.pdfReportChkBx.getPdfReportFilePath();
    }
    
    public String getTapReportFilePath() {
        return this.tapReportFilePath;
    }      
    public boolean getIsPdfChecked() {
        if(this.pdfReportChkBx != null) {
            return true;
        }
        return false;
    }
    public TapChkBx getTapChkBx() {
        return this.tapChkBx;
    }
    
    public boolean getIsTapChecked() {
        if(this.tapChkBx != null) {
            return true;
        }
        return false;
    }
    
    public JunitChkBx getJunitChkBx() {
        return this.junitChkBx;
    }
    
    public String getJunitReportFilePath() {
        return this.junitReportFilePath;
    }
    
    public boolean getIsJunitChecked() {
        if(this.junitChkBx != null) {
            return true;
        }
        return false;
    }
    
    public CoberturaChkBx getCoberturaChkBx() {
        return this.coberturaChkBx;
    }
    
    public String getCoberturaReportFilePath() {
        return this.coberturaReportFilePath;
    }
    
    public boolean getIsCoberturaChecked() {
        if(this.coberturaChkBx != null) {
            return true;
        }
        return false;
    }
      
    public StmResultsChkBx getStmResultsChkBx() {
        return this.stmResultsChkBx;
    } 
    
    public String getStmResultsFilePath() {
        return stmResultsFilePath;
    }
    
    public boolean getIsStmChecked() {
        if(this.stmResultsChkBx != null) {
            return true;
        }
        return false;
    }
   
    public ModelCovChkBx getModelCoverageChkBx() {
        return this.modelCoverageChkBx;
    }
    
    public String getModelCoverageFilePath() {
        return modelCoverageFilePath;
    }
    
    public boolean getIsModelCovChecked() {
        if(this.modelCoverageChkBx != null) {
            return true;
        }
        return false;
    }
    
    public PdfChkBx getPdfReportChkBx() {
        return this.pdfReportChkBx;
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



        public FormValidation doCheckCoberturaChkBxFilePath(@QueryParameter String coberturaChkBxFilePath) {
            List<Function<String, FormValidation>> listOfCheckMethods =
                    new ArrayList<Function<String, FormValidation>>();
            if(!coberturaChkBxFilePath.isEmpty()) {
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
                
        public FormValidation doCheckModelCoverageChkBxFilePath(
                @QueryParameter String modelCoverageChkBxFilePath) {
            List<Function<String, FormValidation>> listOfCheckMethods =
                    new ArrayList<Function<String, FormValidation>>();
            if (!modelCoverageChkBxFilePath.isEmpty()) {
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

        public FormValidation doCheckStmResultsChkBxFilePath(@QueryParameter String stmResultsChkBxFilePath) {
            List<Function<String, FormValidation>> listOfCheckMethods =
                    new ArrayList<Function<String, FormValidation>>();
            if (!stmResultsChkBxFilePath.isEmpty()) {
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
        addInputArgs(MatlabBuilderConstants.PDF_REPORT_PATH, getPdfReportFilePath());
        addInputArgs(MatlabBuilderConstants.TAP_RESULTS_PATH, getTapReportFilePath());
        addInputArgs(MatlabBuilderConstants.JUNIT_RESULTS_PATH, getJunitReportFilePath());
        addInputArgs(MatlabBuilderConstants.STM_RESULTS_PATH, getStmResultsFilePath());
        addInputArgs(MatlabBuilderConstants.COBERTURA_CODE_COVERAGE_PATH,
                getCoberturaReportFilePath());
        addInputArgs(MatlabBuilderConstants.COBERTURA_MODEL_COVERAGE_PATH,
                getModelCoverageFilePath());

        if (inputArgs.isEmpty()) {
            return "";
        }

        return String.join(",", inputArgs);
    }
    
    private void addInputArgs(String reportName, String reportPath) {
        if (reportPath != null) {
            inputArgs.add(reportName + "," + "'" + reportPath + "'");
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
    public class PdfChkBx {
        String pdfReportFilePath;

        @DataBoundConstructor
        public PdfChkBx() {
            
        }

        @DataBoundSetter
        public void setPdfReportFilePath(String pdfReportFilePath) {
            this.pdfReportFilePath = pdfReportFilePath;
        }

        public String getPdfReportFilePath() {
            return this.pdfReportFilePath;
        }
    }

    public class TapChkBx {
        String tapReportFilePath;

        @DataBoundConstructor
        public TapChkBx() {

        }

        @DataBoundSetter
        public void setTapReportFilePath(String tapReportFilePath) {
            this.tapReportFilePath = tapReportFilePath;
        }

        public String getTapReportFilePath() {
            return tapReportFilePath;
        }
    }

    public class JunitChkBx {
        String junitReportFilePath;

        @DataBoundConstructor
        public JunitChkBx() {

        }

        @DataBoundSetter
        public void setJunitReportFilePath(String junitReportFilePath) {
            this.junitReportFilePath = junitReportFilePath;
        }

        public String getJunitReportFilePath() {
            return this.junitReportFilePath;
        }
    }

    public class CoberturaChkBx {
        String coberturaReportFilePath;

        @DataBoundConstructor
        public CoberturaChkBx() {

        }

        @DataBoundSetter
        public void setCoberturaReportFilePath(String coberturaReportFilePath) {
            this.coberturaReportFilePath = coberturaReportFilePath;
        }

        public String getCoberturaReportFilePath() {
            return this.coberturaReportFilePath;
        }
    }

    public class StmResultsChkBx {
        String stmResultsFilePath;

        @DataBoundConstructor
        public StmResultsChkBx() {

        }

        @DataBoundSetter
        public void setStmResultsFilePath(String stmResultsFilePath) {
            this.stmResultsFilePath = stmResultsFilePath;
        }

        public String getStmResultsFilePath() {
            return stmResultsFilePath;
        }
    }

    public class ModelCovChkBx {
        String modelCoverageFilePath;

        @DataBoundConstructor
        public ModelCovChkBx() {

        }

        @DataBoundSetter
        public void setModelCoverageFilePath(String modelCoverageFilePath) {
            this.modelCoverageFilePath = modelCoverageFilePath;
        }

        public String getModelCoverageFilePath() {
            return modelCoverageFilePath;
        }
    }
    
}
