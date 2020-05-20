package com.mathworks.ci;

import java.io.IOException;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import hudson.EnvVars;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Launcher.ProcStarter;
import hudson.model.TaskListener;

public class TestStepExecution extends MatlabStepExecution{

    public TestStepExecution(StepContext context, String command, boolean copyScratchFile) {
        super(context, command, copyScratchFile);
        
    }
    
    @Override
    public ProcStarter getProcessToRunMatlabCommand(FilePath workspace, Launcher launcher,
            TaskListener listener, EnvVars envVars, String matlabCommand, String uniqueName)
            throws IOException, InterruptedException {
        // Get node specific tmp directory to copy matlab runner script
        String tmpDir = getNodeSpecificTmpFolderPath(workspace);
        FilePath targetWorkspace = new FilePath(launcher.getChannel(), tmpDir);
        ProcStarter matlabLauncher;
        if (launcher.isUnix()) {
            final String runnerScriptName = uniqueName + "/run_matlab_command_test.sh";
            matlabLauncher = launcher.launch().pwd(workspace).envs(envVars)
                    .cmds(tmpDir + "/" + runnerScriptName, matlabCommand).stdout(listener);

            // Copy runner .sh for linux platform in workspace.
            copyFileInWorkspace("run_matlab_command_test.sh", runnerScriptName,
                    targetWorkspace);
        } else {
            final String runnerScriptName = uniqueName + "\\run_matlab_command_test.bat";
            launcher = launcher.decorateByPrefix("cmd.exe", "/C");
            matlabLauncher = launcher.launch().pwd(workspace).envs(envVars)
                    .cmds(tmpDir + "\\" + runnerScriptName, "\"" + matlabCommand + "\"")
                    .stdout(listener);
            // Copy runner.bat for Windows platform in workspace.
            copyFileInWorkspace("run_matlab_command_test.bat", runnerScriptName,
                    targetWorkspace);
        }
        return matlabLauncher;
    }

    
}
