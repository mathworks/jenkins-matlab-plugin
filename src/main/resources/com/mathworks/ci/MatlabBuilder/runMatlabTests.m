%Copyright 2019 The MathWorks, Inc.

function failed = runMatlabTests(varargin)

p = inputParser;
p.addParameter('TapResults', false, @islogical);
p.addParameter('JunitResults', false, @islogical);
p.addParameter('MATLABTestReport', false, @islogical);
p.addParameter('SimulinkTestResults', false, @islogical);
p.addParameter('CoberturaCodeCoverage', false, @islogical);
p.addParameter('CoberturaModelCoverage', false, @islogical);

p.parse(varargin{:});

produceTAP               = p.Results.TapResults;
produceJUnit             = p.Results.JunitResults;
produceTestReport        = p.Results.MATLABTestReport;
saveSimulinkTestResults  = p.Results.SimulinkTestResults;
produceCodeCoverage      = p.Results.CoberturaCodeCoverage;
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

% Produce Cobertura coverage report (Cobertura report generation is not supported
% below R17a) 
if produceCodeCoverage
    BASE_VERSION_COBERTURA_SUPPORT = '9.3';
    if verLessThan('matlab',BASE_VERSION_COBERTURA_SUPPORT)
        warning('MATLAB:testArtifact:coberturaReportNotSupported', 'Producing Cobertura results is not supported in this release.');
    else
        import('matlab.unittest.plugins.CodeCoveragePlugin');
        import('matlab.unittest.plugins.codecoverage.CoberturaFormat');
        
        mkdirIfNeeded(resultsDir);
        
        % Generate code coverage report
        codeCoverageFile = fullfile(resultsDir, 'codecoverage.xml');
        workSpace = fullfile(pwd);
        runner.addPlugin(CodeCoveragePlugin.forFolder(workSpace,'IncludingSubfolders',true,...
            'Producing', CoberturaFormat(codeCoverageFile)));
    end
end

% Produce Cobertura model coverage report (Not supported below R2018a) 
if produceModelCoverage
    if ~exist('sltest.plugins.ModelCoveragePlugin', 'class')
        warning('MATLAB:testArtifact:cannotGenerateModelCoverageReport', ...
                'Unable to generate model coverage report. Either the required toolbox is not present or the feature is not supported in this release.');
    else 
        import sltest.plugins.ModelCoveragePlugin;
        import matlab.unittest.plugins.codecoverage.CoberturaFormat;
        
        mkdirIfNeeded(resultsDir);
        coverageFile = fullfile(resultsDir, 'modelcoverage.xml');
        runner.addPlugin(ModelCoveragePlugin('Producing',CoberturaFormat(coverageFile)));
    end
end

% Produce an unified test report which includes MATLAB and/or Simulink Test Manager 
% test results (Not supported below R2016b)
if produceTestReport && ~saveSimulinkTestResults
    if testReportPluginNotPresent
        warning('MATLAB:testArtifact:testReportNotSupported', ...
            'Producing MATLAB test report is not supported in this release.');
    else
        import matlab.unittest.plugins.TestReportPlugin;
        mkdirIfNeeded(resultsDir);
        
        if exist('sltest.plugins.TestManagerResultsPlugin', 'class')
            runner.addPlugin(TestManagerResultsPlugin);
        end
        runner.addPlugin(TestReportPlugin.producingPDF(getPDFFile(resultsDir)));
    end
end

% Save Simulink Test Manager results in MLDATX format (Not supported below R2019a)
if saveSimulinkTestResults && ~produceTestReport
    if ~exist('sltest.plugins.TestManagerResultsPlugin', 'class')
        warning('MATLAB:testArtifact:cannotSaveSimulinkTestResults', ...
            'Unable to save Simulink Test Manager results. Either the toolbox is not present or the feature is not supported in this release.');
    else
        mkdirIfNeeded(resultsDir);
        runner.addPlugin(TestManagerResultsPlugin('ExportToFile', getMLDATXFile(resultsDir)));
    end
end

