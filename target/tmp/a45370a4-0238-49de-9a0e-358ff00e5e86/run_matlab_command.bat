@echo off
rem Executes a specified MATLAB command in batch mode. With MATLAB R2018b+,
rem running this script is equivalent to executing "matlab -batch command" from
rem the system prompt.
rem
rem Copyright 2020 The MathWorks, Inc.

setlocal enableextensions enabledelayedexpansion

set command=%~1
if "%command%" == "" (
    call :usage
    exit /b 1
)

for %%a in (matlab.exe) do set matlab_path=%%~$PATH:a

if "%matlab_path%" == "" (
    echo 'matlab.exe' command not found. Please make sure MATLAB_ROOT\bin is on
    echo the system path, where MATLAB_ROOT is the full path to your MATLAB
    echo installation directory.
    exit /b 1
)

for %%a in ("%matlab_path%\..\..") do set matlab_root=%%~fa

rem try to discover the MATLAB version
if exist "%matlab_root%\VersionInfo.xml" (
    rem get version tag contents
    for /f %%a in ('findstr "<version>.*</version>" "%matlab_root%\VersionInfo.xml"') do (
        set ver_line=%%a
        set ver_line=!ver_line:*^<version^>=!
        for /f "tokens=1 delims=<" %%b in ("!ver_line!") do set matlab_ver=%%b
    )
) else if exist "%matlab_root%\toolbox\matlab\general\Contents.m" (
    rem get version printed after "MATLAB Version"
    for /f "delims=" %%a in ('findstr /r /c:"MATLAB Version .*" "%matlab_root%\toolbox\matlab\general\Contents.m"') do (
        set ver_line=%%a
        set ver_line=!ver_line:*MATLAB Version =!
        for /f "tokens=1" %%b in ("!ver_line!") do set matlab_ver=%%b
    )
)

rem if version not discovered, assume worst-case version of 0
if not defined matlab_ver set matlab_ver=0

rem use -r to launch MATLAB versions below R2018b (i.e. 9.5), otherwise use -batch
call :ver_less_than %matlab_ver% 9.5
if %errorlevel% == 0 (
    rem define start-up options
    set opts=-nosplash -nodesktop -wait -log
    call :ver_less_than %matlab_ver% 8.5
    if not !errorlevel! == 0 set opts=!opts! -noDisplayDesktop
    
    rem escape single quotes in command
    set exp=!command:'=''!
    
    matlab.exe !opts! -r "try,eval('!exp!'),catch e,disp(getReport(e,'extended')),exit(1),end,exit" > NUL
) else (
    matlab.exe -batch "%command%"
)
exit /b %errorlevel%

:usage
echo     Usage: run_matlab_command.sh command
echo.
echo     command       - MATLAB script, statement, or function to execute.
echo.
goto :eof

:ver_less_than
setlocal
call :ver_str %~1 v1
call :ver_str %~2 v2
if "%v1%" lss "%v2%" ( exit /b 0 ) else ( exit /b 1 )

:ver_str
setlocal
set ver=%~1
for /f "tokens=1-4 delims=." %%a in ("%ver%") do (
    set major=%%a
    set minor=000%%b
    set minor=!minor:~-3!
    set patch=000%%c
    set patch=!patch:~-3!
    set build=000000000%%d
    set build=!build:~-9!
)
( endlocal & rem return values
    set %~2=%major%%minor%%patch%%build%
)
goto :eof
