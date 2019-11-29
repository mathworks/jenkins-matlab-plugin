package com.mathworks.ci;
/*
 * Copyright 2019 The MathWorks, Inc.
 */

public class MatlabBuilderConstants {
    static final double BASE_MATLAB_VERSION_RUNTESTS_SUPPORT = 8.1;
    static final double BASE_MATLAB_VERSION_BATCH_SUPPORT = 9.5;
    static final double BASE_MATLAB_VERSION_COBERTURA_SUPPORT = 9.3;
    static final double BASE_MATLAB_VERSION_MODELCOVERAGE_SUPPORT = 9.4;
    static final double BASE_MATLAB_VERSION_EXPORTSTMRESULTS_SUPPORT = 9.6;
    
    static final String MATLAB_RUNNER_TARGET_FILE = "Builder.matlab.runner.target.file.name";
    static final String MATLAB_RUNNER_RESOURCE = "com/mathworks/ci/MatlabBuilder/runMatlabTests.m";
    static final String AUTOMATIC_OPTION = "RunTestsAutomaticallyOption";
    
    // Input parameter names (Passed to runMatlabTests.m as name-value pair arguments)
    static final String PDF_REPORT = "'PDFReport'";
    static final String TAP_RESULTS = "'TapResults'";
    static final String JUNIT_RESULTS = "'JunitResults'";
    static final String STM_RESULTS = "'SimulinkTestResults'";
    static final String COBERTURA_CODE_COVERAGE = "'CoberturaCodeCoverage'";
    static final String COBERTURA_MODEL_COVERAGE = "'CoberturaModelCoverage'";
}
