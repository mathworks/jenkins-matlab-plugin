package com.mathworks.ci;

/**
 * Copyright 2022-2023 The MathWorks, Inc.
 *  
 */

import hudson.model.Run;
import hudson.util.ArgumentListBuilder;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.SynchronousNonBlockingStepExecution;
import hudson.EnvVars;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Launcher.ProcStarter;
import hudson.model.Result;
import hudson.model.TaskListener;

public class MatlabBuildStepExecution extends SynchronousNonBlockingStepExecution<Void> implements MatlabBuild {
    
    private static final long serialVersionUID = 4771831219402275744L;
    
    private String tasks;
    private String startupOptions;
    private String buildOptions;
    private static String DEFAULT_PLUGIN = "+ciplugins/+jenkins/getDefaultPlugins.m";
    private static String BUILD_REPORT_PLUGIN = "+ciplugins/+jenkins/BuildReportPlugin.m";
    private static String TASK_RUN_PROGRESS_PLUGIN = "+ciplugins/+jenkins/TaskRunProgressPlugin.m";

    public MatlabBuildStepExecution(StepContext context, String tasks, String startupOptions, String buildOptions) {
        super(context);
        this.tasks = tasks;
        this.startupOptions = startupOptions;
        this.buildOptions = buildOptions;
    }

    private String getTasks() {
        return this.tasks;
    }

    @Override
    public Void run() throws Exception {
        final Launcher launcher = getContext().get(Launcher.class);
        final FilePath workspace = getContext().get(FilePath.class);
        final TaskListener listener = getContext().get(TaskListener.class);
        final EnvVars env = getContext().get(EnvVars.class);
        final Run<?,?> build =  getContext().get(Run.class);
        
        //Make sure the Workspace exists before run
        
        workspace.mkdirs();
        System.out.println("THE ROOT DIR IS"+ build.getRootDir().toString());
        
        int exitCode = execMatlabCommand(workspace, launcher, listener, env, build);
        //Add build result action
        FilePath jsonFile = new FilePath(workspace, ".matlab/buildArtifact.json");
        if(jsonFile.exists()){
            jsonFile.copyTo(new FilePath(new File(build.getRootDir().getAbsolutePath()+"/buildArtifact.json")));
            jsonFile.delete();
        }
        build.addAction(new BuildArtifactAction(build, workspace));

        if(exitCode != 0){
            // throw an exception if return code is non-zero
            stop(new MatlabExecutionException(exitCode));
        }

        getContext().setResult(Result.SUCCESS);
        return null;
    }

    @Override
    public void stop(Throwable cause) throws Exception {
        getContext().onFailure(cause);
    }
    
    private int execMatlabCommand(FilePath workspace, Launcher launcher,
            TaskListener listener, EnvVars envVars, Run<?,?> build) throws IOException, InterruptedException {
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

        try (BuildConsoleAnnotator bca = new BuildConsoleAnnotator(listener.getLogger(), build.getCharset())) {
            matlabLauncher = getProcessToRunMatlabCommand(workspace, launcher, bca, envVars,
                    "setenv('MW_ORIG_WORKING_FOLDER', cd('"+ uniqueTmpFolderPath.getRemote().replaceAll("'", "''") +"'));"+ uniqueBuildFile, startupOptions, uniqueTmpFldrName);
            listener.getLogger()
                    .println("#################### Starting command output ####################");
            return matlabLauncher.pwd(workspace).join();

        } finally {
            // Cleanup the tmp directory
            if (uniqueTmpFolderPath.exists()) {
                uniqueTmpFolderPath.deleteRecursive();
            }
        }
    }
    
    private void createMatlabScriptByName(FilePath uniqueTmpFolderPath, String uniqueScriptName, TaskListener listener, EnvVars envVars) throws IOException, InterruptedException {

        // Create a new command runner script in the temp folder.
        final FilePath matlabBuildFile =
                new FilePath(uniqueTmpFolderPath, uniqueScriptName + ".m");
        final String tasks = getContext().get(EnvVars.class).expand(getTasks());
        final String buildOptions = this.buildOptions;

        // Set ENV variable to override the default plugin list
        envVars.put("MW_MATLAB_BUILDTOOL_DEFAULT_PLUGINS_FCN_OVERRIDE", "ciplugins.jenkins.getDefaultPlugins");
        String cmd = "buildtool";

        if (!tasks.trim().isEmpty()) {
            cmd += " " + tasks;
        }

        if (!buildOptions.trim().isEmpty()) {
            cmd += " " + buildOptions;
        }

        final String matlabBuildFileContent =
                "addpath(pwd);cd(getenv('MW_ORIG_WORKING_FOLDER'));\n" + cmd;

        // Display the commands on console output for users reference
        listener.getLogger()
                .println("Generating MATLAB script with content:\n" + cmd + "\n");

        matlabBuildFile.write(matlabBuildFileContent, "UTF-8");
    }

    public ProcStarter getProcessToRunMatlabCommand(FilePath workspace,
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
