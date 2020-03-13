package com.mathworks.ci;
/**
 * Copyright 2019-2020 The MathWorks, Inc.
 * 
 * Build Interface has two default methods. MATLAB builders can override the default behavior.
 * 
 */

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;
import hudson.EnvVars;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Launcher.ProcStarter;
import hudson.model.Computer;
import hudson.model.TaskListener;

public interface MatlabBuild {

    /**
     * This Method decorates the launcher with MATLAB command provided and returns the Process
     * object to launch MATLAB with appropriate startup options like -r or -batch
     * 
     * @param workspace Current build workspace
     * @param launcher Current build launcher
     * @param listener Current build listener
     * @param envVars Environment variables of the current build
     * @param matlabCommand MATLAB command to execute on shell
     * @return matlabLauncher returns the process launcher to run MATLAB commands
     */
    default ProcStarter getProcessToRunMatlabCommand(FilePath workspace, Launcher launcher,
            TaskListener listener, EnvVars envVars, String matlabCommand, String uniqueName)
            throws IOException, InterruptedException {
        // Get node specific tmp directory to copy matlab runner script
        String tmpDir = getNodeSpecificTmpFolderPath(workspace);
        FilePath targetWorkspace = new FilePath(launcher.getChannel(), tmpDir);
        ProcStarter matlabLauncher;
        if (launcher.isUnix()) {
            final String runnerScriptName = uniqueName + "/run_matlab_command.sh";
            matlabLauncher = launcher.launch().pwd(workspace).envs(envVars)
                    .cmds(tmpDir + "/" + runnerScriptName, matlabCommand).stdout(listener);

            // Copy runner .sh for linux platform in workspace.
            copyFileInWorkspace(MatlabBuilderConstants.SHELL_RUNNER_SCRIPT, runnerScriptName,
                    targetWorkspace);
        } else {
            final String runnerScriptName = uniqueName + "\\run_matlab_command.bat";
            launcher = launcher.decorateByPrefix("cmd.exe", "/C");
            matlabLauncher = launcher.launch().pwd(workspace).envs(envVars)
                    .cmds(tmpDir + "\\" + runnerScriptName, "\"" + matlabCommand + "\"")
                    .stdout(listener);
            // Copy runner.bat for Windows platform in workspace.
            copyFileInWorkspace(MatlabBuilderConstants.BAT_RUNNER_SCRIPT, runnerScriptName,
                    targetWorkspace);
        }
        return matlabLauncher;
    }

    /*
     * Method to copy given file from source to target node specific workspace.
     */
    default void copyFileInWorkspace(String sourceFile, String targetFile, FilePath targetWorkspace)
            throws IOException, InterruptedException {
        final ClassLoader classLoader = getClass().getClassLoader();
        FilePath targetFilePath = new FilePath(targetWorkspace, targetFile);
        InputStream in = classLoader.getResourceAsStream(sourceFile);
        targetFilePath.copyFrom(in);
        // set executable permission
        targetFilePath.chmod(0755);
    }

    default FilePath getFilePathForUniqueFolder(Launcher launcher, String uniqueName,FilePath workspace)
            throws IOException, InterruptedException {
        /*Use of Computer is not recommended as jenkins hygeine for pipeline support
         * https://javadoc.jenkins-ci.org/jenkins/tasks/SimpleBuildStep.html */
        
        Computer cmp = workspace.toComputer();
        String tmpDir = (String) cmp.getSystemProperties().get("java.io.tmpdir");
        if (launcher.isUnix()) {
            tmpDir = tmpDir + "/" + uniqueName;
        } else {
            tmpDir = tmpDir + "\\" + uniqueName;
        }
        return new FilePath(launcher.getChannel(), tmpDir);
    }

    default String getNodeSpecificTmpFolderPath(FilePath workspace) throws IOException, InterruptedException {
        Computer cmp = workspace.toComputer();
        String tmpDir = (String) cmp.getSystemProperties().get("java.io.tmpdir");
        return tmpDir;
    }

    default String getUniqueNameForRunnerFile() {
        return UUID.randomUUID().toString();
    }

}
