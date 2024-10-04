package com.mathworks.ci.tools;

import java.io.IOException;

// Extend IOException so we can throw and stop the build if installation fails

public class InstallationFailedException extends IOException {
    InstallationFailedException(String message) {
        super(message);
    }
}