package com.mathworks.ci;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import javax.annotation.Nonnull;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import com.mathworks.ci.MatlabBuildWrapper.MatabBuildWrapperDescriptor;
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

public class MatlabTestRunBuilder extends Builder implements SimpleBuildStep {
    
    private int buildResult;
    private String matlabRoot;
    private EnvVars env;
    private MatlabReleaseInfo matlabRel;
    private CommandConstructUtil utils;
    private boolean tatapChkBx;
    private boolean taJunitChkBx;
    private boolean taCoberturaChkBx;
    private boolean taSTMResultsChkBx;
    private boolean taModelCoverageChkBx;
    private boolean taPDFReportChkBx;

    @DataBoundConstructor
    public MatlabTestRunBuilder() {


    }


    // Getter and Setters to access local members

    @DataBoundSetter
    public void setMatlabRoot(String matlabRoot) {
        this.matlabRoot = matlabRoot;
    }

    @DataBoundSetter
    public void setTatapChkBx(boolean tatapChkBx) {
        this.tatapChkBx = tatapChkBx;
    }

    @DataBoundSetter
    public void setTaJunitChkBx(boolean taJunitChkBx) {
        this.taJunitChkBx = taJunitChkBx;
    }

    @DataBoundSetter
    public void setTaCoberturaChkBx(boolean taCoberturaChkBx) {
        this.taCoberturaChkBx = taCoberturaChkBx;
    }
    
    @DataBoundSetter
    public void setTaSTMResultsChkBx(boolean taSTMResultsChkBx) {
        this.taSTMResultsChkBx = taSTMResultsChkBx;
    }
    
    @DataBoundSetter
    public void setTaModelCoverageChkBx(boolean taModelCoverageChkBx) {
        this.taModelCoverageChkBx = taModelCoverageChkBx;
    }
    
    @DataBoundSetter
    public void setTaPDFReportChkBx(boolean taPDFReportChkBx) {
        this.taPDFReportChkBx = taPDFReportChkBx;
    }
            
    public boolean getTatapChkBx() {
        return tatapChkBx;
    }

    public boolean getTaJunitChkBx() {
        return taJunitChkBx;
    }

    public boolean getTaCoberturaChkBx() {
        return taCoberturaChkBx;
    }

    public boolean getTaSTMResultsChkBx() {
        return taSTMResultsChkBx;
    }
            
    public boolean getTaModelCoverageChkBx() {
        return taModelCoverageChkBx;
    }
    
    public boolean getTaPDFReportChkBx() {
        return taPDFReportChkBx;
    }

    public String getMatlabRoot() {
        return this.matlabRoot;
    }
    
    private void setEnv(EnvVars env) {
        this.env = env;
    }
    

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

        public FormValidation doCheckTaCoberturaChkBx(@QueryParameter boolean taCoberturaChkBx) {
            List<Function<String, FormValidation>> listOfCheckMethods =
                    new ArrayList<Function<String, FormValidation>>();
            if (taCoberturaChkBx) {
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
        
        public FormValidation doCheckTaModelCoverageChkBx(@QueryParameter boolean taModelCoverageChkBx) {
            List<Function<String, FormValidation>> listOfCheckMethods =
                    new ArrayList<Function<String, FormValidation>>();
            if (taModelCoverageChkBx) {
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
        
        public FormValidation doCheckTaSTMResultsChkBx(@QueryParameter boolean taSTMResultsChkBx) {
            List<Function<String, FormValidation>> listOfCheckMethods =
                    new ArrayList<Function<String, FormValidation>>();
            if (taSTMResultsChkBx) {
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
                        .getDescriptorByType(MatabBuildWrapperDescriptor.class).getMatlabRoot();
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
        String matlabRoot = this.env.get("matlabroot");
        utils = new CommandConstructUtil(launcher, matlabRoot);
        //Set the environment variable specific to the this build
        setEnv(build.getEnvironment(listener));
        //Get node specific matlabroot to get matlab version information
        FilePath nodeSpecificMatlabRoot = new FilePath(launcher.getChannel(),matlabRoot);
        matlabRel = new MatlabReleaseInfo(nodeSpecificMatlabRoot);
        
        // Invoke MATLAB command and transfer output to standard
        // Output Console

        buildResult = execMatlabCommand(workspace, launcher, listener);

        if (buildResult != 0) {
            build.setResult(Result.FAILURE);
        }
    }

    private synchronized int execMatlabCommand(FilePath workspace, Launcher launcher,
            TaskListener listener)
            throws IOException, InterruptedException {
        ProcStarter matlabLauncher;
        try {
            matlabLauncher = launcher.launch().pwd(workspace).envs(this.env);
            if (matlabRel.verLessThan(MatlabBuilderConstants.BASE_MATLAB_VERSION_BATCH_SUPPORT)) {
                ListenerLogDecorator outStream = new ListenerLogDecorator(listener);
                matlabLauncher = matlabLauncher.cmds(utils.constructDefaultCommandForTestRun(getInputArguments())).stderr(outStream);
            } else {
                matlabLauncher = matlabLauncher.cmds(utils.constructBatchCommandForTestRun(getInputArguments())).stdout(listener);
            }
                        
            // Copy MATLAB scratch file into the workspace.
            FilePath targetWorkspace = new FilePath(launcher.getChannel(), workspace.getRemote());
            copyMatlabScratchFileInWorkspace(MatlabBuilderConstants.MATLAB_RUNNER_RESOURCE, MatlabBuilderConstants.MATLAB_RUNNER_TARGET_FILE, targetWorkspace);
        } catch (Exception e) {
            listener.getLogger().println(e.getMessage());
            return 1;
        }
        return matlabLauncher.join();
    }
    
    private void copyMatlabScratchFileInWorkspace(String matlabRunnerResourcePath,
            String matlabRunnerTarget, FilePath targetWorkspace)
            throws IOException, InterruptedException {
        final ClassLoader classLoader = getClass().getClassLoader();
        FilePath targetFile =
                new FilePath(targetWorkspace, Message.getValue(matlabRunnerTarget));
        InputStream in = classLoader.getResourceAsStream(matlabRunnerResourcePath);

        targetFile.copyFrom(in);
    }
    
    // Concatenate the input arguments
    private String getInputArguments() {
        String pdfReport = MatlabBuilderConstants.PDF_REPORT + "," + this.getTaPDFReportChkBx();
        String tapResults = MatlabBuilderConstants.TAP_RESULTS + "," + this.getTatapChkBx();
        String junitResults = MatlabBuilderConstants.JUNIT_RESULTS + "," + this.getTaJunitChkBx();
        String stmResults = MatlabBuilderConstants.STM_RESULTS + "," + this.getTaSTMResultsChkBx();
        String coberturaCodeCoverage = MatlabBuilderConstants.COBERTURA_CODE_COVERAGE + "," + this.getTaCoberturaChkBx();
        String coberturaModelCoverage = MatlabBuilderConstants.COBERTURA_MODEL_COVERAGE + "," + this.getTaModelCoverageChkBx();
        
        String inputArgsToMatlabFcn = pdfReport + "," + tapResults + "," + junitResults + ","
                + stmResults + "," + coberturaCodeCoverage + "," + coberturaModelCoverage;
        
        return inputArgsToMatlabFcn;
    }
    

}
