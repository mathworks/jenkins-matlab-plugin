#!/bin/bash

#Copyright 2018 The MathWorks, Inc.

if [ "$1" == "-positive" ]; then
    echo "MATLAB is invoking positive tests"
fi

if [ "$1" == "negative" ]; then
    echo "MATLAB is invoking negative tests"
fi

if [ "$1" == "failTests" ]; then
    echo "Build failed due to test failure"
    exit 31
fi

if [ "$1" == "-positiveFail" ]; then
    echo "MATLAB exception occured"
    exit 1
fi
