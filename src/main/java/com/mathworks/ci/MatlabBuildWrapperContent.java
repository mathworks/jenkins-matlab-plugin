package com.mathworks.ci;

/**
 * Copyright 2020 The MathWorks, Inc.
 *
 * Class to parse Stapler request for Use MATLAB Version build wrapper.
 *
 */

import org.kohsuke.stapler.DataBoundConstructor;

public class MatlabBuildWrapperContent {

    private final String matlabInstName;
    private final String matlabRootFolder;

    @DataBoundConstructor
    public MatlabBuildWrapperContent(String matlabInstName, String matlabRootFolder){
        this.matlabInstName = matlabInstName;
        this.matlabRootFolder = matlabRootFolder;
    }

    public String getMatlabInstName() {
        return matlabInstName;
    }

    public String getMatlabRootFolder() {
        return matlabRootFolder;
    }
}
