package com.mathworks.ci;
/**
 * Copyright 2019-2020 The MathWorks, Inc.
 * 
 * Build Interface has two default methods. MATLAB builders can override the 
 * default behavior.
 * 
 */

import java.io.IOException;
import java.io.InputStream;
import hudson.EnvVars;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Launcher.ProcStarter;
import hudson.model.TaskListener;

public interface MatlabBuild {
    
    /**
     * This Method decorates the launcher with MATLAB command provided and returns the Process 
     * object to launch MATLAB with appropriate startup options like -r or -batch
     * @param workspace
     * @param launcher
     * @param listener
     * @param envVars
     * @param matlabCommand
     * @return
     * @throws IOException
     * @throws InterruptedException
     */
    default ProcStarter getProcessToRunMatlabCommand(FilePath workspace, Launcher launcher,TaskListener listener, EnvVars envVars, String matlabCommand) throws IOException, InterruptedException {
        ProcStarter matlabLauncher;
        matlabLauncher = launcher.launch().pwd(workspace).envs(envVars);
        FilePath targetWorkspace = new FilePath(launcher.getChannel(), workspace.getRemote());
        if(launcher.isUnix()) {
            matlabLauncher = launcher.launch().pwd(workspace).envs(envVars).cmds("./run_matlab_command.sh",matlabCommand).stdout(listener);
            //Copy runner .sh for linux platform in workspace.
            copyFileInWorkspace(MatlabBuilderConstants.SHELL_RUNNER_SCRIPT, "Builder.matlab.runner.script.target.file.linux.name", targetWorkspace);
        }else {
            launcher = launcher.decorateByPrefix("cmd.exe","/C");
            matlabLauncher = launcher.launch().pwd(workspace).envs(envVars).cmds("run_matlab_command.bat","\""+matlabCommand+"\"").stdout(listener);
            //Copy runner.bat for Windows platform in workspace.
            copyFileInWorkspace(MatlabBuilderConstants.BAT_RUNNER_SCRIPT, "Builder.matlab.runner.script.target.file.windows.name", targetWorkspace);
        }
        return matlabLauncher;
    }

    /**
     * Method to copy given file from source to target node specific workspace.
     * @param sourceFile
     * @param targetFile
     * @param targetWorkspace
     * @throws IOException
     * @throws InterruptedException
     */
    default void copyFileInWorkspace(String sourceFile, String targetFile,
            FilePath targetWorkspace) throws IOException, InterruptedException {
        final ClassLoader classLoader = getClass().getClassLoader();
        FilePath targetFilePath = new FilePath(targetWorkspace, Message.getValue(targetFile));
        InputStream in = classLoader.getResourceAsStream(sourceFile);
        targetFilePath.copyFrom(in);
        // set executable permission
        targetFilePath.chmod(0755);
        
    }

}
