package com.mathworks.ci;

/**
 * Copyright 2020 The MathWorks, Inc.
 *
 * Utility class for common methods.
 *
 */

import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class Utilities {

    public static String getCellArrayFrmList(List<String> listOfStr){
        // Ignore empty string values in the list
        Predicate<String> isEmpty = String::isEmpty;
        Predicate<String> isNotEmpty = isEmpty.negate();
        List<String> filteredListOfStr = listOfStr.stream().filter(isNotEmpty).collect(Collectors.toList());

        // Escape apostrophe for MATLAB
        filteredListOfStr.replaceAll(val -> "'" + val.replaceAll("'", "''") + "'");
        return "{" + String.join(",", filteredListOfStr) + "}";
    }
}
