
% This is the MATLAB script which generates the JSON file.
function  buildrunner(varargin)
p= buildfile;
if isempty(varargin)
    res = p.run();
else
    s = string(varargin);
    a = split(s);
    tasks = [];
    for idx = 1:length(a)
        tasks = [tasks,a(idx)];
    end
    res= p.run(tasks);
end
d = configureDictionary("string","string");
for pl = p.Tasks
    d = insert(d,pl.Name,pl.Description);
end
fID = makeFile();

taskDetails = struct();
for idx = 1:numel(res.TaskResults)
    taskDetails(idx).name = res.TaskResults(idx).Name;
    taskDetails(idx).description = d(res.TaskResults(idx).Name);
    taskDetails(idx).failed = res.TaskResults(idx).Failed;
    taskDetails(idx).skipped = res.TaskResults(idx).Skipped;
    taskDetails(idx).duration = string(res.TaskResults(idx).Duration);
end
a = struct("taskDetails",taskDetails);
s = jsonencode(a,"PrettyPrint",true);
fprintf(fID, '%s',s);
fclose(fID);
end


function fID = makeFile()
if exist('buildArtifact.json','file') == 2
    delete 'buildArtifact.json';
end
fID = fopen('buildArtifact.json', 'a');
end