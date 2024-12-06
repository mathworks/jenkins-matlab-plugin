package com.mathworks.ci.actions;

/**
 * Copyright 2024, The MathWorks Inc.
 */

import com.mathworks.ci.BuildArtifactAction;
import com.mathworks.ci.BuildConsoleAnnotator;
import com.mathworks.ci.MatlabBuilderConstants;
import com.mathworks.ci.utilities.MatlabCommandRunner;
import hudson.FilePath;
import hudson.model.Run;
import org.apache.commons.lang.RandomStringUtils;

import java.io.File;
import java.io.IOException;

public class MatlabAction {
    MatlabCommandRunner runner;
    BuildConsoleAnnotator annotator;
    String actionID;

    public String getActionID() {
        return (this.actionID == null) ? "" : this.actionID;
    }

    public MatlabAction(MatlabCommandRunner runner) {
        this.runner = runner;
    }

    public MatlabAction(MatlabCommandRunner runner, BuildConsoleAnnotator annotator) {
        this.runner = runner;
        this.actionID = RandomStringUtils.randomAlphanumeric(8);
        this.annotator = annotator;
    }

    public void copyBuildPluginsToTemp() throws IOException, InterruptedException {
        // Copy plugins and override default plugins function
        runner.copyFileToTempFolder(MatlabBuilderConstants.DEFAULT_PLUGIN, MatlabBuilderConstants.DEFAULT_PLUGIN);
        runner.copyFileToTempFolder(MatlabBuilderConstants.BUILD_REPORT_PLUGIN,
                MatlabBuilderConstants.BUILD_REPORT_PLUGIN);
        runner.copyFileToTempFolder(MatlabBuilderConstants.TASK_RUN_PROGRESS_PLUGIN,
                MatlabBuilderConstants.TASK_RUN_PROGRESS_PLUGIN);
    }

    public void setBuildEnvVars() throws IOException, InterruptedException {
        // Set environment variable
        runner.addEnvironmentVariable(
                "MW_MATLAB_BUILDTOOL_DEFAULT_PLUGINS_FCN_OVERRIDE",
                "ciplugins.jenkins.getDefaultPlugins");
        runner.addEnvironmentVariable("MW_BUILD_PLUGIN_ACTION_ID", this.getActionID());
        runner.addEnvironmentVariable(
                "MW_MATLAB_TEMP_FOLDER",
                runner.getTempFolder().toString());
    }

    public void teardownAction(Run<?, ?> build) {
        // Handle build result
        if (this.annotator != null) {
            moveJsonArtifactToBuildRoot(build, MatlabBuilderConstants.BUILD_ARTIFACT);
        }

        try {
            this.runner.removeTempFolder();
        } catch (Exception e) {
            System.err.println(e.toString());
        }
    }

    private void moveJsonArtifactToBuildRoot(Run<?, ?> build, String artifactBaseName) {
        try {
            FilePath file = new FilePath(this.runner.getTempFolder(), artifactBaseName + ".json");
            if (file.exists()) {
                FilePath rootLocation = new FilePath(
                        new File(
                                build.getRootDir().getAbsolutePath(),
                                artifactBaseName + this.getActionID() + ".json"));
                file.copyTo(rootLocation);
                file.delete();
                build.addAction(new BuildArtifactAction(build, this.getActionID()));
            }
        } catch (Exception e) {
            // Don't want to override more important error
            // thrown in catch block
            System.err.println(e.toString());
        }
    }
}
