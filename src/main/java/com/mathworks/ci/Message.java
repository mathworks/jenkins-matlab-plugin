package com.mathworks.ci;

/* Copyright 2018-2024 The MathWorks, Inc. 
 * 
 * This Class is wrapper to access the static configuration values across project. Acts as 
 * Utility class to access key & value pairs from config.properties
 */

import java.util.ResourceBundle;

public class Message {

	private static String MATLAB_BUILDER_DISPLAY_NAME = "Builder.display.name";
	private static String CONFIG_FILE = "config";

	private static ResourceBundle rb = ResourceBundle.getBundle(CONFIG_FILE);

	public static String getBuilderDisplayName() {

		return rb.getString(MATLAB_BUILDER_DISPLAY_NAME);

	}

	public static String getValue(String key) {

		return rb.getString(key);
	}

}
