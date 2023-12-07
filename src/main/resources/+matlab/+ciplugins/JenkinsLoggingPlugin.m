classdef JenkinsLoggingPlugin < matlab.buildtool.plugins.BuildRunnerPlugin
%

%   Copyright 2023 The MathWorks, Inc.

    methods
        function obj = JenkinsLoggingPlugin()
        end
    end

    methods (Access=protected)

        function runTaskGraph(plugin, pluginData)
            % Run task graph
            runTaskGraph@matlab.buildtool.plugins.BuildRunnerPlugin(plugin, pluginData);
            if exist('.matlab/buildArtifact.json','file') == 2
                delete '.matlab/buildArtifact.json';
            end
            fID = fopen('.matlab/buildArtifact.json', 'a');
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