package com.mathworks.ci.utilities;

/**
 * Copyright 2024, The MathWorks, Inc.
 */

import jenkins.security.MasterToSlaveCallable;

public class GetSystemProperties extends MasterToSlaveCallable<String[], InterruptedException> {

    private static final long serialVersionUID = 1L;

    private final String[] properties;

    public GetSystemProperties(String... properties) {
        this.properties = properties;
    }

    public String[] call() {
        String[] values = new String[properties.length];
        for (int i = 0; i < properties.length; i++) {
            values[i] = System.getProperty(properties[i]);
        }
        return values;
    }
}
