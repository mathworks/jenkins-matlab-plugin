package com.mathworks.ci;

/**
 * Copyright 2022-2023 The MathWorks, Inc.
 *  
 */

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

    public MatlabBuildStepExecution(StepContext context, String tasks, String startupOptions) {
        super(context);
        this.tasks = tasks;
        this.startupOptions = startupOptions;
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
        
        //Make sure the Workspace exists before run
        
        workspace.mkdirs();
        
        int exitCode = execMatlabCommand(workspace, launcher, listener, env);

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
            TaskListener listener, EnvVars envVars) throws IOException, InterruptedException {
        final String uniqueTmpFldrName = getUniqueNameForRunnerFile();  
        final String uniqueBuildFile =
                "build_" + getUniqueNameForRunnerFile().replaceAll("-", "_");
        final FilePath uniqueTmpFolderPath =
                getFilePathForUniqueFolder(launcher, uniqueTmpFldrName, workspace);

        // Create MATLAB script
        createMatlabScriptByName(uniqueTmpFolderPath, uniqueBuildFile, listener);
        ProcStarter matlabLauncher;

        try {
            matlabLauncher = getProcessToRunMatlabCommand(workspace, launcher, listener, envVars,
                    "setenv('MW_ORIG_WORKING_FOLDER', cd('"+ uniqueTmpFolderPath.getRemote().replaceAll("'", "''") +"')); "+ uniqueBuildFile, startupOptions, uniqueTmpFldrName);
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
    
    private void createMatlabScriptByName(FilePath uniqueTmpFolderPath, String uniqueScriptName, TaskListener listener) throws IOException, InterruptedException {

        // Create a new command runner script in the temp folder.
        final FilePath matlabBuildFile =
                new FilePath(uniqueTmpFolderPath, uniqueScriptName + ".m");
        final String tasks = getContext().get(EnvVars.class).expand(getTasks());
        String cmd = "buildtool";

        if (!tasks.trim().isEmpty()) {
            cmd += " " + tasks;
        }
        final String matlabBuildFileContent =
                "cd(getenv('MW_ORIG_WORKING_FOLDER'));\n" + cmd;

        // Display the commands on console output for users reference
        listener.getLogger()
                .println("Generating MATLAB script with content:\n" + cmd + "\n");

        matlabBuildFile.write(matlabBuildFileContent, "UTF-8");
    }
}
