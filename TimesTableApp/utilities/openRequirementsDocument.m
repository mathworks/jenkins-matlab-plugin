function openRequirementsDocument()
% Utility to open the requirements document

%   Copyright 2018 MathWorks, Inc.

project = currentProject;
open( fullfile(project.RootFolder, 'requirements', 'TimesTableRequirements.mlx') )

end
