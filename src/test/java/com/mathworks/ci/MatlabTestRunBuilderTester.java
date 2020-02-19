package com.mathworks.ci;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nonnull;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.StaplerRequest;
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
import net.sf.json.JSONObject;

public class MatlabTestRunBuilderTester extends MatlabTestRunBuilder {

    private boolean tapChkBx;
    private boolean junitChkBx;
    private boolean coberturaChkBx;
    private boolean stmResultsChkBx;
    private boolean modelCoverageChkBx;
    private boolean pdfReportChkBx;
    private EnvVars env;
    private int buildResult;
    private MatlabReleaseInfo matlabRel;
    private String matlabroot;
    private String commandParameter;
    private String matlabExecutorPath;



    public MatlabTestRunBuilderTester(String matlabExecutorPath, String customTestPointArgument) {
        super();
        this.commandParameter = customTestPointArgument;
        this.matlabExecutorPath = matlabExecutorPath;
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

    public void geetEnv(EnvVars env) {
        this.env = env;
    }

    public String getMatlabRoot() {
        return this.matlabroot;
    }

    @Extension
    public static class Descriptor extends BuildStepDescriptor<Builder> {
        @Override
        public String getDisplayName() {
            return null;
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

        this.matlabroot = this.env.get("matlabroot");
        // Get node specific matlabroot to get matlab version information
        FilePath nodeSpecificMatlabRoot = new FilePath(launcher.getChannel(), matlabroot);
        matlabRel = new MatlabReleaseInfo(nodeSpecificMatlabRoot);

        // Invoke MATLAB command and transfer output to standard
        // Output Console

        buildResult = execCommand(workspace, launcher, listener);
        if (buildResult != 0) {
            build.setResult(Result.FAILURE);
        }
    }

    public int execCommand(FilePath workspace, Launcher launcher, TaskListener listener)
            throws IOException, InterruptedException {
        ProcStarter matlabLauncher;
        try {
            matlabLauncher = launcher.launch().pwd(workspace).envs(this.env);
            if (matlabRel.verLessThan(MatlabBuilderConstants.BASE_MATLAB_VERSION_BATCH_SUPPORT)) {
                ListenerLogDecorator outStream = new ListenerLogDecorator(listener);
                matlabLauncher = matlabLauncher.cmds(testMatlabCommand()).stderr(outStream);
            } else {
                matlabLauncher = matlabLauncher.cmds(testMatlabCommand()).stdout(listener);
            }

        } catch (Exception e) {
            listener.getLogger().println(e.getMessage());
            return 1;
        }
        return matlabLauncher.join();

    }

    private List<String> testMatlabCommand() {
        List<String> matlabDefaultArgs = new ArrayList<String>();
        matlabDefaultArgs.add(this.matlabExecutorPath);
        matlabDefaultArgs.add(this.commandParameter);
        return matlabDefaultArgs;
    }


}
