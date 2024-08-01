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
import com.mathworks.ci.parameters.RunActionParameters;
import com.mathworks.ci.utilities.MatlabCommandRunner;

import java.io.File;
import hudson.FilePath;
import hudson.model.Run;

import com.mathworks.ci.MatlabBuilderConstants;

public class RunMatlabCommandAction {
    private RunActionParameters params; 
    private MatlabCommandRunner runner;
    private String id;

    public RunMatlabCommandAction(MatlabCommandRunner runner, RunActionParameters params) {
        this.runner = runner;
        this.params = params;
        id = RandomStringUtils.randomAlphanumeric(8);
    }

    public RunMatlabCommandAction(RunActionParameters params) throws IOException, InterruptedException {
        this(new MatlabCommandRunner(params), params);
    }

    public void run() throws IOException, InterruptedException, MatlabExecutionException {
        try {
            runner.runMatlabCommand(this.params.getCommand());
        } catch (Exception e) {
            this.params.getTaskListener().getLogger()
                .println(e.getMessage());
            throw(e);
        } finally{
        
            Run<?,?> build = this.params.getBuild();
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

    public String getId() {
        return id;
    }
}
