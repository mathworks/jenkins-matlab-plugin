package com.mathworks.ci.parameters;

/**
 * Copyright 2024 The MathWorks, Inc.
 */

import java.util.List;
import java.util.ArrayList;
import java.io.IOException;
import hudson.FilePath;
import hudson.EnvVars;
import hudson.Launcher;
import hudson.model.Run;
import hudson.model.TaskListener;
import org.jenkinsci.plugins.workflow.steps.StepContext;

public class TestActionParameters extends MatlabActionParameters {
    private String testResultsPDF;
    private String testResultsTAP;
    private String testResultsJUnit;
    private String codeCoverageCobertura;
    private String testResultsSimulinkTest;
    private String modelCoverageCobertura;
    private String selectByTag;
    private String loggingLevel;
    private String outputDetail;
    private boolean useParallel;
    private boolean strict;
    private List<String> sourceFolder = new ArrayList<>();
    private List<String> selectByFolder = new ArrayList<>();

    public TestActionParameters(StepContext context, String startupOpts,
            String testResultsPDF, String testResultsTAP, String testResultsJUnit,
            String codeCoverageCobertura, String testResultsSimulinkTest, String modelCoverageCobertura,
            String selectByTag, String loggingLevel, String outputDetail,
            boolean useParallel, boolean strict, List<String> sourceFolder,
            List<String> selectByFolder)
            throws IOException, InterruptedException {
        super(context, startupOpts);
        this.testResultsPDF = testResultsPDF;
        this.testResultsTAP = testResultsTAP;
        this.testResultsJUnit = testResultsJUnit;
        this.codeCoverageCobertura = codeCoverageCobertura;
        this.testResultsSimulinkTest = testResultsSimulinkTest;
        this.modelCoverageCobertura = modelCoverageCobertura;
        this.selectByTag = selectByTag;
        this.loggingLevel = loggingLevel;
        this.outputDetail = outputDetail;
        this.useParallel = useParallel;
        this.strict = strict;
        this.sourceFolder = sourceFolder;
        this.selectByFolder = selectByFolder;
    }

    public TestActionParameters(Run<?, ?> build, FilePath workspace, EnvVars env, Launcher launcher,
            TaskListener listener, String startupOpts,
            String testResultsPDF, String testResultsTAP, String testResultsJUnit,
            String codeCoverageCobertura, String testResultsSimulinkTest, String modelCoverageCobertura,
            String selectByTag, String loggingLevel, String outputDetail,
            boolean useParallel, boolean strict, List<String> sourceFolder,
            List<String> selectByFolder) {
        super(build, workspace, env, launcher, listener, startupOpts);
        this.testResultsPDF = testResultsPDF;
        this.testResultsTAP = testResultsTAP;
        this.testResultsJUnit = testResultsJUnit;
        this.codeCoverageCobertura = codeCoverageCobertura;
        this.testResultsSimulinkTest = testResultsSimulinkTest;
        this.modelCoverageCobertura = modelCoverageCobertura;
        this.selectByTag = selectByTag;
        this.loggingLevel = loggingLevel;
        this.outputDetail = outputDetail;
        this.useParallel = useParallel;
        this.strict = strict;
        this.sourceFolder = sourceFolder;
        this.selectByFolder = selectByFolder;
    }

    public String getTestResultsPDF() {
        return testResultsPDF;
    }

    public String getTestResultsTAP() {
        return testResultsTAP;
    }

    public String getTestResultsJUnit() {
        return testResultsJUnit;
    }

    public String getCodeCoverageCobertura() {
        return codeCoverageCobertura;
    }

    public String getTestResultsSimulinkTest() {
        return testResultsSimulinkTest;
    }

    public String getModelCoverageCobertura() {
        return modelCoverageCobertura;
    }

    public String getSelectByTag() {
        return selectByTag;
    }

    public String getLoggingLevel() {
        if (loggingLevel == null) {
            return null;
        }

        return loggingLevel.equalsIgnoreCase("default") ? null : loggingLevel;
    }

    public String getOutputDetail() {
        if (outputDetail == null) {
            return null;
        }

        return outputDetail.equalsIgnoreCase("default") ? null : outputDetail;
    }

    public String getUseParallel() {
        return String.valueOf(useParallel);
    }

    public String getStrict() {
        return String.valueOf(strict);
    }

    public List<String> getSourceFolder() {
        return sourceFolder;
    }

    public List<String> getSelectByFolder() {
        return selectByFolder;
    }
}
