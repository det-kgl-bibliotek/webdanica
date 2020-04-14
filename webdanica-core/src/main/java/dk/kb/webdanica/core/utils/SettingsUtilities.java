package dk.kb.webdanica.core.utils;

import java.io.File;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class for settingsfile utilities.
 */
public class SettingsUtilities {
	
	/** Logging mechanism. */
    private static final Logger logger = LoggerFactory.getLogger(SettingsUtilities.class);

    /**
     * Tests if the given settingsFile is a valid SimpleXMl settingsfile.
     * @param settingsFile a given SimpleXml based settings-file.
     * @param verbose if true, write the stack-trace in the logfile.
     * @return true, if the settingsFile is valid, else false;
     */
    public static boolean isValidSimpleXmlSettingsFile(File settingsFile, boolean verbose) {
    	try {
    		new SimpleXml(settingsFile);
    	} catch(Throwable e) {
    		if (verbose) {
    			logger.warn(e.toString());
    		}
    		return false;
    	}
    	return true;
    }
    
    /**
     * Tests if the given settingsFile is a valid SimpleXMl settingsfile.
     * @param settingsFile a given SimpleXml based settings-file.
     * @return true, if the settingsFile is valid, else false;
     */
	public static boolean isValidSimpleXmlSettingsFile(File settingsFile) {
		return isValidSimpleXmlSettingsFile(settingsFile, false);
	}
	
	public static String getStringSetting(String settingsName, String default_string_value) {
		return getStringSetting(settingsName, default_string_value, true);
	}
	
	public static String getStringSetting(String settingsName, String default_string_value, boolean logging) {
    	String returnValue = default_string_value;
	    if (Settings.hasKey(settingsName)) {
	    	String settingsValue = Settings.get(settingsName);  
	    	if (settingsValue == null || settingsValue.isEmpty()) {
	    		if (logging) logger.warn("Using default value '" + default_string_value + "' for setting '" + settingsName + "', as the value in the settings is null or empty");
	    	} else {
	    		if (logging) logger.info("Using value '" + settingsValue + "' for setting '" + settingsName + "'.");
	    		returnValue = settingsValue;
	    	}
	    } else {
	    	if (logging) logger.warn("The setting '" + settingsName + "' is not defined in the settingsfile. Using the default value: '" + default_string_value + "'");
	    }
	    return returnValue;
    }
    
	public static int getIntegerSetting(String settingsName, int default_int_value) {
	    return getIntegerSetting(settingsName, default_int_value, true);
	}
	
	public static int getIntegerSetting(String settingsName, int default_int_value, boolean logging) {
	 	int returnValue = default_int_value;
	    if (Settings.hasKey(settingsName)) {
	    	String settingsValueAsString = Settings.get(settingsName);  
	    	if (settingsValueAsString == null || settingsValueAsString.isEmpty()) {
	    	    if (logging) logger.warn("Using default value '" + default_int_value + "' for setting '" + settingsName + "', as the value in the settings is null or empty");
	    	} else { // Try to parse the settingsValueAsString as a valid Integer
	    		int intValue;
	    		try {
	            	intValue = Integer.parseInt(settingsValueAsString);
	            	returnValue = intValue;
	            	if (logging) logger.info("Using value '" + returnValue + "' for setting '" + settingsName + "'.");
	            } catch (NumberFormatException e) {
	                if (logging) logger.warn("Using default value '" + default_int_value + "' for setting '" + settingsName + "', as the value '" + settingsValueAsString
	            			+ "'  in the settings is not a valid integer");
	            }
	    	}
	    } else {
	        if (logging) logger.warn("The setting '" + settingsName + "' is not defined in the settingsfile. Using the default value: '" + default_int_value + "'");
	    }
	    return returnValue;
    }

