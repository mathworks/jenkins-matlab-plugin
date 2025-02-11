package com.mathworks.ci.utilities;

/**
 * Copyright 2024, The MathWorks Inc.
 */

import java.io.IOException;
import hudson.FilePath;
import com.mathworks.ci.parameters.MatlabActionParameters;

public class MatlabCommandRunnerTester extends MatlabCommandRunner {
    public MatlabCommandRunnerTester(MatlabActionParameters params) throws IOException, InterruptedException {
        super(params);
    }

    @Override
    public FilePath prepareRunnerExecutable() throws IOException, InterruptedException {
        return super.prepareRunnerExecutable();
    }

    @Override
    public FilePath createFileWithContent(String content) throws IOException, InterruptedException {
        return super.createFileWithContent(content);
    }
}
