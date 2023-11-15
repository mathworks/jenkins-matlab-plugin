package com.mathworks.ci;

/**
 * Copyright 2022-2023 The MathWorks, Inc.
 *  
 */

import java.io.File;
import java.io.IOException;
import javax.annotation.Nonnull;
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
import hudson.model.Computer;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.Util;
import jenkins.tasks.SimpleBuildStep;
import net.sf.json.JSONObject;

public class RunMatlabBuildBuilder extends Builder implements SimpleBuildStep, MatlabBuild {
    private int buildResult;
    private String tasks;
    private StartupOptions startupOptions;

    @DataBoundConstructor
    public RunMatlabBuildBuilder() {}

    // Getter and Setters to access local members
    @DataBoundSetter
    public void setTasks(String tasks) {
        this.tasks = tasks;
    }

    @DataBoundSetter
    public void setStartupOptions(StartupOptions startupOptions) {
        this.startupOptions = startupOptions;
    }

    public String getTasks() {
        return this.tasks;
    }

    public StartupOptions getStartupOptions() {
        return this.startupOptions;
    }
    
    @Extension
    public static class RunMatlabBuildDescriptor extends BuildStepDescriptor<Builder> {

        // Overridden Method used to show the text under build dropdown
        @Override
        public String getDisplayName() {
            return Message.getValue("Builder.build.builder.display.name");
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

        // Get the environment variable specific to the this build
        final EnvVars env = build.getEnvironment(listener);

        // Invoke MATLAB build and transfer output to standard
        // Output Console

        buildResult = execMatlabCommand(workspace, launcher, listener, env, build);

        //Add build result action
        FilePath jsonFile = new FilePath(workspace, ".matlab/buildArtifact.json");
        if(jsonFile.exists()){
            jsonFile.copyTo(new FilePath(new File(build.getRootDir().getAbsolutePath()+"/buildArtifact.json")));
            jsonFile.delete();
        }
        build.addAction(new BuildArtifactAction(build, workspace));

        if (buildResult != 0) {
            build.setResult(Result.FAILURE);
        }
    }

    private int execMatlabCommand(FilePath workspace, Launcher launcher,
            TaskListener listener, EnvVars envVars, @Nonnull Run<?, ?> build) throws IOException, InterruptedException {

        /*
         * Handle the case for using MATLAB Axis for multi conf projects by adding appropriate
         * matlabroot to env PATH
         * */
        Utilities.addMatlabToEnvPathFrmAxis(Computer.currentComputer(), listener, envVars);

        final String uniqueTmpFldrName = getUniqueNameForRunnerFile();
        final String uniqueBuildFile =
                "build_" + getUniqueNameForRunnerFile().replaceAll("-", "_");
        final FilePath uniqueTmpFolderPath =
                getFilePathForUniqueFolder(launcher, uniqueTmpFldrName, workspace);

        // Create MATLAB script
        createMatlabScriptByName(uniqueTmpFolderPath, uniqueBuildFile, workspace, listener, envVars);
        // Copy buildRunner in temp folder
        copyFileInWorkspace("buildRunner.m","buildRunner.m",uniqueTmpFolderPath);
        ProcStarter matlabLauncher;
        String options = getStartupOptions() == null ? "" : getStartupOptions().getOptions();
        try {
            matlabLauncher = getProcessToRunMatlabCommand(workspace, launcher, listener, envVars,
                    "cd('"+ uniqueTmpFolderPath.getRemote().replaceAll("'", "''") +"');"+ uniqueBuildFile, options, uniqueTmpFldrName);
            
            listener.getLogger()
                    .println("#################### Starting command output ####################");
            return matlabLauncher.pwd(workspace).join();

        } catch (Exception e) {
            listener.getLogger().println(e.getMessage());
            return 1;
        } finally {
            // Cleanup the tmp directory
            if (uniqueTmpFolderPath.exists()) {
                uniqueTmpFolderPath.deleteRecursive();
            }
        }
    }
    
    private void createMatlabScriptByName(FilePath uniqueTmpFolderPath, String uniqueScriptName, FilePath workspace, TaskListener listener, EnvVars envVars) throws IOException, InterruptedException {

        // Create a new command runner script in the temp folder.
        final FilePath matlabCommandFile =
                new FilePath(uniqueTmpFolderPath, uniqueScriptName + ".m");
        final String tasks = envVars.expand(getTasks());

        String buildScript = "buildRunner('"+tasks+"')";

        final String matlabCommandFileContent =
                "addpath(pwd);cd '" + workspace.getRemote().replaceAll("'", "''") + "';\n" + buildScript;

        // Display the commands on console output for users reference
        listener.getLogger()
                .println("Generating MATLAB script with content:\n" + buildScript + "\n");

        matlabCommandFile.write(matlabCommandFileContent, "UTF-8");
    }
}
