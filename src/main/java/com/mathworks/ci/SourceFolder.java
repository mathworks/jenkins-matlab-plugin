package com.mathworks.ci;

/**
 * Copyright 2020 The MathWorks, Inc.
 *
 * Describable class for Source Folder Option in RunMATLABTest Build step.
 *
 */

import hudson.Extension;
import hudson.Util;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import org.kohsuke.stapler.DataBoundConstructor;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class SourceFolder extends AbstractDescribableImpl<SourceFolder> {

    private List<SourceFolderPaths> sourceFolderPaths;
    private static final String SOURCE_FOLDER = "SourceFolder";

    @DataBoundConstructor
    public SourceFolder(List<SourceFolderPaths> sourceFolderPaths) {
        this.sourceFolderPaths = Util.fixNull(sourceFolderPaths);
    }

    public List<SourceFolderPaths> getSourceFolderPaths() {
        return this.sourceFolderPaths;
    }

    public  void addSourceToInputArgs(List<String> inputArgsList, String cellArraySourceVal) {
        // Concatenate all source folders to MATLAB cell array string.
        inputArgsList.add("'" + SOURCE_FOLDER + "'" + "," + cellArraySourceVal);
    }

    @Extension public static class DescriptorImpl extends Descriptor<SourceFolder> {}

}
