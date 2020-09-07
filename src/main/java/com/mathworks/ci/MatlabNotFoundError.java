package com.mathworks.ci;
/**
 * Copyright 2020 The MathWorks, Inc.
 * 
 */

public class MatlabNotFoundError extends Error  {

    private static final long serialVersionUID = -8643427754117565716L;
    
   MatlabNotFoundError(String errorMessage){
       super(errorMessage);
    }
}
