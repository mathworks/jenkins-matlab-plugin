classdef BuildReportPlugin < matlab.buildtool.plugins.BuildRunnerPlugin

%   Copyright 2024 The MathWorks, Inc.

    methods (Access=protected)

        function runTaskGraph(plugin, pluginData)
            runTaskGraph@matlab.buildtool.plugins.BuildRunnerPlugin(plugin, pluginData);
            fID = fopen(fullfile(getenv("WORKSPACE"),'.matlab/buildArtifact.json'), 'w');
            taskDetails = struct();
            for idx = 1:numel(pluginData.TaskResults)
                taskDetails(idx).name = pluginData.TaskResults(idx).Name;
                taskDetails(idx).description = pluginData.TaskGraph.Tasks(idx).Description;
                taskDetails(idx).failed = pluginData.TaskResults(idx).Failed;
                taskDetails(idx).skipped = pluginData.TaskResults(idx).Skipped;
                taskDetails(idx).duration = string(pluginData.TaskResults(idx).Duration);
            end
            a = struct("taskDetails",taskDetails);
            s = jsonencode(a,"PrettyPrint",true);
            fprintf(fID, '%s',s);
            fclose(fID);
        end

    end
end