	public static boolean getBooleanSetting(String settingsName, boolean default_bool_value) {
		boolean returnValue = default_bool_value;
	    if (Settings.hasKey(settingsName)) {
	    	String settingsValueAsString = Settings.get(settingsName);  
	    	if (settingsValueAsString == null || settingsValueAsString.isEmpty()) {
	    		logger.warn("Using default value '" + default_bool_value + "' for setting '" + settingsName + "', as the value in the settings is null or empty");
	    	} else {
	    		boolean boolValue = Boolean.parseBoolean(settingsValueAsString);
	            returnValue = boolValue;
	            logger.info("Using value '" + returnValue + "' for setting '" + settingsName + "'.");
	    	}
	    } else {
	    	logger.warn("The setting '" + settingsName + "' is not defined in the settingsfile. Using the default value: '" + default_bool_value + "'");
	    }
	    return returnValue;
    }
	
	/**
	 * test if property file is defined by the given propertyKey. If not call
	 * System.exit(1);
	 * @param propertyKey a key for a property
	 */
	public static boolean testPropertyFile(String propertyKey, boolean exitIfCheckFails){
		String setting = System.getProperty(propertyKey);
		if (setting == null) {
			if (exitIfCheckFails) {
				System.err.println("ERROR: Required java property '" + propertyKey + "' is undefined");
				System.exit(1);
			} else {
				logger.warn("Required java property '" + propertyKey + "' is undefined");
				return false;
			}
		}
		File settingsFile = new File(setting);
	
		if (!settingsFile.exists()) {
			String errMsg = "ERROR: The settings file defined by property '" + propertyKey + "' does not exist: " 
					+ settingsFile.getAbsolutePath() + "' does not exist";
			if (exitIfCheckFails) {
				System.err.println(errMsg);
				System.exit(1);
			} else {
				logger.warn(errMsg);
				return false;
			}
		}
		return true;
	}

	public static boolean verifyClass(String dbdriver, boolean exitIfcheckFails) {
		try {
			Class.forName(dbdriver);
		} catch (ClassNotFoundException e) {
			if (exitIfcheckFails) {
				System.out.println("ERROR: Required class '" + dbdriver + "' not found in classpath");
				System.out.println("Program terminated");
				System.exit(1);
			}
			return false;
		}
		return true;
    }

	public static boolean verifyWebdanicaSettings(Set<String> requiredSettings, boolean exitIfCheckFails) {
	    boolean exit = false;
	    for (String key: requiredSettings){
	    	if (!Settings.hasKey(key)) {
	    		exit = true;
	    		System.err.println("ERROR: Missing setting '" + key + "' in settingsfile");
	    	}
	    }
	    if (exit && exitIfCheckFails) {
	    	System.err.println("ERROR: Exiting program prematurely because of missing settings");
	    	System.exit(1);
	    } else if (exit) {
	    	return false;
	    }
	    return true;
    }

	public static long getLongSetting(String settingsName, long default_long_value) {
        long returnValue = default_long_value;
        if (Settings.hasKey(settingsName)) {
            String settingsValueAsString = Settings.get(settingsName);  
            if (settingsValueAsString == null || settingsValueAsString.isEmpty()) {
                logger.warn("Using default value '" + default_long_value + "' for setting '"
                        + settingsName + "', as the value in the settings is null or empty");
            } else { // Try to parse the settingsValueAsString as a valid Long
                long longValue;
                try {
                    longValue = Long.parseLong(settingsValueAsString);
                    returnValue = longValue;
                    logger.info("Using value '" + returnValue + "' for setting '" + settingsName + "'.");
                } catch (NumberFormatException e) {
                    logger.warn("Using default value '" + default_long_value + "' for setting '" + settingsName + "', as the value '"
                            + settingsValueAsString 
                            + "'  in the settings is not a valid Long");
                }
            }
        } else {
            logger.warn("The setting '" + settingsName + "' is not defined in the settingsfile. Using the default value: '"
                    + default_long_value + "'");
        }
        return returnValue;
    }
}

