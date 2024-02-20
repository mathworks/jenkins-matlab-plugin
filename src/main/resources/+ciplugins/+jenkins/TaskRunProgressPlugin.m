classdef TaskRunProgressPlugin < matlab.buildtool.plugins.BuildRunnerPlugin
%

%   Copyright 2023 The MathWorks, Inc.

    methods (Access=protected)

        function runTask(plugin, pluginData)
            disp("[MATLAB-Build-" + pluginData.TaskResults.Name + "]");
            runTask@matlab.buildtool.plugins.BuildRunnerPlugin(plugin, pluginData);
        end
    end
 end