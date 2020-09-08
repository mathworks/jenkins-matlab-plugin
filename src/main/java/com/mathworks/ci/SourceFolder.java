package com.mathworks.ci;

/**
 * Copyright 2019-2020 The MathWorks, Inc.
 *
 * Describable class for Source Folder Option in RunMATLABTest Build step.
 *
 */

import hudson.Extension;
import hudson.Util;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class SourceFolder extends AbstractDescribableImpl<SourceFolder> {

    private List<SourceFolderPaths> sourceFolderPaths = new ArrayList<>();
    private static final String SOURCE_FOLDER = "SourceFolder";

    @DataBoundConstructor
    public SourceFolder() {

    }

    @DataBoundSetter
    public void setSourceFolderPaths(List<SourceFolderPaths> sourceFolderPaths) {
        this.sourceFolderPaths = Util.fixNull(sourceFolderPaths);
    }

    public List<SourceFolderPaths> getSourceFolderPaths() {
        return this.sourceFolderPaths;
    }

    public  void addFilePathArgTo(Map<String, String> inputArgs) {
        // Concatenate all source folders to a single ";" separated string
        inputArgs.put(SOURCE_FOLDER, this.sourceFolderPaths.stream()
                .map(SourceFolderPaths::getSrcFolderPath)
                .collect(Collectors.joining(";")));
    }

    @Extension public static class DescriptorImpl extends Descriptor<SourceFolder> {
        @Override
        public String getDisplayName() {
            return "";
        }
    }

}
