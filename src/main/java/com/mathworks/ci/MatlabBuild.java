package com.mathworks.ci;
/**
 * Copyright 2019-2023 The MathWorks, Inc.
 * 
 * Build Interface has two default methods. MATLAB builders can override the default behavior.
 * 
 */

import java.io.IOException;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import org.apache.commons.lang.RandomStringUtils;
import hudson.EnvVars;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Launcher.ProcStarter;
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
    default ProcStarter getProcessToRunMatlabCommand(FilePath workspace,
            Launcher launcher, TaskListener listener, EnvVars envVars, String matlabCommand, String startupOpts, String uniqueName)
            throws IOException, InterruptedException {
        // Get node specific temp .matlab directory to copy matlab runner script             
        FilePath targetWorkspace = new FilePath(launcher.getChannel(),
                workspace.getRemote() + "/" + MatlabBuilderConstants.TEMP_MATLAB_FOLDER_NAME);
        ProcStarter matlabLauncher;
        if (launcher.isUnix()) {
            // Determine whether we're on Mac on Linux
            ByteArrayOutputStream kernelStream = new ByteArrayOutputStream();
            launcher.launch()
                .cmds("uname")
                .masks(true)
                .stdout(kernelStream)
                .join();

            String binaryName;
            String runnerName;
            if (kernelStream.toString("UTF-8").contains("Linux")) {
                binaryName = "glnxa64/run-matlab-command";
                runnerName = uniqueName + "/run-matlab-command";
            } else {
                binaryName = "maci64/run-matlab-command";
                runnerName = uniqueName + "/run-matlab-command";
            }

            matlabLauncher = launcher.launch().envs(envVars);
            matlabLauncher.cmds(MatlabBuilderConstants.TEMP_MATLAB_FOLDER_NAME + "/" + runnerName, matlabCommand, startupOpts).stdout(listener);

            // Copy runner .sh for linux platform in workspace.
            copyFileInWorkspace(binaryName, runnerName, targetWorkspace);
        } else {
            final String runnerName = uniqueName + "\\run-matlab-command.exe";
            // launcher = launcher.decorateByPrefix("cmd.exe", "/C");
            matlabLauncher = launcher.launch().envs(envVars);
            matlabLauncher.cmds(MatlabBuilderConstants.TEMP_MATLAB_FOLDER_NAME + "\\" + runnerName, "\"" + matlabCommand + "\"", startupOpts)
                    .stdout(listener);
            // Copy runner.bat for Windows platform in workspace.
            copyFileInWorkspace("win64/run-matlab-command.exe", runnerName,
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

    default FilePath getFilePathForUniqueFolder(Launcher launcher, String uniqueName,
            FilePath workspace) throws IOException, InterruptedException {

        String tmpDir =
                workspace.getRemote() + "/" + MatlabBuilderConstants.TEMP_MATLAB_FOLDER_NAME;

        return new FilePath(launcher.getChannel(), tmpDir + "/" + uniqueName);
    }

    default String getUniqueNameForRunnerFile() {
        //Using 8 bit long random alphanumeric string
        return RandomStringUtils.randomAlphanumeric(8);
    }
    
    // This method prepares the temp folder by coping all helper files in it.
    default void prepareTmpFldr(FilePath tmpFldr, String runnerScript) throws IOException, InterruptedException {
        // copy genscript package
        copyFileInWorkspace(MatlabBuilderConstants.MATLAB_SCRIPT_GENERATOR,
                MatlabBuilderConstants.MATLAB_SCRIPT_GENERATOR, tmpFldr);
        FilePath zipFileLocation =
                new FilePath(tmpFldr, MatlabBuilderConstants.MATLAB_SCRIPT_GENERATOR);
        runnerScript=replaceZipPlaceholder(runnerScript, zipFileLocation.getRemote());

        // Write MATLAB scratch file in temp folder.
        FilePath scriptFile =
                new FilePath(tmpFldr, getValidMatlabFileName(tmpFldr.getBaseName()) + ".m");
        scriptFile.write(runnerScript, "UTF-8");
    }

    //This method replaces the placeholder with genscript's zip file location URL in temp folder
    default String replaceZipPlaceholder(String script, String url) {
        script = script.replace("${ZIP_FILE}", url.replaceAll("'","''"));
        return script;
    }
    
    default String getRunnerScript(String script, String params, String uniqueTmpFldrName) {
        script = script.replace("${PARAMS}", params);
        return script;
    }
    
    default String getValidMatlabFileName(String actualName) {
        return MatlabBuilderConstants.MATLAB_TEST_RUNNER_FILE_PREFIX
                + actualName.replaceAll("-", "_");
    }
}
