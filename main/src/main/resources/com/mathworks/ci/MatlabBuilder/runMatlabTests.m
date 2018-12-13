%Copyright 2018 The MathWorks, Inc.

function failed = runMatlabTests(produceJUnit, produceTAP, produceCobertura)
import('matlab.unittest.TestRunner');
import('matlab.unittest.plugins.XMLPlugin');
import('matlab.unittest.plugins.TAPPlugin');
import('matlab.unittest.plugins.ToFile');
import('matlab.unittest.plugins.ToFile');

workSpace = fullfile(pwd);

%Create test suite for tests folder
suite = testsuite(pwd,'IncludeSubfolders',true);

% Create and configure the runner
runner = TestRunner.withTextOutput('Verbosity',3);

% Add the requested plugins
resultsDir = fullfile(pwd, 'results');

if produceJUnit
    mkdirIfNeeded(resultsDir)
    xmlFile = fullfile(resultsDir, 'JUnittestresults.xml');
    runner.addPlugin(XMLPlugin.producingJUnitFormat(xmlFile));
end

if produceTAP
    mkdirIfNeeded(resultsDir)
    tapFile = fullfile(resultsDir, 'Taptestresults.tap');
    fclose(fopen(tapFile,'w'));
    runner.addPlugin(TAPPlugin.producingVersion13(ToFile(tapFile)));
end

BASE_VERSION_COBERTURA_SUPPORT = '9.3';
if produceCobertura && ~verLessThan('matlab',BASE_VERSION_COBERTURA_SUPPORT)
    import('matlab.unittest.plugins.CodeCoveragePlugin');
    import('matlab.unittest.plugins.codecoverage.CoberturaFormat');
    mkdirIfNeeded(resultsDir)
    coverageFile = fullfile(resultsDir, 'cobertura.xml');
    runner.addPlugin(CodeCoveragePlugin.forFolder(fullfile(workSpace),'IncludingSubfolders',true,...
        'Producing', CoberturaFormat(coverageFile)));
end

results = runner.run(suite);
failed = any([results.Failed]);


function mkdirIfNeeded(dir)
if exist(dir,'dir') ~= 7
    mkdir(dir);
end


