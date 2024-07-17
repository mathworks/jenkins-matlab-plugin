package com.mathworks.ci.actions;

/**
 * Copyright 2024, The MathWorks Inc.
 *
 */

import java.io.File;
import java.io.IOException;

import hudson.FilePath;
import hudson.model.Run;

import com.mathworks.ci.BuildArtifactAction;
import com.mathworks.ci.BuildConsoleAnnotator;
import com.mathworks.ci.MatlabExecutionException;
import com.mathworks.ci.parameters.BuildActionParameters;
import com.mathworks.ci.utilities.MatlabCommandRunner;
import org.apache.commons.lang.RandomStringUtils;

public class RunMatlabBuildAction {
    private BuildActionParameters params; 
    private MatlabCommandRunner runner;
    private BuildConsoleAnnotator annotator;

    private static String DEFAULT_PLUGIN = 
        "+ciplugins/+jenkins/getDefaultPlugins.m";
    private static String BUILD_REPORT_PLUGIN = 
        "+ciplugins/+jenkins/BuildReportPlugin.m";
    private static String TASK_RUN_PROGRESS_PLUGIN = 
        "+ciplugins/+jenkins/TaskRunProgressPlugin.m";
    private String actionID;

    public String getActionID(){
        return this.actionID;
    }

    public RunMatlabBuildAction(MatlabCommandRunner runner, BuildConsoleAnnotator annotator, BuildActionParameters params) {
        this.runner = runner;
        this.actionID = RandomStringUtils.randomAlphanumeric(8);
        this.annotator = annotator;
        this.params = params;
    }

    public RunMatlabBuildAction(BuildActionParameters params) throws IOException, InterruptedException {
        this(new MatlabCommandRunner(params), 
                new BuildConsoleAnnotator(
                    params.getTaskListener().getLogger(), 
                    params.getBuild().getCharset()),
                params);
    }

    public void run() throws IOException, InterruptedException, MatlabExecutionException {
        // Copy plugins and override default plugins function
        runner.copyFileToTempFolder(DEFAULT_PLUGIN, DEFAULT_PLUGIN);
        runner.copyFileToTempFolder(BUILD_REPORT_PLUGIN, BUILD_REPORT_PLUGIN);
        runner.copyFileToTempFolder(TASK_RUN_PROGRESS_PLUGIN, TASK_RUN_PROGRESS_PLUGIN);

        
        // Set environment variable
        runner.addEnvironmentVariable(
                "MW_MATLAB_BUILDTOOL_DEFAULT_PLUGINS_FCN_OVERRIDE",
                "ciplugins.jenkins.getDefaultPlugins");
        runner.addEnvironmentVariable("MW_BUILD_PLUGIN_ACTION_ID",this.getActionID());
        runner.addEnvironmentVariable(
                "MW_MATLAB_TEMP_FOLDER",
                runner.getTempFolder().toString());

        // Redirect output to the build annotator
        runner.redirectStdOut(annotator);

        // Prepare the build tool command
        // TODO: Devise better solution then prepending the command
        //   here.
        String command = "addpath('" 
            + runner.getTempFolder().getRemote()
            + "'); buildtool";

        if (params.getTasks() != null) {
            command += " " + params.getTasks();
        }

        if (params.getBuildOptions() != null) {
            command += " " + params.getBuildOptions();
        }

        try {
            runner.runMatlabCommand(command);
        } catch (Exception e) {
            this.params.getTaskListener().getLogger()
                .println(e.getMessage());
            throw(e);
        } finally {
            annotator.forceEol();

            try {
                // Handle build result
                Run<?,?> build = this.params.getBuild();
                FilePath jsonFile = new FilePath(runner.getTempFolder(), "buildArtifact.json");
                if (jsonFile.exists()) {
                    FilePath rootLocation = new FilePath(
                            new File(
                                build.getRootDir().getAbsolutePath(),
                                "buildArtifact" + this.getActionID() + ".json")
                            );
                    jsonFile.copyTo(rootLocation);
                    jsonFile.delete();
                    build.addAction(new BuildArtifactAction(build, this.getActionID()));
                }
            } catch (Exception e) {
                // Don't want to override more important error
                // thrown in catch block
                System.err.println(e.toString());
            } finally {
                try {
                    this.runner.removeTempFolder();
                } catch (Exception e) {
                    System.err.println(e.toString());
                }
            }
        }

    }
}