package com.mathworks.ci;
/**
 * Copyright 2019-2020 The MathWorks, Inc.
 * 
 * Script builder used to run custom MATLAB commands or scripts.
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

public class RunMatlabCommandBuilder extends Builder implements SimpleBuildStep, MatlabBuild {
    private int buildResult;
    private EnvVars env;
    private String matlabCommand;

    @DataBoundConstructor
    public RunMatlabCommandBuilder() {

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

    private EnvVars getEnv() {
        return this.env;
    }

    @Symbol("RunMatlabCommand")
    @Extension
    public static class RunMatlabCommandDescriptor extends BuildStepDescriptor<Builder> {

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
        
        try {
            // Set the environment variable specific to the this build
            setEnv(build.getEnvironment(listener));

            // Invoke MATLAB command and transfer output to standard
            // Output Console

            buildResult = execMatlabCommand(workspace, launcher, listener, getEnv());

            if (buildResult != 0) {
                build.setResult(Result.FAILURE);
            }
        } finally {
            // Cleanup the runner File from tmp directory
            FilePath matlabRunnerScript = getNodeSpecificMatlabRunnerScript(launcher);
            if(matlabRunnerScript.exists()) {
                matlabRunnerScript.delete();
            }
        }
    }

    private synchronized int execMatlabCommand(FilePath workspace, Launcher launcher,
            TaskListener listener, EnvVars envVars) throws IOException, InterruptedException {
        ProcStarter matlabLauncher;
        try {
            matlabLauncher = getProcessToRunMatlabCommand(workspace, launcher, listener, envVars,getCommand());
        } catch (Exception e) {
            listener.getLogger().println(e.getMessage());
            return 1;
        }
        return matlabLauncher.join();
    } 
}
