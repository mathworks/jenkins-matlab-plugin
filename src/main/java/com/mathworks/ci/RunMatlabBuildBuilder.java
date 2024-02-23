package com.mathworks.ci;

/**
 * Copyright 2022-2023 The MathWorks, Inc.
 *  
 */

import hudson.util.ArgumentListBuilder;
import java.io.ByteArrayOutputStream;
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
import jenkins.tasks.SimpleBuildStep;
import net.sf.json.JSONObject;

public class RunMatlabBuildBuilder extends Builder implements SimpleBuildStep, MatlabBuild {
    private int buildResult;
    private String tasks;
    private StartupOptions startupOptions;
    private BuildOptions buildOptions;
    private static String DEFAULT_PLUGIN = "+ciplugins/+jenkins/getDefaultPlugins.m";
    private static String BUILD_REPORT_PLUGIN = "+ciplugins/+jenkins/BuildReportPlugin.m";
    private static String TASK_RUN_PROGRESS_PLUGIN = "+ciplugins/+jenkins/TaskRunProgressPlugin.m";

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

    @DataBoundSetter
    public void setBuildOptions (BuildOptions buildOptions) {
        this.buildOptions = buildOptions;
    }

    public String getTasks() {
        return this.tasks;
    }

    public StartupOptions getStartupOptions() {
        return this.startupOptions;
    }

    public BuildOptions getBuildOptions() {
        return this.buildOptions;
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
        createMatlabScriptByName(uniqueTmpFolderPath, uniqueBuildFile, listener, envVars);
        // Copy JenkinsLogging plugin in temp folder
        copyFileInWorkspace(DEFAULT_PLUGIN,DEFAULT_PLUGIN,uniqueTmpFolderPath);
        copyFileInWorkspace(BUILD_REPORT_PLUGIN,BUILD_REPORT_PLUGIN,uniqueTmpFolderPath);
        copyFileInWorkspace(TASK_RUN_PROGRESS_PLUGIN,TASK_RUN_PROGRESS_PLUGIN,uniqueTmpFolderPath);
      
        ProcStarter matlabLauncher;
        BuildConsoleAnnotator bca = new BuildConsoleAnnotator(listener.getLogger(), build.getCharset());
        String options = getStartupOptions() == null ? "" : getStartupOptions().getOptions();
        try {
            matlabLauncher = getProcessToRunMatlabCommand(workspace, launcher, bca, envVars,
                    "setenv('MW_ORIG_WORKING_FOLDER', cd('"+ uniqueTmpFolderPath.getRemote().replaceAll("'", "''") +"'));"+ uniqueBuildFile, options, uniqueTmpFldrName);

            
            listener.getLogger()
                    .println("#################### Starting command output ####################");
            return matlabLauncher.pwd(workspace).join();

        } catch (Exception e) {
            listener.getLogger().println(e.getMessage());
            return 1;
        } finally {
            bca.forceEol();
            // Cleanup the tmp directory
            if (uniqueTmpFolderPath.exists()) {
                uniqueTmpFolderPath.deleteRecursive();
            }
        }
    }
    
    private void createMatlabScriptByName(FilePath uniqueTmpFolderPath, String uniqueScriptName, TaskListener listener, EnvVars envVars) throws IOException, InterruptedException {

        // Create a new command runner script in the temp folder.
        final FilePath matlabCommandFile =
                new FilePath(uniqueTmpFolderPath, uniqueScriptName + ".m");
        final String tasks = envVars.expand(getTasks());
        final String buildOptions = getBuildOptions() == null ? "": getBuildOptions().getOptions();

        // Set ENV variable to override the default plugin list
        envVars.put("MW_MATLAB_BUILDTOOL_DEFAULT_PLUGINS_FCN_OVERRIDE", "ciplugins.jenkins.getDefaultPlugins");

        String cmd = "buildtool";

        if (!tasks.trim().isEmpty()) {
            cmd += " " + tasks;
        }

        if (!buildOptions.trim().isEmpty()) {
            cmd += " " + buildOptions;
        }

        final String matlabCommandFileContent =
                "addpath(pwd);cd(getenv('MW_ORIG_WORKING_FOLDER'));\n" + cmd;

        // Display the commands on console output for users reference
        listener.getLogger()
                .println("Generating MATLAB script with content:\n" + cmd + "\n");

        matlabCommandFile.write(matlabCommandFileContent, "UTF-8");
    }


    private ProcStarter getProcessToRunMatlabCommand(FilePath workspace,
                                                    Launcher launcher, BuildConsoleAnnotator bca, EnvVars envVars, String matlabCommand, String startupOpts, String uniqueName)
            throws IOException, InterruptedException {
        // Get node specific temp .matlab directory to copy matlab runner script
        FilePath targetWorkspace;
        ProcStarter matlabLauncher;
        ArgumentListBuilder args = new ArgumentListBuilder();
        if (launcher.isUnix()) {
            targetWorkspace = new FilePath(launcher.getChannel(),
                    workspace.getRemote() + "/" + MatlabBuilderConstants.TEMP_MATLAB_FOLDER_NAME);

            // Determine whether we're on Mac on Linux
            ByteArrayOutputStream kernelStream = new ByteArrayOutputStream();
            launcher.launch()
                    .cmds("uname")
                    .masks(true)
                    .stdout(kernelStream)
                    .join();

            String binaryName;
            String runnerName = uniqueName + "/run-matlab-command";
            if (kernelStream.toString("UTF-8").contains("Linux")) {
                binaryName = "glnxa64/run-matlab-command";
            } else {
                binaryName = "maci64/run-matlab-command";
            }

            args.add(MatlabBuilderConstants.TEMP_MATLAB_FOLDER_NAME + "/" + runnerName);
            args.add(matlabCommand);
            args.add(startupOpts.split(" "));

            matlabLauncher = launcher.launch().envs(envVars).cmds(args).stdout(bca);

            // Copy runner for linux platform in workspace.
            copyFileInWorkspace(binaryName, runnerName, targetWorkspace);
        } else {
            targetWorkspace = new FilePath(launcher.getChannel(),
                    workspace.getRemote() + "\\" + MatlabBuilderConstants.TEMP_MATLAB_FOLDER_NAME);

            final String runnerName = uniqueName + "\\run-matlab-command.exe";

            args.add(targetWorkspace.toString() + "\\" + runnerName, "\"" + matlabCommand + "\"");
            args.add(startupOpts.split(" "));

            matlabLauncher = launcher.launch().envs(envVars).cmds(args).stdout(bca);

            // Copy runner for Windows platform in workspace.
            copyFileInWorkspace("win64/run-matlab-command.exe", runnerName,
                    targetWorkspace);
        }
        return matlabLauncher;
    }
}
