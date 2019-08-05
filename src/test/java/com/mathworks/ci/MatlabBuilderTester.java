package com.mathworks.ci;

/*
 * Copyright 2018 The MathWorks, Inc.
 * 
 * Tester class for MatlabBuilder
 * 
 * Author : Nikhil Bhoski email : nikhil.bhoski@mathworks.in Date : 08/07/2018 (Initial draft)
 */

import java.util.ArrayList;
import java.util.List;
import hudson.EnvVars;
import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractProject;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;

public class MatlabBuilderTester extends MatlabBuilder {
    private String commandParameter;
    private String matlabExecutorPath;

    public MatlabBuilderTester(String localMatlab, String matlabExecutorPath,
            String customTestPointArgument) {
        super();
        setMatlabRoot(localMatlab);
        this.commandParameter = customTestPointArgument;
        this.matlabExecutorPath = matlabExecutorPath;
    }

    @Override
    public List<String> constructMatlabCommandWithBatch() {
        return testMatlabCommand();
    }

    @Override
    public List<String> constructDefaultMatlabCommand(boolean isLinuxLauncher) {
        return testMatlabCommand();
    }

    private List<String> testMatlabCommand() {
        List<String> matlabDefaultArgs = new ArrayList<String>();
        matlabDefaultArgs.add(this.matlabExecutorPath);
        matlabDefaultArgs.add(this.commandParameter);
        return matlabDefaultArgs;
    }


    @Extension
    public static class Descriptor extends BuildStepDescriptor<Builder> {

        @Override
        public boolean isApplicable(
                @SuppressWarnings("rawtypes") Class<? extends AbstractProject> jobType) {

            return true;
        }

        @Override
        public String getDisplayName() {

            return null;
        }
    }
}
