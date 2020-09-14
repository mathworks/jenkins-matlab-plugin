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

public class SourceFolder extends AbstractDescribableImpl<SourceFolder> implements MatlabBuild {

    private List<SourceFolderPaths> sourceFolderPaths;

    @DataBoundConstructor
    public SourceFolder(List<SourceFolderPaths> sourceFolderPaths) {
        this.sourceFolderPaths = Util.fixNull(sourceFolderPaths);
    }

    public List<SourceFolderPaths> getSourceFolderPaths() {
        return this.sourceFolderPaths;
    }

    public  void addFilePathArgTo(String sourceKeyVal, Map<String, String> inputArgs) {
        // Concatenate all source folders to MATLAB cell array string.
        inputArgs.put(sourceKeyVal, getCellArrayFrmList(this.sourceFolderPaths.stream()
                .map(SourceFolderPaths::getSrcFolderPath)
                .collect(Collectors.toList())));
    }

    @Extension public static class DescriptorImpl extends Descriptor<SourceFolder> {
        @Override
        public String getDisplayName() {
            return "";
        }
    }

}
