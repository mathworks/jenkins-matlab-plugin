classdef JenkinsLoggingPlugin < matlab.buildtool.plugins.BuildRunnerPlugin
%

%   Copyright 2023 The MathWorks, Inc.

    methods
        function obj = JenkinsLoggingPlugin()
        end
    end

    methods (Access=protected)
        function runTask(plugin, pluginData)
            % Get task name and start log group
            taskName = pluginData.Name;
            marker = sprintf("[MATLAB Build - %s]",taskName)
            backspaces = repmat(char(8), 1, strlength(marker))
            fprintf("%s%s",marker,backspaces)
            runTask@matlab.buildtool.plugins.BuildRunnerPlugin(plugin, pluginData);

        end
    end
end
