package com.mathworks.ci;
import java.util.*;

public class Verbosity {
	
	public static enum verbosityTypes implements java.io.Serializable {
		NONE, TERSE, CONCISE, DETAILED , VERBOSE
	}
	
	private static Map<verbosityTypes, String> verbosityMap = new HashMap() ;
	
    public static Map<verbosityTypes, String> getverbosityValue()
    {
    	verbosityMap.put(verbosityTypes.NONE, "Verbosity.None");
    	verbosityMap.put(verbosityTypes.TERSE, "Verbosity.Terse");
    	verbosityMap.put(verbosityTypes.CONCISE, "Verbosity.Concise");
    	verbosityMap.put(verbosityTypes.DETAILED, "Verbosity.Detailed");
    	verbosityMap.put(verbosityTypes.VERBOSE, "Verbosity.Verbose");

    	return verbosityMap ;
    }
    
<<<<<<< HEAD
}
=======
}
>>>>>>> d6c552409ce3d4a484b0f57f51d5386aa6946cb6
