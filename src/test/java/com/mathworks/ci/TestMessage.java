package com.mathworks.ci;

/*
 * Copyright 2018-2024 The MathWorks, Inc.
 * 
 * This Class is wrapper to access the static configuration values used across test classes. Acts as
 * Utility class to access key & value pairs from testconfig.properties
 */

import java.util.ResourceBundle;

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

    public static String getValue(String key) {

        return rb.getString(key);
    }
}
