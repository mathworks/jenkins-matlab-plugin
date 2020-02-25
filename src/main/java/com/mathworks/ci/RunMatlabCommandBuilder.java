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

public class RunMatlabCommandBuilder extends Builder implements SimpleBuildStep {
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
    public static class MatlabScriptDescriptor extends BuildStepDescriptor<Builder> {

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
            // Get mMatlabroot set in wrapper.
            String matlabRoot = envVars.get("matlabroot");
            CommandConstructUtil cmdUtils = new CommandConstructUtil(launcher, matlabRoot);
            matlabLauncher = launcher.launch().pwd(workspace).envs(envVars);
            FilePath targetWorkspace = new FilePath(launcher.getChannel(), workspace.getRemote());
            if(launcher.isUnix()) {
                matlabLauncher = launcher.launch().pwd(workspace).envs(envVars).cmds("/bin/bash","-c","./run_matlab_command.sh "+cmdUtils.constructCommandForRunCommand(getCommand())).stdout(listener);
                //Copy runner .sh for linux platform in workspace.
                cmdUtils.copyMatlabScratchFileInWorkspace(MatlabBuilderConstants.SHELL_RUNNER_SCRIPT, "Builder.matlab.runner.script.target.file.linux.name", targetWorkspace);
            }else {
                launcher = launcher.decorateByPrefix("cmd.exe","/C");
                matlabLauncher = launcher.launch().pwd(workspace).envs(envVars).cmds("run_matlab_command.bat","\""+cmdUtils.constructCommandForRunCommand(getCommand())+"\"").stdout(listener);
                //Copy runner.bat for Windows platform in workspace.
                cmdUtils.copyMatlabScratchFileInWorkspace(MatlabBuilderConstants.BAT_RUNNER_SCRIPT, "Builder.matlab.runner.script.target.file.windows.name", targetWorkspace);
            }
        } catch (Exception e) {
            listener.getLogger().println(e.getMessage());
            return 1;
        }
        return matlabLauncher.join();
    }
}