% 1. Produce a test report which includes MATLAB and/or Simulink Test Manager test results (Not supported below R2016b)
% 2. Save Simulink Test Manager results in MLDATX file (Not supported below R2019a)
if produceTestReport && saveSimulinkTestResults
    try
        import matlab.unittest.plugins.TestReportPlugin;
        mkdirIfNeeded(resultsDir);

        testReportPlugin = TestReportPlugin.producingPDF(getPDFFile(resultsDir));
        stmResultsPlugin = TestManagerResultsPlugin('ExportToFile', getMLDATXFile(resultsDir));
    catch ME
        testReportPlugin = matlab.unittest.plugins.TestRunnerPlugin.empty;
        stmResultsPlugin = matlab.unittest.plugins.TestRunnerPlugin.empty;
        
        if testReportPluginNotPresent
            warning('MATLAB:testArtifact:testReportNotSupported', ...
                'Producing MATLAB test report is not supported in this release.');
        else
            if exist('sltest.plugins.TestManagerResultsPlugin', 'class')
                runner.addPlugin(TestManagerResultsPlugin);
            end
            testReportPlugin = TestReportPlugin.producingPDF(getPDFFile(resultsDir));
        end
            
        if ~exist('sltest.plugins.TestManagerResultsPlugin', 'class')
            warning('MATLAB:testArtifact:cannotSaveSimulinkTestResults', ...
                'Unable to save Simulink Test Manager results. Either the toolbox is not present or the feature is not supported in this release.');
        end
    end
    runner.addPlugin(testReportPlugin);
    runner.addPlugin(stmResultsPlugin);
end

% tic;
% % 1. Produce a test report which includes MATLAB and/or Simulink Test Manager test results (Not supported below R2016b)
% % 2. Save Simulink Test Manager results in MLDATX file (Not supported below R2019a)
% if produceTestReport || saveSimulinkTestResults
%     if produceTestReport && testReportPluginNotPresent
%         warning('MATLAB:testArtifact:artifactNotSupported', ...
%             'Producing test report is not supported in this release.');
%         
%         if saveSimulinkTestResults
%             warning('MATLAB:testArtifact:cannotSaveSimulinkTestResults', ...
%                 'Unable to save Simulink Test Manager results. Either the toolbox is not present or the feature is not supported in this release.');
%         end
%         
%     elseif saveSimulinkTestResults && ~exist('sltest.plugins.TestManagerResultsPlugin', 'class')
%         warning('MATLAB:testArtifact:cannotSaveSimulinkTestResults', ...
%             'Unable to save Simulink Test Manager results. Either the toolbox is not present or the feature is not supported in this release.');
%         
%         if produceTestReport
%             import matlab.unittest.plugins.TestReportPlugin;
%             mkdirIfNeeded(resultsDir);
%             
%             if exist('sltest.plugins.TestManagerResultsPlugin', 'class')
%                 runner.addPlugin(TestManagerResultsPlugin);
%             end
%             runner.addPlugin(TestReportPlugin.producingPDF(getPDFFile(resultsDir)));
%         end
%         
%     else
%         import matlab.unittest.plugins.TestReportPlugin;
%         mkdirIfNeeded(resultsDir);
%         
%         if produceTestReport && saveSimulinkTestResults
%             runner.addPlugin(TestReportPlugin.producingPDF(getPDFFile(resultsDir)));
%             runner.addPlugin(TestManagerResultsPlugin('ExportToFile', getMLDATXFile(resultsDir)));
%             
%         elseif produceTestReport
%             if exist('sltest.plugins.TestManagerResultsPlugin', 'class')
%                 runner.addPlugin(TestManagerResultsPlugin);
%             end
%             runner.addPlugin(TestReportPlugin.producingPDF(getPDFFile(resultsDir)));
%         else
%             runner.addPlugin(TestManagerResultsPlugin('ExportToFile', getMLDATXFile(resultsDir)));
%         end
%     end
% end
% toc;

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

function fileName = getPDFFile(resultsDir)
fileName = fullfile(resultsDir, 'testreport.pdf');

function fileName = getMLDATXFile(resultsDir)
fileName = fullfile(resultsDir, 'simulinktestresults.mldatx');

function tf = modelCoveragePluginNotPresent()
BASE_VERSION_MODELCOVERAGE_SUPPORT = '9.4'; % R2018a

tf = verLessThan('matlab',BASE_VERSION_MODELCOVERAGE_SUPPORT);

function tf = testReportPluginNotPresent()
BASE_VERSION_REPORTPLUGIN_SUPPORT = '9.1'; % R2016b

tf = verLessThan('matlab',BASE_VERSION_REPORTPLUGIN_SUPPORT);

function tf = savingSTMResultsNotSupported()
BASE_VERSION_SAVINGSTMRESULTS_SUPPORT = '9.6';  % R2019a

tf = verLessThan('matlab',BASE_VERSION_SAVINGSTMRESULTS_SUPPORT);
