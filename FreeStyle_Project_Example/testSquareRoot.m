classdef testSquareRoot < matlab.unittest.TestCase

    methods (Test)
        function testOutput(testCase)
            testCase.verifyEqual(squareRoot(16),4);
            testCase.verifyEqual(squareRoot(81),9);            
        end
    end
 
end

