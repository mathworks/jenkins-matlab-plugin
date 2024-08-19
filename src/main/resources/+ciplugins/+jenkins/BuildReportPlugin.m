classdef BuildReportPlugin < matlab.buildtool.plugins.BuildRunnerPlugin

%   Copyright 2024 The MathWorks, Inc.

    properties
        taskDetails = {};
    end

    methods (Access=protected)
        function runTaskGraph(plugin, pluginData)
            runTaskGraph@matlab.buildtool.plugins.BuildRunnerPlugin(plugin, pluginData);

            [fID, msg] = fopen(fullfile(getenv("MW_MATLAB_TEMP_FOLDER"),"buildArtifact.json"), "w");
            if fID == -1
                warning("ciplugins:jenkins:BuildReportPlugin:UnableToOpenFile","Could not open a file for Jenkins build result table due to: %s", msg);
            else
                closeFile = onCleanup(@()fclose(fID));
                a = struct();
                a.taskDetails = plugin.taskDetails;
                s = jsonencode(a, PrettyPrint=true);
                fprintf(fID, "%s", s);
            end
        end

        function runTask(plugin, pluginData)
            runTask@matlab.buildtool.plugins.BuildRunnerPlugin(plugin, pluginData);

            taskDetail = plugin.getCommonTaskData(pluginData);
            plugin.taskDetails = [plugin.taskDetails, taskDetail];
        end

        function skipTask(plugin, pluginData)
            skipTask@matlab.buildtool.plugins.BuildRunnerPlugin(plugin, pluginData);

            taskDetail = plugin.getCommonTaskData(pluginData);
            taskDetail.skipReason = pluginData.SkipReason;
            plugin.taskDetails = [plugin.taskDetails, taskDetail];
        end
    end

    methods(Static, Access=private)
        function taskDetail = getCommonTaskData(pluginData)
            taskDetail = struct();
            taskDetail.name = pluginData.TaskResults.Name;
            taskDetail.description = pluginData.TaskGraph.Tasks.Description;
            taskDetail.failed = pluginData.TaskResults.Failed;
            taskDetail.skipped = pluginData.TaskResults.Skipped;
            taskDetail.duration = string(pluginData.TaskResults.Duration);
        end
    end
end