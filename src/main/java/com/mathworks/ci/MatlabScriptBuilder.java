package com.mathworks.ci;
/*
 * Copyright 2020-2021 The MathWorks, Inc.
 * 
 * Script builder used to run custom MATLAB commands or scripts. Author : Nikhil Bhoski email :
 * nbhoski@mathworks.com Date : 11/02/2020
 * 
 */

import java.io.IOException;
import javax.annotation.Nonnull;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
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
import jenkins.tasks.SimpleBuildStep;
import net.sf.json.JSONObject;

public class MatlabScriptBuilder extends Builder implements SimpleBuildStep {
    private int buildResult;
    private EnvVars env;
    private MatlabReleaseInfo matlabRel;
    private CommandConstructUtil cmdUtils;
    private String matlabCommand;

    @DataBoundConstructor
    public MatlabScriptBuilder() {


    }


    // Getter and Setters to access local members


    @DataBoundSetter
    public void setMatlabCommand(String matlabCommand) {
        this.matlabCommand = matlabCommand;
    }

    public String getMatlabCommand() {
        return this.matlabCommand;
    }

    private String getCommand() {
        return this.env == null ? getMatlabCommand() : this.env.expand(getMatlabCommand());
    }

    private void setEnv(EnvVars env) {
        this.env = env;
    }

    @Symbol("runMatlabCommand")
    @Extension
    public static class MatlabScriptDescriptor extends BuildStepDescriptor<Builder> {


        MatlabReleaseInfo rel;


        // Overridden Method used to show the text under build dropdown
        @Override
        public String getDisplayName() {
            return Message.getValue("Builder.script.builder.display.name");
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

        // Create command util for command constrcution.
        String matlabRoot = this.env.get("matlabroot");
        cmdUtils = new CommandConstructUtil(launcher, matlabRoot);

        // Get node specific matlabroot to get matlab version information
        FilePath nodeSpecificMatlabRoot = new FilePath(launcher.getChannel(), matlabRoot);
        matlabRel = new MatlabReleaseInfo(nodeSpecificMatlabRoot);

        // Invoke MATLAB command and transfer output to standard
        // Output Console

        buildResult = execMatlabCommand(workspace, launcher, listener);

        if (buildResult != 0) {
            build.setResult(Result.FAILURE);
        }
    }

    private synchronized int execMatlabCommand(FilePath workspace, Launcher launcher,
            TaskListener listener) throws IOException, InterruptedException {
        ProcStarter matlabLauncher;
        try {
            matlabLauncher = launcher.launch().pwd(workspace).envs(this.env);
            if (matlabRel.verLessThan(MatlabBuilderConstants.BASE_MATLAB_VERSION_BATCH_SUPPORT)) {
                ListenerLogDecorator outStream = new ListenerLogDecorator(listener);
                matlabLauncher = matlabLauncher
                        .cmds(cmdUtils.constructDefaultCommandForScriptRun(getCommand()))
                        .stderr(outStream);
            } else {
                matlabLauncher = matlabLauncher
                        .cmds(cmdUtils.constructBatchCommandForScriptRun(getCommand()))
                        .stdout(listener);
            }

        } catch (Exception e) {
            listener.getLogger().println(e.getMessage());
            return 1;
        }
        return matlabLauncher.join();
    }
}
