package com.mathworks.ci;

import java.io.IOException;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepExecution;
import hudson.EnvVars;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Launcher.ProcStarter;
import hudson.model.Result;
import hudson.model.TaskListener;

public class MatlabCommandStepExecution extends StepExecution implements MatlabBuild {
    private static final long serialVersionUID = 1L;
    private String command;
    private EnvVars env;
    private boolean copyScratchFile;


    public MatlabCommandStepExecution(StepContext context, String command, boolean copyScratchFile) {
        super(context);
        this.command = command;
        this.copyScratchFile = copyScratchFile;
    }

    private String getCommand() {
        return this.env == null ? getMatlabCommand() : this.env.expand(getMatlabCommand());
    }

    private String getMatlabCommand() {
        return this.command;
    }

    private void setEnv(EnvVars env) {
        this.env = env;
    }

    private EnvVars getEnv() {
        return this.env;
    }

    @Override
    public boolean start() throws Exception {
        Launcher launcher = getContext().get(Launcher.class);
        FilePath workspace = getContext().get(FilePath.class);
        TaskListener listener = getContext().get(TaskListener.class);
        EnvVars env = getContext().get(EnvVars.class);
        setEnv(env);

        int res = execMatlabCommand(workspace, launcher, listener, getEnv());
        if (res == 0) {
            getContext().setResult(Result.SUCCESS);
        } else {
            getContext().setResult(Result.FAILURE);
        }
        getContext().onSuccess(true);
        
        //return false represents the asynchronous run. 
        return false;
    }

    @Override
    public void stop(Throwable cause) throws Exception {
        getContext().onFailure(cause);
    }

    private synchronized int execMatlabCommand(FilePath workspace, Launcher launcher,
            TaskListener listener, EnvVars envVars) throws IOException, InterruptedException {
        final String uniqueTmpFldrName = getUniqueNameForRunnerFile();
        ProcStarter matlabLauncher;
        try {
            matlabLauncher = getProcessToRunMatlabCommand(workspace, launcher, listener, envVars,
                    getCommand(), uniqueTmpFldrName);
            
            // Copy MATLAB scratch file into the workspace.
            FilePath targetWorkspace = new FilePath(launcher.getChannel(), workspace.getRemote());
            copyFileInWorkspace(MatlabBuilderConstants.MATLAB_TESTS_RUNNER_RESOURCE,
                    MatlabBuilderConstants.MATLAB_TESTS_RUNNER_TARGET_FILE, targetWorkspace);
            
            return matlabLauncher.join();
        } catch (Exception e) {
            listener.getLogger().println(e.getMessage());
            return 1;
        } finally {
            // Cleanup the runner File from tmp directory
            FilePath matlabRunnerScript =
                    getFilePathForUniqueFolder(launcher, uniqueTmpFldrName, workspace);
            if (matlabRunnerScript.exists()) {
                matlabRunnerScript.deleteRecursive();
            }
        }

    }
}
