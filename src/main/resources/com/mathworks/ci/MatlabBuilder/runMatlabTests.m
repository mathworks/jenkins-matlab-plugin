%Copyright 2019 The MathWorks, Inc.

function failed = runMatlabTests(varargin)

p = inputParser;
p.addParameter('TapResults', false, @islogical);
p.addParameter('JunitResults', false, @islogical);
p.addParameter('CoberturaCodeCoverage', false, @islogical);
p.addParameter('CoberturaModelCoverage', false, @islogical);
p.addParameter('ExportTestResults', false, @islogical);
p.addParameter('IntegratedTestResults', false, @islogical);

p.parse(varargin{:});

produceTAP               = p.Results.TapResults;
produceJUnit             = p.Results.JunitResults;
produceCodeCoverage      = p.Results.CoberturaCodeCoverage;
produceModelCoverage     = p.Results.CoberturaModelCoverage;
exportTestResults        = p.Results.ExportTestResults;
produceIntegratedResults = p.Results.IntegratedTestResults;

BASE_VERSION_MATLABUNIT_SUPPORT = '8.1';

if verLessThan('matlab',BASE_VERSION_MATLABUNIT_SUPPORT)
    error('MATLAB:unitTest:testFrameWorkNotSupported','Running tests automatically is not supported in this relase.');
end

%Create test suite for tests folder
suite = getTestSuite();

% Create and configure the runner
import('matlab.unittest.TestRunner');
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

% Produce Cobertura code coverage report (Cobertura report generation is not supported
% below R17a) 
if produceCodeCoverage 
    if isCoberturaNotSupported
        warning('MATLAB:testArtifact:coberturaReportNotSupported', 'Producing Cobertura results is not supported in this release.');
    else
        import('matlab.unittest.plugins.CodeCoveragePlugin');
        import('matlab.unittest.plugins.codecoverage.CoberturaFormat');
        mkdirIfNeeded(resultsDir)
        coverageFile = fullfile(resultsDir, 'codecoverage.xml');
        workSpace = fullfile(pwd);
        runner.addPlugin(CodeCoveragePlugin.forFolder(workSpace,'IncludingSubfolders',true,...
            'Producing', CoberturaFormat(coverageFile)));
    end
end

% Produce Cobertura model coverage report (Cobertura report generation is not supported
% below R17a) 
if produceModelCoverage
    if isCoberturaNotSupported
        warning('MATLAB:testArtifact:coberturaReportNotSupported', 'Producing Cobertura results is not supported in this release.');
    else
        import sltest.plugins.ModelCoveragePlugin;
        import matlab.unittest.plugins.codecoverage.CoberturaFormat;
        
        mkdirIfNeeded(resultsDir);
        coverageFile = fullfile(resultsDir, 'modelcoverage.xml');
        runner.addPlugin(ModelCoveragePlugin('Producing',CoberturaFormat(coverageFile)));
    end
end

% Produce unified MATLAB/Simulink Test report and save
% Simulink Test results in MLDATX format (Not supported below R2018b)
if produceIntegratedResults || exportTestResults
    if isTestManagerResultsPluginNotPresent
        warning('MATLAB:testArtifact:artifactNotSupported', ...
            'Generating unified test report and saving simulink test results is not supported in this release.');
    else 
        import sltest.plugins.TestManagerResultsPlugin;
        import matlab.unittest.plugins.TestReportPlugin;
        
        mkdirIfNeeded(resultsDir);
        htmlFolder = fullfile(resultsDir, 'htmlTestResults');
        htmlFile = 'report.html';
        mldatxFile = fullfile(resultsDir, 'simulinktestresults.mldatx');
            
        if produceIntegratedResults && exportTestResults
            runner.addPlugin(TestReportPlugin.producingHTML(htmlFolder, 'MainFile', htmlFile));
            runner.addPlugin(TestManagerResultsPlugin('ExportToFile', mldatxFile));
        elseif produceIntegratedResults
            runner.addPlugin(TestReportPlugin.producingHTML(htmlFolder, 'MainFile', htmlFile));
            runner.addPlugin(TestManagerResultsPlugin);
        else
            runner.addPlugin(TestManagerResultsPlugin('ExportToFile', mldatxFile));
        end
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

function suite = getTestSuite()
import('matlab.unittest.TestSuite');
BASE_VERSION_TESTSUITE_SUPPORT = '9.0';
if verLessThan('matlab',BASE_VERSION_TESTSUITE_SUPPORT)
    suite = matlab.unittest.TestSuite.fromFolder(pwd,'IncludingSubfolders',true);
else
    suite = testsuite(pwd,'IncludeSubfolders',true);
end

function mkdirIfNeeded(dir)
if exist(dir,'dir') ~= 7
    mkdir(dir);
end

function tf = isCoberturaNotSupported()
BASE_VERSION_COBERTURA_SUPPORT = '9.3';

tf = verLessThan('matlab',BASE_VERSION_COBERTURA_SUPPORT);

function tf = isTestManagerResultsPluginNotPresent()
BASE_VERSION_TESTMANAGERRESULTSPLUGIN_SUPPORT = '9.5'; 

tf = verLessThan('matlab',BASE_VERSION_TESTMANAGERRESULTSPLUGIN_SUPPORT);
