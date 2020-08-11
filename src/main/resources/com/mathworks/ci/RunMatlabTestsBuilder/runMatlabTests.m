%Copyright 2020 The MathWorks, Inc.

function runMatlabTests(varargin)

p = inputParser;
validationFcn = @(c)ischar(c) && (isempty(c) || isrow(c));

p.addParameter('PDFReportPath', '', validationFcn);
p.addParameter('TAPResultsPath', '', validationFcn);
p.addParameter('JUnitResultsPath', '', validationFcn);
p.addParameter('SimulinkTestResultsPath', '', validationFcn);
p.addParameter('CoberturaCodeCoveragePath', '', validationFcn);
p.addParameter('CoberturaModelCoveragePath', '', validationFcn);

p.parse(varargin{:});

pdfReportPath            = p.Results.PDFReportPath;
tapReportPath            = p.Results.TAPResultsPath;
junitReportPath          = p.Results.JUnitResultsPath;
stmReportPath            = p.Results.SimulinkTestResultsPath;
coberturaReportPath      = p.Results.CoberturaCodeCoveragePath;
modelCoveragePath        = p.Results.CoberturaModelCoveragePath;

srcFldr = getenv('WORKSPACE');

testScript = genscript('Test',...
   'PDFTestReport',pdfReportPath,...
   'TAPTestResults',tapReportPath,...
   'JUnitTestResults',junitReportPath,...
   'SimulinkTestResults',stmReportPath,...
   'CoberturaCodeCoverage',coberturaReportPath,...
   'CoberturaModelCoverage',modelCoveragePath,...
   'SourceFolder',srcFldr);

disp('Running MATLAB script with content:\n');
disp(strtrim(testScript.writeToText()));
fprintf('___________________________________\n\n');
run(testScript);
