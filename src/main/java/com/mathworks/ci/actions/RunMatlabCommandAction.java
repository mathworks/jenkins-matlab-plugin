package com.mathworks.ci.actions;

/**
 * Copyright 2024, The MathWorks Inc.
 *
 */

import java.io.IOException;

import org.apache.commons.lang.RandomStringUtils;

import com.mathworks.ci.MatlabExecutionException;
import com.mathworks.ci.TestFile;
import com.mathworks.ci.TestResultsViewAction;
import com.mathworks.ci.utilities.MatlabCommandRunner;
import com.mathworks.ci.BuildConsoleAnnotator;
import com.mathworks.ci.parameters.CommandActionParameters;
import com.mathworks.ci.MatlabBuilderConstants;

import java.io.File;
import hudson.FilePath;
import hudson.model.Run;

public class RunMatlabCommandAction extends MatlabAction {
    private CommandActionParameters params;

    public RunMatlabCommandAction(MatlabCommandRunner runner, BuildConsoleAnnotator annotator, CommandActionParameters params) {
        super(runner, annotator);
        this.params = params;
    }

    public RunMatlabCommandAction(CommandActionParameters params) throws IOException, InterruptedException {
        this(new MatlabCommandRunner(params), 
                new BuildConsoleAnnotator(
                    params.getTaskListener().getLogger(), 
                    params.getBuild().getCharset()),
                params);
    }

    public void run() throws IOException, InterruptedException, MatlabExecutionException {
        super.copyBuildPluginsToTemp();
        super.setBuildEnvVars();

        // Redirect output to the build annotator
        runner.redirectStdOut(annotator);

        // Prepare MATLAB command
        String command = "addpath('" 
            + runner.getTempFolder().getRemote()
            + "'); " + this.params.getCommand();

        try {
            runner.runMatlabCommand(command);
        } catch (Exception e) {
            this.params.getTaskListener().getLogger()
                .println(e.getMessage());
            throw(e);
        } finally{
            annotator.forceEol();

            Run<?, ?> build = this.params.getBuild();
            super.teardownAction(build);

            FilePath jsonFile = new FilePath(params.getWorkspace(), ".matlab" + File.separator + MatlabBuilderConstants.TEST_RESULTS_VIEW_ARTIFACT + ".json");

            if (jsonFile.exists()) {
                FilePath rootLocation = new FilePath(
                        new File(
                            build.getRootDir()
                            .getAbsolutePath()
                            + File.separator
                            + MatlabBuilderConstants.TEST_RESULTS_VIEW_ARTIFACT + this.id + ".json"));
                jsonFile.copyTo(rootLocation);
                jsonFile.delete();
                TestResultsViewAction testResultsViewAction = new TestResultsViewAction(build, this.id, this.params.getWorkspace(), this.params.getTaskListener());
                build.addAction(testResultsViewAction);

                // try{
                    
                //     for(TestFile testFile : testResultsViewAction.getTestResults()){
                //         this.params.getTaskListener().getLogger().println(testFile.getName());
                //     }
                // }
                // catch(Exception e){
                //     String whatYouWant = testResultsViewAction.getWhatYouWant();
                //     this.params.getTaskListener().getLogger().println(whatYouWant);
                //     String[] whatYouWantSplit = whatYouWant.split("/");
                //     this.params.getTaskListener().getLogger().println(whatYouWantSplit[0]);
                //     this.params.getTaskListener().getLogger().println(whatYouWantSplit[1]);
                //     this.params.getTaskListener().getLogger().println(e.getMessage());
                //     // throw(e);
                // }
            }
        }
    }
}
