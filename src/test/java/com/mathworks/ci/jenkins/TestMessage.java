package com.mathworks.ci.jenkins;

import java.util.ResourceBundle;

/*
 * Copyright 2018 The MathWorks, Inc.
 * 
 * This Class is wrapper to access the static configuration values used across test classes. Acts as
 * Utility class to access key & value pairs from testconfig.properties
 */

public class TestMessage {

    private static String VERIFY_MATLAB_INVOKES_POSITIVE = "Verify.matlab.invokes.positive";
    private static String VERIFY_BUILD_IGNORES_TEST_FAILURE = "Verify.build.ignore.test.failure";
    private static String CONFIG_FILE = "testconfig";

    private static ResourceBundle rb = ResourceBundle.getBundle(CONFIG_FILE);

    public String getMatlabInvokesPositive() {
        return rb.getString(VERIFY_MATLAB_INVOKES_POSITIVE);
    }

    public String getBuildIgnoresTestFailure() {
        return rb.getString(VERIFY_BUILD_IGNORES_TEST_FAILURE);
    }

    public String getValue(String key) {

        return rb.getString(key);
    }
}
