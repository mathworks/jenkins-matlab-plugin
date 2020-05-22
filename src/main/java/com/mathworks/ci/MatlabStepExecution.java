package com.mathworks.ci;

/**
 * Copyright 2020 The MathWorks, Inc.
 *  
 */

import java.io.IOException;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepExecution;
import hudson.EnvVars;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Launcher.ProcStarter;
import hudson.model.Result;
import hudson.model.TaskListener;

public class MatlabStepExecution extends StepExecution implements MatlabBuild {
    private static final long serialVersionUID = 1L;
    private String command;


    public MatlabStepExecution(StepContext context, String command) {
        super(context);
        this.command = command;
    }

    private String getCommand() {
        return this.command;
    }

    @Override
    public boolean start() throws Exception {
        Launcher launcher = getContext().get(Launcher.class);
        FilePath workspace = getContext().get(FilePath.class);
        TaskListener listener = getContext().get(TaskListener.class);
        EnvVars env = getContext().get(EnvVars.class);
        

        int res = execMatlabCommand(workspace, launcher, listener, env);
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
                    envVars.expand(getCommand()), uniqueTmpFldrName);
            
                     
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
