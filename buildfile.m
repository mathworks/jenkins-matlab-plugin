function plan = buildfile
% Copyright 2023 The MathWorks, Inc.

import matlab.buildtool.tasks.*
import matlab.buildtool.Task;

plan = buildplan(localfunctions);

plan("check") = Task;
plan("check").Actions = @checkTask;
plan("test") = Task;
plan("test").Actions = @testTask;
plan("test").Dependencies = ["check","show"];
plan("show") = Task;
plan("show").Actions = @showTask;

%plan("dsplay")

plan("clean") = CleanTask();
plan.DefaultTasks = ["test"];

function checkTask(~)
 % Identify code issues
disp("Hello Check");
end
function showTask(~)
 % Identify code issues
disp("Hello Show");
end

function testTask(~)
 % Identify code issues
disp("Hello test");
end

end