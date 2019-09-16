classdef testSquareRootParameterized < matlab.unittest.TestCase
    
    properties (TestParameter)
        inputs = {4, 16, 81, 121, 49};
        expected_outputs = {2, 4, 9 , 11, 7};
    end
    
%     methods (TestClassSetup)
%         function addTestContentToPath(~)
%             %cd ..;
%             addpath(fullfile(pwd,'source'));
%         end
%     end       
    
    
    methods (Test,ParameterCombination='sequential')
        function testOutput(testCase,inputs,expected_outputs)
            testCase.verifyEqual(squareRoot(inputs),expected_outputs);
        end
        
        function testError(testCase)
            testCase.verifyError(@()squareRoot(-1),'SQUAREROOT:INVALIDINPUT');
        end
  
    end
  
    
end

