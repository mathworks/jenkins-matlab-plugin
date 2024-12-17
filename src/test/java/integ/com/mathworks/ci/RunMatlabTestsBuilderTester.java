package com.mathworks.ci;

/**
 * Copyright 2019-2024 The MathWorks, Inc.
 * 
 * Tester builder for RunMatlabTestsBuilder.
 */

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

import com.mathworks.ci.freestyle.RunMatlabTestsBuilder;

public class RunMatlabTestsBuilderTester extends RunMatlabTestsBuilder {

    private Artifact tapArtifact = new NullArtifact();;
    private Artifact junitArtifact = new NullArtifact();;
    private Artifact coberturaArtifact = new NullArtifact();;
    private Artifact stmResultsArtifact = new NullArtifact();;
    private Artifact modelCoverageArtifact = new NullArtifact();
    private Artifact pdfReportArtifact = new NullArtifact();;
    private EnvVars env;
    private int buildResult;
    private MatlabReleaseInfo matlabRel;
    private String matlabroot;
    private String commandParameter;
    private String matlabExecutorPath;
    private String matlabVerName;

    public RunMatlabTestsBuilderTester(String matlabExecutorPath, String customTestPointArgument) {
        super();
        this.commandParameter = customTestPointArgument;
        this.matlabExecutorPath = matlabExecutorPath;
    }

    public RunMatlabTestsBuilderTester(String customTestPointArgument) {
        super();
        this.commandParameter = customTestPointArgument;
    }

    // Getter and Setters to access local members

    @DataBoundSetter
    public void setTapChkBx(TapArtifact tapArtifact) {
        this.tapArtifact = tapArtifact;
    }

    @DataBoundSetter
    public void setJunitChkBx(JunitArtifact junitArtifact) {
        this.junitArtifact = junitArtifact;
    }

    @DataBoundSetter
    public void setCoberturaChkBx(CoberturaArtifact coberturaArtifact) {
        this.coberturaArtifact = coberturaArtifact;
    }

    @DataBoundSetter
    public void setStmResultsChkBx(StmResultsArtifact stmResultsArtifact) {
        this.stmResultsArtifact = stmResultsArtifact;
    }

    @DataBoundSetter
    public void setModelCoverageChkBx(ModelCovArtifact modelCoverageArtifact) {
        this.modelCoverageArtifact = modelCoverageArtifact;
    }

    @DataBoundSetter
    public void setPdfReportChkBx(PdfArtifact pdfReportArtifact) {
        this.pdfReportArtifact = pdfReportArtifact;
    }

    public Artifact getTapChkBx() {
        return tapArtifact;
    }

    public Artifact getJunitChkBx() {
        return junitArtifact;
    }

    public Artifact getCoberturaChkBx() {
        return coberturaArtifact;
    }

    public Artifact getStmResultsChkBx() {
        return stmResultsArtifact;
    }

    public Artifact getModelCoverageChkBx() {
        return modelCoverageArtifact;
    }

    public Artifact getPdfReportChkBx() {
        return pdfReportArtifact;
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
         * This is to identify which project type in jenkins this should be
         * applicable.(non-Javadoc)
         * 
         * @see hudson.tasks.BuildStepDescriptor#isApplicable(java.lang.Class)
         * 
         * if it returns true then this build step will be applicable for all project
         * type.
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
        if (this.matlabroot != null) {
            FilePath nodeSpecificMatlabRoot = new FilePath(launcher.getChannel(), matlabroot);
            matlabRel = new MatlabReleaseInfo(nodeSpecificMatlabRoot);
        }

        buildResult = execCommand(workspace, launcher, listener);
        if (buildResult != 0) {
            build.setResult(Result.FAILURE);
        }
    }

    public int execCommand(FilePath workspace, Launcher launcher, TaskListener listener)
            throws IOException, InterruptedException {
        if (this.matlabExecutorPath == null) {
            this.matlabVerName = this.env.get(Message.getValue("Axis.matlab.key"));
            this.matlabExecutorPath = MatlabInstallation.getInstallation(this.matlabVerName).getHome();
        }
        ProcStarter matlabLauncher;
        try {
            matlabLauncher = launcher.launch().pwd(workspace).envs(this.env).cmds(testMatlabCommand()).stdout(listener);
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
