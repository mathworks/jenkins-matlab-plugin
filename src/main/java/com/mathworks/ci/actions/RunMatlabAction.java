package com.mathworks.ci.actions;

/**
 * Copyright 2024, The MathWorks Inc.
 *
 */

import com.mathworks.ci.BuildConsoleAnnotator;
import com.mathworks.ci.MatlabBuilderConstants;
import com.mathworks.ci.utilities.MatlabCommandRunner;
import org.apache.commons.lang.RandomStringUtils;

import java.io.IOException;

public class RunMatlabAction {
    private MatlabCommandRunner runner;
    private BuildConsoleAnnotator annotator;
    private String actionID;

    public String getActionID(){
        return this.actionID;
    }

    public RunMatlabAction(MatlabCommandRunner runner, BuildConsoleAnnotator annotator) {
        this.runner = runner;
        this.actionID = RandomStringUtils.randomAlphanumeric(8);
        this.annotator = annotator;
    }

    public void copyPluginsToTemp() throws IOException, InterruptedException {
        runner.copyFileToTempFolder(MatlabBuilderConstants.DEFAULT_PLUGIN, MatlabBuilderConstants.DEFAULT_PLUGIN);
        runner.copyFileToTempFolder(MatlabBuilderConstants.BUILD_REPORT_PLUGIN, MatlabBuilderConstants.BUILD_REPORT_PLUGIN);
        runner.copyFileToTempFolder(MatlabBuilderConstants.TASK_RUN_PROGRESS_PLUGIN, MatlabBuilderConstants.TASK_RUN_PROGRESS_PLUGIN);
    }
}
