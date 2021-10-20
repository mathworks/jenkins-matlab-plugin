package com.mathworks.ci;

public class Verbosity {
	private static String [] arr ;
	
    public static String[] getverbosity()
    {
    	arr = new String[]{"None","Concise","Terse","Detailed","Verbose"} ;
    	return arr ;
    }
}
