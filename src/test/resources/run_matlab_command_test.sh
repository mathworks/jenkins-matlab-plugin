#!/bin/bash

#Copyright 2020 The MathWorks, Inc.

echo "tester_started"
echo $1
if [[ $1 == "exitMatlab" ]]; then
  exit 1
fi
