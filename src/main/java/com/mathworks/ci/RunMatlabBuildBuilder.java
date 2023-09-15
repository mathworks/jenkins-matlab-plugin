package com.mathworks.ci;

/**
 * Copyright 2022 The MathWorks, Inc.
 *  
 */

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

    @DataBoundConstructor
    public RunMatlabBuildBuilder() {}

    // Getter and Setters to access local members
    @DataBoundSetter
    public void setTasks(String tasks) {
        this.tasks = tasks;
    }

    public String getTasks() {
        return this.tasks;
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
        final FilePath uniqeTmpFolderPath =
                getFilePathForUniqueFolder(launcher, uniqueTmpFldrName, workspace);

        // Create MATLAB script
        createMatlabScriptByName(uniqeTmpFolderPath, uniqueBuildFile, workspace, listener, envVars);
        ProcStarter matlabLauncher;
        BuildConsoleAnnotator bca = new BuildConsoleAnnotator(listener.getLogger(),build.getCharset());
        try {

            matlabLauncher = getProcessToRunMatlabCommandb(workspace, launcher, listener, envVars,
                    "cd('"+ uniqeTmpFolderPath.getRemote().replaceAll("'", "''") +"');"+ uniqueBuildFile, uniqueTmpFldrName, bca);
            
            listener.getLogger()
                    .println("#################### Starting command output ####################");
            return matlabLauncher.pwd(workspace).join();

        } catch (Exception e) {
            listener.getLogger().println(e.getMessage());
            return 1;
        } finally {
            bca.forceEol();
            // Cleanup the tmp directory
            if (uniqeTmpFolderPath.exists()) {
                uniqeTmpFolderPath.deleteRecursive();
            }
        }
    }
    
    private void createMatlabScriptByName(FilePath uniqeTmpFolderPath, String uniqueScriptName, FilePath workspace, TaskListener listener, EnvVars envVars) throws IOException, InterruptedException {

        // Create a new command runner script in the temp folder.
        final FilePath matlabCommandFile =
                new FilePath(uniqeTmpFolderPath, uniqueScriptName + ".m");
        final String tasks = envVars.expand(getTasks());
        String cmd = "buildtool";

        if (!tasks.trim().isEmpty()) {
            cmd += " " + tasks;
        }

        final String matlabCommandFileContent =
                "cd '" + workspace.getRemote().replaceAll("'", "''") + "';\n" + cmd;

        // Display the commands on console output for users reference
        listener.getLogger()
                .println("Generating MATLAB script with content:\n" + cmd + "\n");

        matlabCommandFile.write(matlabCommandFileContent, "UTF-8");
    }

    public ProcStarter getProcessToRunMatlabCommandb(FilePath workspace,
        Launcher launcher, TaskListener listener, EnvVars envVars, String matlabCommand, String uniqueName, BuildConsoleAnnotator bca)
        throws IOException, InterruptedException {
        // Get node specific temp .matlab directory to copy matlab runner script
        FilePath targetWorkspace = new FilePath(launcher.getChannel(),
            workspace.getRemote() + "/" + MatlabBuilderConstants.TEMP_MATLAB_FOLDER_NAME);
        ProcStarter matlabLauncher;
        if (launcher.isUnix()) {
            final String runnerScriptName = uniqueName + "/run_matlab_command.sh";
            matlabLauncher = launcher.launch().envs(envVars);
            matlabLauncher.cmds(MatlabBuilderConstants.TEMP_MATLAB_FOLDER_NAME + "/" + runnerScriptName, matlabCommand).stdout(bca);

            // Copy runner .sh for linux platform in workspace.
            copyFileInWorkspace(MatlabBuilderConstants.SHELL_RUNNER_SCRIPT, runnerScriptName,
                targetWorkspace);
        } else {
            final String runnerScriptName = uniqueName + "\\run_matlab_command.bat";
            launcher = launcher.decorateByPrefix("cmd.exe", "/C");
            matlabLauncher = launcher.launch().envs(envVars);
            matlabLauncher.cmds(MatlabBuilderConstants.TEMP_MATLAB_FOLDER_NAME + "\\" + runnerScriptName, "\"" + matlabCommand + "\"")
                .stdout(bca);
            // Copy runner.bat for Windows platform in workspace.
            copyFileInWorkspace(MatlabBuilderConstants.BAT_RUNNER_SCRIPT, runnerScriptName,
                targetWorkspace);
        }
        return matlabLauncher;
    }
}
