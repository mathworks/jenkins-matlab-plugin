%Copyright 2019-2020 The MathWorks, Inc.

function failed = runMatlabTests(varargin)

p = inputParser;
p.addParameter('PDFReport', false, @islogical);
p.addParameter('TAPResults', false, @islogical);
p.addParameter('JUnitResults', false, @islogical);
p.addParameter('SimulinkTestResults', false, @islogical);
p.addParameter('CoberturaCodeCoverage', false, @islogical);
p.addParameter('CoberturaModelCoverage', false, @islogical);

p.parse(varargin{:});

producePDFReport         = p.Results.PDFReport;
produceTAP               = p.Results.TAPResults;
produceJUnit             = p.Results.JUnitResults;
exportSTMResults         = p.Results.SimulinkTestResults;
produceCobertura         = p.Results.CoberturaCodeCoverage;
produceModelCoverage     = p.Results.CoberturaModelCoverage;

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
    elseif verLessThan('matlab',BASE_VERSION_TAP13_SUPPORT)
        tapFile = getTapResultFile(resultsDir);
        import('matlab.unittest.plugins.TAPPlugin');
        tapPlugin = TAPPlugin.producingOriginalFormat(tapFile);
        runner.addPlugin(tapPlugin);
    else
        tapFile = getTapResultFile(resultsDir);
        import('matlab.unittest.plugins.TAPPlugin');
        tapPlugin = TAPPlugin.producingVersion13(tapFile);
        runner.addPlugin(tapPlugin);
    end
    
end

% Produce Cobertura report (Cobertura report generation is not supported
% below R17a) 
if produceCobertura 
    BASE_VERSION_COBERTURA_SUPPORT = '9.3';
    
    if verLessThan('matlab',BASE_VERSION_COBERTURA_SUPPORT)
         warning('MATLAB:testArtifact:coberturaReportNotSupported', 'Producing Cobertura code coverage results is not supported in this release.');
    else 
        import('matlab.unittest.plugins.CodeCoveragePlugin');
        mkdirIfNeeded(resultsDir)
        coverageFile = fullfile(resultsDir, 'cobertura.xml');
        workSpace = fullfile(pwd);
        runner.addPlugin(CodeCoveragePlugin.forFolder(workSpace,'IncludingSubfolders',true,...
        'Producing', CoberturaFormat(coverageFile)));
    end
end

% Produce Cobertura model coverage report (Not supported below R2018b) 
if produceModelCoverage
    if ~exist('sltest.plugins.ModelCoveragePlugin', 'class') || ~coberturaModelCoverageSupported
        warning('MATLAB:testArtifact:cannotGenerateModelCoverageReport', ...
                'Unable to generate Cobertura model coverage report. To generate the report, use a Simulink Coverage license with MATLAB R2018b or a newer release.');
    else 
        import('sltest.plugins.ModelCoveragePlugin');
        
        mkdirIfNeeded(resultsDir);
        coverageFile = fullfile(resultsDir, 'coberturamodelcoverage.xml');
        runner.addPlugin(ModelCoveragePlugin('Producing',CoberturaFormat(coverageFile)));
    end
end

stmResultsPluginAddedToRunner = false;

% Save Simulink Test Manager results in MLDATX format (Not supported below R2019a)
if exportSTMResults
    if ~stmResultsPluginPresent || ~exportSTMResultsSupported
        issueExportSTMResultsUnsupportedWarning;
    else
        mkdirIfNeeded(resultsDir);
        runner.addPlugin(TestManagerResultsPlugin('ExportToFile', getMLDATXFilePath(resultsDir)));
        stmResultsPluginAddedToRunner = true;
    end
end

% Produce PDF test report (Not supported on MacOS platforms and below R2017a)
if producePDFReport
    if ismac
        warning('MATLAB:testArtifact:unSupportedPlatform', ...
            'Producing a PDF test report is not currently supported on MacOS platforms.');
    elseif ~testReportPluginPresent
        issuePDFReportUnsupportedWarning;
    else
        mkdirIfNeeded(resultsDir);
        import('matlab.unittest.plugins.TestReportPlugin');
        runner.addPlugin(TestReportPlugin.producingPDF(getPDFFilePath(resultsDir)));
        
        if ~stmResultsPluginAddedToRunner && stmResultsPluginPresent
            runner.addPlugin(TestManagerResultsPlugin);
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

function plugin = CoberturaFormat(varargin)
plugin = matlab.unittest.plugins.codecoverage.CoberturaFormat(varargin{:});

function plugin = TestManagerResultsPlugin(varargin)
plugin = sltest.plugins.TestManagerResultsPlugin(varargin{:});

function filePath = getPDFFilePath(resultsDir)
filePath = fullfile(resultsDir, 'testreport.pdf');

function filePath = getMLDATXFilePath(resultsDir)
filePath = fullfile(resultsDir, 'simulinktestresults.mldatx');

function tf = testReportPluginPresent
BASE_VERSION_REPORTPLUGIN_SUPPORT = '9.2'; % R2017a 

tf = ~verLessThan('matlab',BASE_VERSION_REPORTPLUGIN_SUPPORT);

function tf = stmResultsPluginPresent
tf = logical(exist('sltest.plugins.TestManagerResultsPlugin', 'class'));

function tf = coberturaModelCoverageSupported
BASE_VERSION_MODELCOVERAGE_SUPPORT = '9.5'; % R2018b

tf = ~verLessThan('matlab',BASE_VERSION_MODELCOVERAGE_SUPPORT);

function tf = exportSTMResultsSupported
BASE_VERSION_EXPORTSTMRESULTS_SUPPORT = '9.6'; % R2019a

tf = ~verLessThan('matlab',BASE_VERSION_EXPORTSTMRESULTS_SUPPORT);

function issuePDFReportUnsupportedWarning
warning('MATLAB:testArtifact:pdfReportNotSupported', ...
    'Producing a test report in PDF format is not supported in the current MATLAB release.');

function issueExportSTMResultsUnsupportedWarning
warning('MATLAB:testArtifact:cannotExportSimulinkTestManagerResults', ...
    ['Unable to export Simulink Test Manager results. This feature ', ...
    'requires a Simulink Test license and is supported only in MATLAB R2019a or a newer release.']);
