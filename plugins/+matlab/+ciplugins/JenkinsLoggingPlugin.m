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
            fprintf('<a id="matlab-%s" name="matlab-%s"> </a>', pluginData.Name,pluginData.Name);
            runTask@matlab.buildtool.plugins.BuildRunnerPlugin(plugin, pluginData);

        end
    end
end