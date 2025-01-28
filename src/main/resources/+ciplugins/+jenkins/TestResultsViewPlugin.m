classdef TestResultsViewPlugin < matlab.unittest.plugins.TestRunnerPlugin
    % Copyright 2025 The MathWorks, Inc.
    
    methods (Access=protected)
        function reportFinalizedSuite(plugin, pluginData)
            % Checkout MATLAB Test license
            license('checkout', 'matlab_test');

            testDetails = {};
            for idx = 1:numel(pluginData.TestResult)
                testDetails(idx).TestResult = pluginData.TestResult(idx);
                testDetails(idx).BaseFolder = pluginData.TestSuite(idx).BaseFolder;
            end

            % If test results artifact exists, update the same file
            testArtifactFile = fullfile(getenv("MW_MATLAB_TEMP_FOLDER"),"matlabTestResults.json");
            if isfile(testArtifactFile)
                testResults = {jsondecode(fileread(testArtifactFile))};
            else
                testResults = {};
            end
            testResults{end+1} = testDetails;
            JsonTestResults = jsonencode(testResults, "PrettyPrint", true);
            
            fID = fopen(testArtifactFile, "w");
            fprintf(fID, '%s', JsonTestResults);
            fclose(fID);

            % Invoke the superclass method
            reportFinalizedSuite@matlab.unittest.plugins.TestRunnerPlugin(plugin, pluginData);
        end
    end
end