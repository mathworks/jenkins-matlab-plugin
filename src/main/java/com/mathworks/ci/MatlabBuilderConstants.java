package com.mathworks.ci;

/*
 * Copyright 2019-2024 The MathWorks, Inc.
 */

public class MatlabBuilderConstants {
    public static final double BASE_MATLAB_VERSION_RUNTESTS_SUPPORT = 8.1;
    public static final double BASE_MATLAB_VERSION_NO_APP_ICON_SUPPORT = 8.6;
    public static final double BASE_MATLAB_VERSION_BATCH_SUPPORT = 9.5;
    public static final double BASE_MATLAB_VERSION_COBERTURA_SUPPORT = 9.3;
    public static final double BASE_MATLAB_VERSION_MODELCOVERAGE_SUPPORT = 9.5;
    public static final double BASE_MATLAB_VERSION_EXPORTSTMRESULTS_SUPPORT = 9.6;

    public static final String MATLAB_RUNNER_TARGET_FILE = "Builder.matlab.runner.target.file.name";
    public static final String MATLAB_TESTS_RUNNER_TARGET_FILE = "runMatlabTests.m";
    public static final String MATLAB_RUNNER_RESOURCE = "com/mathworks/ci/MatlabBuilder/runMatlabTests.m";
    public static final String AUTOMATIC_OPTION = "RunTestsAutomaticallyOption";

    // Input parameter names (Passed to runMatlabTests.m as name-value pair
    // arguments)
    public static final String PDF_REPORT = "'PDFReport'";
    public static final String TAP_RESULTS = "'TAPResults'";
    public static final String JUNIT_RESULTS = "'JUnitResults'";
    public static final String STM_RESULTS = "'SimulinkTestResults'";
    public static final String COBERTURA_CODE_COVERAGE = "'CoberturaCodeCoverage'";
    public static final String COBERTURA_MODEL_COVERAGE = "'CoberturaModelCoverage'";

    // Matlab Script generator package
    public static final String MATLAB_SCRIPT_GENERATOR = "matlab-script-generator.zip";

    // Test runner file prefix
    public static final String MATLAB_TEST_RUNNER_FILE_PREFIX = "runner_";

    // Temporary MATLAB folder name in workspace
    public static final String TEMP_MATLAB_FOLDER_NAME = ".matlab";

    // MATLAB default function/plugin paths
    public static final String DEFAULT_PLUGIN = "+ciplugins/+jenkins/getDefaultPlugins.m";
    public static final String BUILD_REPORT_PLUGIN = "+ciplugins/+jenkins/BuildReportPlugin.m";
    public static final String TASK_RUN_PROGRESS_PLUGIN = "+ciplugins/+jenkins/TaskRunProgressPlugin.m";
    public static final String BUILD_ARTIFACT = "buildArtifact";

    public static final String NEW_LINE = System.getProperty("line.separator");

    // MATLAB Runner Script
    public static final String TEST_RUNNER_SCRIPT = String.join(NEW_LINE,
            "addpath('${TEMP_FOLDER}');",
            "testScript = genscript(${PARAMS});",
            "disp('Running MATLAB script with content:');",
            "disp(testScript.Contents);",
            "fprintf('___________________________________\\n\\n');",
            "run(testScript);");
}
