classdef TestRand < matlab.unittest.TestCase
    properties (ClassSetupParameter)
        generator = {'twister','combRecursive','multFibonacci'};
    end
    
    properties (MethodSetupParameter)
        seed = {0, 123, 4294967295};
    end
    
    properties (TestParameter)
        dim1 = struct('small', 1,'medium', 2, 'large', 3);
        dim2 = struct('small', 2,'medium', 3, 'large', 4);
        dim3 = struct('small', 3,'medium', 4, 'large', 5);
        type = {'single','double'};
    end
    
    methods (TestClassSetup)
        function ClassSetup(testCase, generator)
            orig = rng;
            testCase.addTeardown(@rng, orig)
            rng(0, generator)
        end
    end
    
    methods (TestMethodSetup)
        function MethodSetup(testCase, seed)
            orig = rng;
            testCase.addTeardown(@rng, orig)
            rng(seed)
        end
    end
    
    methods (Test, ParameterCombination='sequential')
        function testSize(testCase,dim1,dim2,dim3)
            testCase.verifySize(rand(dim1,dim2,dim3),[dim1 dim2 dim3])
        end 
    end
    
    methods (Test, ParameterCombination='pairwise')
        function testRepeatable(testCase,dim1,dim2,dim3)
            state = rng;
            firstRun = rand(dim1,dim2,dim3);
            rng(state)
            secondRun = rand(dim1,dim2,dim3);
            testCase.verifyEqual(firstRun,secondRun);
        end
    end
    
    methods (Test)
        function testClass(testCase,dim1,dim2,type)
            testCase.verifyClass(rand(dim1,dim2,type), type)
        end
    end
end