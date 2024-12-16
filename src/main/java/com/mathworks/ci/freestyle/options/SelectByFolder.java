package com.mathworks.ci.freestyle.options;

/**
 * Copyright 2020-2024 The MathWorks, Inc.
 */

import java.util.List;
import java.util.stream.Collectors;

import org.kohsuke.stapler.DataBoundConstructor;

import hudson.Extension;
import hudson.Util;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;

public class SelectByFolder extends AbstractDescribableImpl<SelectByFolder> {
    private List<TestFolders> testFolderPaths;
    private static final String SELECT_BY_FOLDER = "SelectByFolder";

    @DataBoundConstructor
    public SelectByFolder(List<TestFolders> testFolderPaths) {
        this.testFolderPaths = Util.fixNull(testFolderPaths);
    }

    public List<TestFolders> getTestFolderPaths() {
        return this.testFolderPaths;
    }

    public List<String> getTestFolderStringPaths() {
        return this.testFolderPaths.stream().map(
                p -> p.getTestFolders()).collect(Collectors.toList());
    }

    public void addSourceToInputArgs(List<String> inputArgsList, String cellArraySourceVal) {
        // Concatenate all source folders to MATLAB cell array string.
        inputArgsList.add("'" + SELECT_BY_FOLDER + "'" + "," + cellArraySourceVal);
    }

    @Extension
    public static class DescriptorImpl extends Descriptor<SelectByFolder> {
    }
}
