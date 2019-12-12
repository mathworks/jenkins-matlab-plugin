classdef timesTableGame < handle
    %TIMESTABLEGAME A simple educational app that tests the user on times
    %tables. 
    
    % Copyright 2018 The MathWorks, Inc.
    
    properties
        CurrentTimesTable = 3;
        CurrentQuestion;
        NumberCorrectAnswers = 0;
    end
    
    methods
        
        function obj = timesTableGame()
            % 
        end
        
        function b = isAnswerCorrect(obj, guess)
            % Check that "guess" is valid -- expect a number
            numericGuess = str2double(guess);
            if isempty(numericGuess)
                b = false;
                return
            end
            
            expectedAnswer = obj.CurrentTimesTable * obj.CurrentQuestion;
            b = (expectedAnswer == numericGuess);
        end
        
        function setNewTimesTable(obj, newTimesTable)
            % update obj data, reset current answer and questions
            obj.CurrentTimesTable = str2double(newTimesTable);
            obj.askNewQuestion;
        end
        
        function askNewQuestion(obj)
            obj.setNewQuestion;
        end
        
        function incrementScore(obj)
            obj.NumberCorrectAnswers = obj.NumberCorrectAnswers + 1;
        end
        
        function setNewQuestion(obj)
            % Pick a random number between 1 and 12:
            obj.CurrentQuestion = floor(12*rand(1)) + 1;
        end
        
    end
end

