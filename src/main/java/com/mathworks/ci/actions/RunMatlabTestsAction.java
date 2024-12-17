package com.mathworks.ci.actions;

/**
 * Copyright 2024, The MathWorks Inc.
 */

import java.io.IOException;
import java.util.List;
import java.util.ArrayList;

import hudson.FilePath;
import hudson.model.Run;

import com.mathworks.ci.Utilities;
import com.mathworks.ci.MatlabBuilderConstants;
import com.mathworks.ci.MatlabExecutionException;
import com.mathworks.ci.parameters.TestActionParameters;
import com.mathworks.ci.utilities.MatlabCommandRunner;

public class RunMatlabTestsAction extends MatlabAction {
    private TestActionParameters params;

    public RunMatlabTestsAction(MatlabCommandRunner runner, TestActionParameters params) {
        super(runner);
        this.params = params;
    }

    public RunMatlabTestsAction(TestActionParameters params) throws IOException, InterruptedException {
        this(new MatlabCommandRunner(params), params);
    }

    public void run() throws IOException, InterruptedException, MatlabExecutionException {
        // Copy in genscript
        FilePath genScriptZip = runner.copyFileToTempFolder(
                MatlabBuilderConstants.MATLAB_SCRIPT_GENERATOR,
                "genscript.zip");
        genScriptZip.unzip(runner.getTempFolder());

        // Prepare the command
        String command = MatlabBuilderConstants.TEST_RUNNER_SCRIPT;
        command = command.replace("${TEMP_FOLDER}", runner.getTempFolder().getRemote());
        command = command.replace("${PARAMS}", getParameterString());

        // Run the command
        try {
            runner.runMatlabCommand(command);
        } catch (Exception e) {
            this.params.getTaskListener()
                    .getLogger()
                    .println(e.getMessage());
            throw (e);
        } finally {
            Run<?, ?> build = this.params.getBuild();
            super.teardownAction(build);
        }
    }

    private String singleQuotify(String in) {
        return "'" + in.replace("'", "''") + "'";
    }

    // Concatenate the input arguments, try to keep this function as
    // readable as possible because it can get hairy.
    private String getParameterString() {
        // The final list to be concatted and returned
        final List<String> inputArgsList = new ArrayList<String>();

        inputArgsList.add("'Test'");

        // Prepare source and test folder lists
        String sourceFolders = null;
        if (this.params.getSourceFolder() != null) {
            sourceFolders = this.params.getSourceFolder().size() == 0
                    ? null
                    : Utilities.getCellArrayFromList(this.params.getSourceFolder());
        }

        String selectFolders = null;
        if (this.params.getSelectByFolder() != null) {
            selectFolders = this.params.getSelectByFolder().size() == 0
                    ? null
                    : Utilities.getCellArrayFromList(this.params.getSelectByFolder());
        }

        // All string-based fields
        final String[] names = {
                "'PDFTestReport'",
                "'TAPTestResults'",
                "'JUnitTestResults'",
                "'CoberturaCodeCoverage'",
                MatlabBuilderConstants.STM_RESULTS,
                "'CoberturaModelCoverage'",
                "'SelectByTag'",
                "'UseParallel'",
                "'Strict'",
                "'LoggingLevel'",
                "'OutputDetail'",
                "'SourceFolder'",
                "'SelectByFolder'"
        };
        final String[] values = {
                this.params.getTestResultsPDF(),
                this.params.getTestResultsTAP(),
                this.params.getTestResultsJUnit(),
                this.params.getCodeCoverageCobertura(),
                this.params.getTestResultsSimulinkTest(),
                this.params.getModelCoverageCobertura(),
                this.params.getSelectByTag(),
                this.params.getUseParallel(),
                this.params.getStrict(),
                this.params.getLoggingLevel(),
                this.params.getOutputDetail(),
                sourceFolders,
                selectFolders
        };

        for (int i = 0; i < names.length; i++) {
            if (values[i] != null && !values[i].equals("false")) {
                inputArgsList.add(names[i]);
                String arg = values[i].equals("true") || values[i].startsWith("{")
                        ? values[i]
                        : singleQuotify(values[i]);
                inputArgsList.add(arg);
            }
        }

        return String.join(",", inputArgsList);
    }
}
