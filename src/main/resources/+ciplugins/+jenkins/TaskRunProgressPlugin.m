classdef TaskRunProgressPlugin < matlab.buildtool.plugins.BuildRunnerPlugin
%

%   Copyright 2023 The MathWorks, Inc.

    methods (Access=protected)

        function runTask(plugin, pluginData)
            disp("[MATLAB-Build-" + pluginData.TaskResults.Name + "-" + getenv('MW_BUILD_PLUGIN_ACTION_ID') +"]");
            runTask@matlab.buildtool.plugins.BuildRunnerPlugin(plugin, pluginData);
        end

        function skipTask(plugin, pluginData)
            disp("[MATLAB-Build-" + pluginData.TaskResults.Name + "-" + getenv('MW_BUILD_PLUGIN_ACTION_ID') +"]");
            skipTask@matlab.buildtool.plugins.BuildRunnerPlugin(plugin, pluginData);
        end
    end
 end