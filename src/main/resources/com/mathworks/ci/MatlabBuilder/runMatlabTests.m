%Copyright 2019 The MathWorks, Inc.

function failed = runMatlabTests(varargin)

p = inputParser;
p.addParameter('PDFReport', false, @islogical);
p.addParameter('TapResults', false, @islogical);
p.addParameter('JunitResults', false, @islogical);
p.addParameter('SimulinkTestResults', false, @islogical);
p.addParameter('CoberturaCodeCoverage', false, @islogical);
p.addParameter('CoberturaModelCoverage', false, @islogical);

p.parse(varargin{:});

producePDFReport         = p.Results.PDFReport;
produceTAP               = p.Results.TapResults;
produceJUnit             = p.Results.JunitResults;
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

% Produce Cobertura model coverage report (Not supported below R2018a) 
if produceModelCoverage
    if ~exist('sltest.plugins.ModelCoveragePlugin', 'class')
        warning('MATLAB:testArtifact:cannotGenerateModelCoverageReport', ...
                'Unable to generate Cobertura model coverage report. To generate the report, use a Simulink Coverage license with MATLAB R2018a or a newer release.');
    else 
        import sltest.plugins.ModelCoveragePlugin;
        import matlab.unittest.plugins.codecoverage.CoberturaFormat;
        
        mkdirIfNeeded(resultsDir);
        coverageFile = fullfile(resultsDir, 'coberturamodelcoverage.xml');
        runner.addPlugin(ModelCoveragePlugin('Producing',CoberturaFormat(coverageFile)));
    end
end

% Produce PDF test report (Not supported below R2016b)
if producePDFReport && ~exportSTMResults
    if ~testReportPluginPresent
        issuePDFReportUnsupportedWarning();
    else
        import matlab.unittest.plugins.TestReportPlugin;
        mkdirIfNeeded(resultsDir);
        
        if stmResultsPluginPresent
            runner.addPlugin(TestManagerResultsPlugin);
        end
        runner.addPlugin(TestReportPlugin.producingPDF(getPDFFilePath(resultsDir)));
    end
end

% Save Simulink Test Manager results in MLDATX format (Not supported below R2019a)
if exportSTMResults && ~producePDFReport
    if ~stmResultsPluginPresent || ~exportSTMResultsSupported
        issueExportSTMResultsUnsupportedWarning();
    else
        mkdirIfNeeded(resultsDir);
        runner.addPlugin(TestManagerResultsPlugin('ExportToFile', getMLDATXFilePath(resultsDir)));
    end
end

% Note: We can remove the following piece of code once c4668443 gets ported to 19a branch.
%
% This check is to tackle the situation wherein user wants both pdf report and simulink test results.  
% Basically, TestManagerResultsPlugin throws an error if we use two different instances of the
% same plugin (g1898027). The geck is fixed but the changes are yet to be ported to 19a.
if producePDFReport && exportSTMResults
    try
        import matlab.unittest.plugins.TestReportPlugin;
        mkdirIfNeeded(resultsDir);

        testReportPlugin = TestReportPlugin.producingPDF(getPDFFilePath(resultsDir));
        stmResultsPlugin = TestManagerResultsPlugin('ExportToFile', getMLDATXFilePath(resultsDir));
    catch exception
        testReportPlugin = matlab.unittest.plugins.TestRunnerPlugin.empty;
        stmResultsPlugin = matlab.unittest.plugins.TestRunnerPlugin.empty;
        
        if testReportPluginPresent && stmResultsPluginPresent && ...
                exportSTMResultsSupported
            throw(exception);
        end
        
        if ~testReportPluginPresent
            issuePDFReportUnsupportedWarning();
        else
            if stmResultsPluginPresent
                stmResultsPlugin = TestManagerResultsPlugin;
            end
            testReportPlugin = TestReportPlugin.producingPDF(getPDFFilePath(resultsDir));
        end
            
        if ~stmResultsPluginPresent || ~exportSTMResultsSupported
            issueExportSTMResultsUnsupportedWarning();
        end
    end
    runner.addPlugin(testReportPlugin);
    runner.addPlugin(stmResultsPlugin);
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

function plugin = TestManagerResultsPlugin(varargin)
plugin = sltest.plugins.TestManagerResultsPlugin(varargin{:});

function filePath = getPDFFilePath(resultsDir)
filePath = fullfile(resultsDir, 'testreport.pdf');

function filePath = getMLDATXFilePath(resultsDir)
filePath = fullfile(resultsDir, 'simulinktestresults.mldatx');

function tf = testReportPluginPresent()
BASE_VERSION_REPORTPLUGIN_SUPPORT = '9.1'; % R2016b

tf = ~verLessThan('matlab',BASE_VERSION_REPORTPLUGIN_SUPPORT);

function tf = stmResultsPluginPresent()
tf = logical(exist('sltest.plugins.TestManagerResultsPlugin', 'class'));

function tf = exportSTMResultsSupported()
BASE_VERSION_EXPORTSTMRESULTS_SUPPORT = '9.6'; % R2019a

tf = ~verLessThan('matlab',BASE_VERSION_EXPORTSTMRESULTS_SUPPORT);

function issuePDFReportUnsupportedWarning()
warning('MATLAB:testArtifact:pdfReportNotSupported', ...
    'Producing a test report in PDF format is not supported in the current MATLAB release.');

function issueExportSTMResultsUnsupportedWarning()
warning('MATLAB:testArtifact:cannotSaveSimulinkTestManagerResults', ...
    ['Unable to export Simulink Test Manager results. This feature ', ...
    'requires a Simulink Test license and is supported only in MATLAB R2019a or a newer release.']);
