rem Copyright 2018 The MathWorks, Inc.

echo off

set "arg1=%~1"


if "%arg1%" == "-positive" echo "MATLAB is invoking positive tests"
if "%arg1%" == "-negative" echo "MATLAB is invoking negative tests"
if "%arg1%" == "-positiveFail" ( echo "Build failed due to test failure" 
exit 1  )
if "%arg1%" == "failTests" exit /b 1  
