function plugins = getDefaultPlugins(pluginProviderData)
%

%   Copyright 2024 The MathWorks, Inc.
arguments
    pluginProviderData (1,1) struct = struct();
end

plugins = [ ...
    matlab.buildtool.internal.getFactoryDefaultPlugins(pluginProviderData) ...
    ciplugins.jenkins.BuildReportPlugin() ...
    ciplugins.jenkins.TaskRunProgressPlugin() ...
];
end