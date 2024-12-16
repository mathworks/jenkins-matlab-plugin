package com.mathworks.ci;

/**
 * Copyright 2020-2024 The MathWorks, Inc.
 *
 * Class to parse Stapler request for Use MATLAB Version build wrapper.
 */

import org.kohsuke.stapler.DataBoundConstructor;

public class MatlabBuildWrapperContent {

    private final String matlabInstallationName;
    private final String matlabRootFolder;

    @DataBoundConstructor
    public MatlabBuildWrapperContent(String matlabInstallationName, String matlabRootFolder) {
        this.matlabInstallationName = matlabInstallationName;
        this.matlabRootFolder = matlabRootFolder;
    }

    public String getMatlabInstallationName() {
        return matlabInstallationName;
    }

    public String getMatlabRootFolder() {
        return matlabRootFolder;
    }
}
