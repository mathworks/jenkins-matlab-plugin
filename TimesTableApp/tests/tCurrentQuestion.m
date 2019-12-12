classdef tCurrentQuestion < matlab.unittest.TestCase
    %tCurrentQuestion   MATLAB unit test for the random number generation
    
    % Copyright 2018 The MathWorks, Inc.
    
    properties
        gameObject;
        numTimesToRepeat = 1000;
    end
    
    methods(TestClassSetup)        
        function setupGameObject(testCase)
            testCase.gameObject = timesTableGame;
        end        
    end
    
    methods(Test)        
        function verifyRemainsInLimit(testCase)
            for count = 1:testCase.numTimesToRepeat                
                % Set a new question:
                testCase.gameObject.setNewQuestion;
                % Confirm within expected range:
                testCase.verifyGreaterThanOrEqual( testCase.gameObject.CurrentQuestion, 1);
                testCase.verifyLessThanOrEqual( testCase.gameObject.CurrentQuestion, 12);
            end
        end
    end
    
end
