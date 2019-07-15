%Copyright 2019 The MathWorks, Inc.

function failed = runMatlabTests(produceJUnit, produceTAP, produceCobertura)

BASE_VERSION_MATLABUNIT_SUPPORT = '8.1';
BASE_VERSION_TESTSUITE_SUPPORT = '9.0';

if verLessThan('matlab',BASE_VERSION_MATLABUNIT_SUPPORT)
    error('MATLAB:unitTest:testFrameWorkNotSupported','Running tests automatically is not supported in this relase.');
end

import('matlab.unittest.TestRunner');
import('matlab.unittest.TestSuite');

%Create test suite for tests folder
if verLessThan('matlab',BASE_VERSION_TESTSUITE_SUPPORT)
    suite = matlab.unittest.TestSuite.fromFolder(pwd,'IncludingSubfolders',true);
else
    suite = testsuite(pwd,'IncludeSubfolders',true);
end

% Create and configure the runner
runner = TestRunner.withTextOutput;

% Add the requested plugins
resultsDir = fullfile(pwd, 'matlabTestArtifacts');

% Produce JUnit report
if produceJUnit
    BASE_VERSION_JUNIT_SUPPORT = '8.6';
    if verLessThan('matlab',BASE_VERSION_JUNIT_SUPPORT)
        warning('MATLAB:testArtifact:junitReportNotSupported', 'Producing JUnit xml results is not supported in this release.');
    else
        import('matlab.unittest.plugins.XMLPlugin');
        mkdirIfNeeded(resultsDir)
        xmlFile = fullfile(resultsDir, 'junittestresults.xml');
        runner.addPlugin(XMLPlugin.producingJUnitFormat(xmlFile));
    end
end

% Produce TAP report
if produceTAP
    BASE_VERSION_TAPORIGINALFORMAT_SUPPORT = '8.3';
    BASE_VERSION_TAP13_SUPPORT = '9.1';
    if verLessThan('matlab',BASE_VERSION_TAPORIGINALFORMAT_SUPPORT)
        warning('MATLAB:testArtifact:tapReportNotSupported', 'Producing TAP results is not supported in this release.');
        tapPlugin = matlab.unittest.plugins.TestRunnerPlugin.empty;
    elseif verLessThan('matlab',BASE_VERSION_TAP13_SUPPORT)
        tapFile = getTapResultFile(resultsDir);
        import('matlab.unittest.plugins.TAPPlugin');
        tapPlugin = TAPPlugin.producingOriginalFormat(tapFile);
    else
        tapFile = getTapResultFile(resultsDir);
        import('matlab.unittest.plugins.TAPPlugin');
        tapPlugin = TAPPlugin.producingVersion13(tapFile);
    end
    runner.addPlugin(tapPlugin);
end

% Produce Cobertura report (Cobertura report generation is not supported
% below R17a) 
if produceCobertura 
    BASE_VERSION_COBERTURA_SUPPORT = '9.3';
    
    if verLessThan('matlab',BASE_VERSION_COBERTURA_SUPPORT)
         warning('MATLAB:testArtifact:coberturaReportNotSupported', 'Producing Cobertura results is not supported in this release.');
    else 
        import('matlab.unittest.plugins.CodeCoveragePlugin');
        import('matlab.unittest.plugins.codecoverage.CoberturaFormat');
        mkdirIfNeeded(resultsDir)
        coverageFile = fullfile(resultsDir, 'cobertura.xml');
        workSpace = fullfile(pwd);
        runner.addPlugin(CodeCoveragePlugin.forFolder(workSpace,'IncludingSubfolders',true,...
        'Producing', CoberturaFormat(coverageFile)));
    end
end

results = runner.run(suite);
failed = any([results.Failed]);

function tapToFile = getTapResultFile(resultsDir)
import('matlab.unittest.plugins.ToFile');
mkdirIfNeeded(resultsDir)
tapFile = fullfile(resultsDir, 'taptestresults.tap');
fclose(fopen(tapFile,'w'));
tapToFile = matlab.unittest.plugins.ToFile(tapFile);


function mkdirIfNeeded(dir)
if exist(dir,'dir') ~= 7
    mkdir(dir);
end


