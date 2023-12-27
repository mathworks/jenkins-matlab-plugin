classdef BuildLogUpdater < matlab.buildtool.plugins.BuildRunnerPlugin
%

%   Copyright 2023 The MathWorks, Inc.

    methods
        function obj = BuildLogUpdater()
        end
    end

    methods (Access=protected)

        function runTask(plugin, pluginData)
            disp("[MATLAB-Build-" + pluginData.TaskResults.Name + "]");
            % Run task graph
            runTask@matlab.buildtool.plugins.BuildRunnerPlugin(plugin, pluginData);
        end
    end
 end