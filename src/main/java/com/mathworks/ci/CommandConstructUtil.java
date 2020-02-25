package com.mathworks.ci;
/*
 * Copyright 2020-2021 The MathWorks, Inc.
 * 
 * MATLAB command construction utility class used to construct the startup command based on the
 * builders. Author : Nikhil Bhoski email : nbhoski@mathworks.com Date : 11/02/2020
 */

import java.io.IOException;
import java.io.InputStream;
import org.apache.commons.io.FilenameUtils;
import hudson.FilePath;
import hudson.Launcher;

public class CommandConstructUtil {

    private Launcher launcher;
    private String matlabRoot;

    public Launcher getLauncher() {
        return launcher;
    }

    public void setLauncher(Launcher launcher) {
        this.launcher = launcher;
    }

    public String getMatlabRoot() {
        return matlabRoot;
    }
    

    /*
     * Constructor to accepts the current launcher instance of the build with two parameters
     * launcher : Launcher associated with current build instance matlabRoot: Expanded string value
     * of MATLAB root
     */

    public CommandConstructUtil(Launcher launcher, String matlabRoot) {
        this.launcher = launcher;
        this.matlabRoot = matlabRoot;
    }
    
    /*
     * New set of methods for new changes with run matlab command script
     */

    public String constructCommandForTest(String inputArguments) {
        String runCommand;
        String matlabFunctionName = FilenameUtils.removeExtension(
                Message.getValue(MatlabBuilderConstants.MATLAB_RUNNER_TARGET_FILE));
        runCommand = "exit(" + matlabFunctionName + "(" + inputArguments + "))";
        if(isUnix()) {
            runCommand = getBashCompatibleCommandString(runCommand);
        }
        return runCommand;
    }
    
    public String constructCommandForRunCommand(String command) {
        String runCommand = command;
        if(isUnix()) {
            runCommand = getBashCompatibleCommandString(command);
        }
        return runCommand;
    }

    public String getNodeSpecificFileSeperator() {
        if (getLauncher().isUnix()) {
            return "/";
        } else {
            return "\\";
        }
    }

    public boolean isUnix() {
        return this.launcher.isUnix();
    }
    
    /*
     * This Method is to escape all open and closing brackets and single quotes 
     * to make it compatible with /bin/bash -c command on linux
     *
     */
    private String getBashCompatibleCommandString(String command) {
       return command.replaceAll("'","\\\\'").replaceAll("\\(", "\\\\(").replaceAll("\\)", "\\\\)");
    }
    
    public void copyMatlabScratchFileInWorkspace(String matlabRunnerResourcePath,
            String matlabRunnerTarget, FilePath targetWorkspace)
            throws IOException, InterruptedException {
        final ClassLoader classLoader = getClass().getClassLoader();
        FilePath targetFile =
                new FilePath(targetWorkspace, Message.getValue(matlabRunnerTarget));
        InputStream in = classLoader.getResourceAsStream(matlabRunnerResourcePath);
        targetFile.copyFrom(in);
        //set executable permission to the file on Unix.
        if(isUnix()) {
            targetFile.chmod(0777);            
        }
        
    }
}
