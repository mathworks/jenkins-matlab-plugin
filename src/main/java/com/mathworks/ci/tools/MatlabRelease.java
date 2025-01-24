package com.mathworks.ci.tools;

/**
 * Copyright 2025, The MathWorks, Inc.
 */

public class MatlabRelease {
    public String name;
    public boolean isPrerelease;

    public MatlabRelease(String name, boolean isPrerelease) {
        this.name = name;
        this.isPrerelease = isPrerelease;
    }
}
