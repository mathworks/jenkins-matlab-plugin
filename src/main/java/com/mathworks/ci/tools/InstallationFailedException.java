package com.mathworks.ci.tools;

/**
 * Copyright 2024, The MathWorks, Inc.
 */

import java.io.IOException;

// Extend IOException so we can throw and stop the build if installation fails

public class InstallationFailedException extends IOException {

    InstallationFailedException(String message) {
        super(message);
    }
}
