classdef tNewTimesTable < matlab.unittest.TestCase
    %tNewTimesTable   MATLAB unit test for changing the time stable
    
    % Copyright 2018 The MathWorks, Inc.
    
    properties
        gameObject;
    end
    
    methods(TestClassSetup)        
        function setupGameObject(testCase)
            testCase.gameObject = timesTableGame;            
        end        
    end
    
    methods(Test)        
        function verifyCanChangeTimesTable(testCase)
            currentTable = testCase.gameObject.CurrentTimesTable;
            newTable = 7;            
            testCase.assertNotEqual(currentTable, newTable);
            
            % Times table expects a string for the currentTimesTable
            testCase.gameObject.setNewTimesTable(num2str(newTable));
            testCase.verifyEqual(testCase.gameObject.CurrentTimesTable, newTable);            
        end
    end
    
end
