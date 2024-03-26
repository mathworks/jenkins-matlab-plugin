package com.mathworks.ci;

import java.lang.Exception;

public class MatlabExecutionException extends Exception {

    private final int exitCode;

    public MatlabExecutionException(int exitCode) {
        super(String.format(Message.getValue("matlab.execution.exception.prefix"), exitCode));
        this.exitCode = exitCode;
    }

    /*
     * Function to retrieve MATLAB process's exit code.
     * This may require Jenkins In-process script approval.
     */
    public int getExitCode() {
        return exitCode;
    }
}
