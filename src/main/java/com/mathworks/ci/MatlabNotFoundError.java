package com.mathworks.ci;

/**
 * Copyright 2020 The MathWorks, Inc.
 * 
 */

import java.io.FileNotFoundException;

public class MatlabNotFoundError extends Error {

    private static final long serialVersionUID = 7918595075502022644L;

    MatlabNotFoundError(String errorMessage){
        super(errorMessage);
    }

}
