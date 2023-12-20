function plugins = getDefaultPlugins(pluginProviderData)
%

%   Copyright 2023 The MathWorks, Inc.
arguments
    pluginProviderData (1,1) struct = struct();
end

plugins = [ ...
    matlab.buildtool.internal.getFactoryDefaultPlugins(pluginProviderData) ...
    matlab.ciplugins.BuildJsonCreator() ...
    matlab.ciplugins.BuildLogUpdater() ...
];
end