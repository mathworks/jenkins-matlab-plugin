package com.mathworks.ci;

import java.io.FileNotFoundException;

public class MatlabNotFoundError extends Error {
   
    /**
     * 
     */
    private static final long serialVersionUID = 7918595075502022644L;

    MatlabNotFoundError(String errorMessage){
        super(errorMessage);
    }

}
