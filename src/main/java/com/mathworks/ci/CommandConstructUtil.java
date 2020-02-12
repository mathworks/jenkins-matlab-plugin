package com.mathworks.ci;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.ArrayUtils;
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

    public void setMatlabRoot(String matlabRoot) {
        this.matlabRoot = matlabRoot;
    }
    
    /*
     * Constructor to accepts the current launcher instance of the build with two parameters 
     * launcher : Launcher associated with current build instance
     * matlabRoot: Expanded string value of MATLAB root  
     */
    
    public CommandConstructUtil(Launcher launcher,String matlabRoot) {
        this.launcher = launcher;
        this.matlabRoot = matlabRoot;
    }
    
    public List<String> constructBatchCommandForTestRun(String inputArguments) {
        final String runCommand;
        final List<String> matlabDefaultArgs;
            String matlabFunctionName =
                    FilenameUtils.removeExtension(Message.getValue(MatlabBuilderConstants.MATLAB_RUNNER_TARGET_FILE));
            runCommand = "exit(" + matlabFunctionName + "("
                    + inputArguments + "))";
            matlabDefaultArgs =
                    Arrays.asList(getMatlabRoot() + getNodeSpecificFileSeperator() + "bin" + getNodeSpecificFileSeperator() + "matlab",
                            "-batch", runCommand);
        return matlabDefaultArgs;
    }
    
    public List<String> constructBatchCommandForScriptRun(String customCommand){
        final List<String> matlabDefaultArgs;
        matlabDefaultArgs =
                Arrays.asList(getMatlabRoot() + getNodeSpecificFileSeperator() + "bin" + getNodeSpecificFileSeperator() + "matlab",
                        "-batch", customCommand);
        return matlabDefaultArgs;
    }

    public List<String> constructDefaultCommandForTestRun(String inputArguments) throws MatlabVersionNotFoundException {
        final List<String> matlabDefaultArgs = new ArrayList<String>();
        Collections.addAll(matlabDefaultArgs, getPreRunnerSwitches());
        if (!getLauncher().isUnix()) {
            matlabDefaultArgs.add("-noDisplayDesktop");
        }
        Collections.addAll(matlabDefaultArgs, getRunnerSwitch(inputArguments));
        if (!!getLauncher().isUnix()) {
            matlabDefaultArgs.add("-wait");
        }
        Collections.addAll(matlabDefaultArgs, getPostRunnerSwitches());
        return matlabDefaultArgs;
    }
    
    public List<String> constructDefaultCommandForScriptRun(String CustomScript) throws MatlabVersionNotFoundException {
        final List<String> matlabDefaultArgs = new ArrayList<String>();
        Collections.addAll(matlabDefaultArgs, getPreRunnerSwitches());
        if (!getLauncher().isUnix()) {
            matlabDefaultArgs.add("-noDisplayDesktop");
        }
        Collections.addAll(matlabDefaultArgs, getRunnerForScriptRun(CustomScript));
        if (!!getLauncher().isUnix()) {
            matlabDefaultArgs.add("-wait");
        }
        Collections.addAll(matlabDefaultArgs, getPostRunnerSwitches());
        return matlabDefaultArgs;
    }


    private String[] getPreRunnerSwitches() throws MatlabVersionNotFoundException {
        FilePath nodeSpecificMatlabRoot = new FilePath(getLauncher().getChannel(),getMatlabRoot());
        MatlabReleaseInfo matlabRel =new MatlabReleaseInfo(nodeSpecificMatlabRoot);
        String[] preRunnerSwitches =
                {getMatlabRoot() + getNodeSpecificFileSeperator() + "bin" + getNodeSpecificFileSeperator() + "matlab", "-nosplash",
                        "-nodesktop"};
        if(!matlabRel.verLessThan(MatlabBuilderConstants.BASE_MATLAB_VERSION_NO_APP_ICON_SUPPORT)) {
            preRunnerSwitches =  (String[]) ArrayUtils.add(preRunnerSwitches, "-noAppIcon");
        } 
        return preRunnerSwitches;
    }

    private String[] getPostRunnerSwitches() {
        String[] postRunnerSwitch = {"-log"};
        return postRunnerSwitch;
    }

    private String[] getRunnerSwitch(String inputArguments) {
        final String runCommand;
            String matlabFunctionName =
                    FilenameUtils.removeExtension(Message.getValue(MatlabBuilderConstants.MATLAB_RUNNER_TARGET_FILE));
            runCommand = "try,exit(" + matlabFunctionName + "("
                    + inputArguments
                    + ")),catch e,disp(getReport(e,'extended')),exit(1),end";

        final String[] runnerSwitch = {"-r", runCommand};
        return runnerSwitch;
    }
    
    private String[] getRunnerForScriptRun(String customCommand) {
        final String runCommand;
 
            runCommand = "try,eval('" + customCommand.replaceAll("'","''")
                    + "'),catch e,disp(getReport(e,'extended')),exit(1),end,exit";

        final String[] runnerSwitch = {"-r", runCommand};
        return runnerSwitch;
    }
    
    public String getNodeSpecificFileSeperator() {
        if (getLauncher().isUnix()) {
            return "/";
        } else {
            return "\\";
        }
    }
}
