package com.mathworks.ci;

/**
 * Copyright 2020 The MathWorks, Inc.
 *  
 */

import java.io.IOException;
import java.util.Map;
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
    
    private Map<String,String> command;


    public MatlabRunTestsStepExecution(StepContext context, Map<String,String> command) {
        super(context);
        this.command = command;
    }

    private Map<String,String> getCommand() {
        return this.command;
    }

    @Override
    public Void run() throws Exception {
        final Launcher launcher = getContext().get(Launcher.class);
        final FilePath workspace = getContext().get(FilePath.class);
        final TaskListener listener = getContext().get(TaskListener.class);
        final EnvVars env = getContext().get(EnvVars.class);
        
        //Make sure the Workspace exists before run
        
        workspace.mkdirs();
        
        int res = execMatlabCommand(workspace, launcher, listener, env);

        getContext().setResult((res == 0) ? Result.SUCCESS : Result.FAILURE);
        
        return null;
    }

    @Override
    public void stop(Throwable cause) throws Exception {
        getContext().onFailure(cause);
    }

    private synchronized int execMatlabCommand(FilePath workspace, Launcher launcher,
            TaskListener listener, EnvVars envVars) throws IOException, InterruptedException {
        final String uniqueTmpFldrName = getUniqueNameForRunnerFile();  
        try {
            FilePath genScriptLocation =
                    getFilePathForUniqueFolder(launcher, uniqueTmpFldrName, workspace);
            final String cmdPrefix =
                    "addpath('" + genScriptLocation.getRemote().replaceAll("'", "''") + "'); ";
            final String matlabFunctionName = MatlabBuilderConstants.MATLAB_TEST_RUNNER_FILE_PREFIX
                    + genScriptLocation.getBaseName().replaceAll("-", "_");

            ProcStarter matlabLauncher = getProcessToRunMatlabCommand(workspace, launcher, listener,
                    envVars, cmdPrefix + matlabFunctionName+ "("+envVars.expand(getCommand()+")"), uniqueTmpFldrName);
            
            //prepare temp folder by coping genscript package.
            prepareTmpFldr(genScriptLocation,getRunnerScript(MatlabBuilderConstants.TEST_RUNNER_SCRIPT,getCommand()));
                               
            return matlabLauncher.pwd(workspace).join();
        } catch (Exception e) {
            listener.getLogger().println(e.getMessage());
            return 1;
        } finally {
            // Cleanup the runner File from tmp directory
            final FilePath matlabRunnerScript =
                    getFilePathForUniqueFolder(launcher, uniqueTmpFldrName, workspace);
            if (matlabRunnerScript.exists()) {
                matlabRunnerScript.deleteRecursive();
            }
        }

    }
  //Replace the MAP values in the script and return the new string 
    
    private String getRunnerScript(String script,Map<String,String> values) {
        for (Map.Entry<String, String> entry : values.entrySet()) {
            script = script.replace("${"+entry.getKey()+"}", entry.getValue());
        }
        return script;
    }
}
