rem Copyright 2020 The MathWorks, Inc.

echo "tester_started"

set "arg1=%~1"

echo "%arg1%"

if "%arg1%" == "exitMatlab" (exit /b 1)
