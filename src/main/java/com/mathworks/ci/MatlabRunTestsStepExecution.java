package com.mathworks.ci;

/**
 * Copyright 2020 The MathWorks, Inc.
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

public class MatlabRunTestsStepExecution extends SynchronousNonBlockingStepExecution<Void> implements MatlabBuild {

    private static final long serialVersionUID = 6704588180717665100L;
    
    private String commandArgs;


    public MatlabRunTestsStepExecution(StepContext context, String commandArgs) {
        super(context);
        this.commandArgs = commandArgs;
    }

    private String getCommandArgs() {
        return this.commandArgs;
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
        try {
            FilePath genScriptLocation =
                    getFilePathForUniqueFolder(launcher, uniqueTmpFldrName, workspace);
            final String cmdPrefix =
                    "addpath('" + genScriptLocation.getRemote().replaceAll("'", "''") + "'); ";
            final String matlabScriptName = getValidMatlabFileName(genScriptLocation.getBaseName());

            ProcStarter matlabLauncher = getProcessToRunMatlabCommand(workspace, launcher, listener,
                    envVars, cmdPrefix + matlabScriptName, uniqueTmpFldrName);
            
            // prepare temp folder by coping genscript package and writing runner script.
            prepareTmpFldr(genScriptLocation,
                    getRunnerScript(MatlabBuilderConstants.TEST_RUNNER_SCRIPT, envVars.expand(getCommandArgs())));
                               
            return matlabLauncher.pwd(workspace).join();
        } finally {
            // Cleanup the runner File from tmp directory
            final FilePath matlabRunnerScript =
                    getFilePathForUniqueFolder(launcher, uniqueTmpFldrName, workspace);
            if (matlabRunnerScript.exists()) {
                matlabRunnerScript.deleteRecursive();
            }
        }

    }
}
