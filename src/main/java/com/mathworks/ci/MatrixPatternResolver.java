package com.mathworks.ci;

/*
 * Copyright 2019-2024 The MathWorks, Inc.
 * 
 * This is Matrix pattern resolver class which is a utility for identifying variables. Either $xyz,
 * ${xyz} or ${a.b} but not $a.b, while ignoring "$$"
 */

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MatrixPatternResolver {
    private String inputString;
    private static Pattern VARIBLE = Pattern.compile("\\$([A-Za-z0-9_]+|\\{[A-Za-z0-9_.]+\\}|\\$)");

    public MatrixPatternResolver(String inputString) {
        this.inputString = inputString;
    }

    public String getInputString() {
        return this.inputString;
    }

    public boolean hasVariablePattern() {
        Matcher m = VARIBLE.matcher(getInputString());
        return m.find(0);
    }
}
