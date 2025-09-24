package translation.check;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class Conf {
	
	public Conf() {
		// Default constructor
	}
	
	// Paths to the files and directories
	private static String filePathCurrent = "PATH_TO_FILE"; // Path to the CSV/Excel file with the terms to be compared
	private static String filePathPrevious = "PATH_TO_FILE"; // Path to the previous CSV/Excel file with the terms to be compared
	private static String destination = "PATH_TO_OUTPUT_FOLDER"; // Path where to create the three files	

	// Database connection variables
	private static String SERVER_URL = "jdbc:mysql://localhost/**DB_NAME**?useUnicode=true&characterEncoding=UTF-8";
	private static String USERNAME = "root";
	private static String PASSWORD = "";
	
	private static String countryCode = "CH";
	private static boolean transformEszett = true; // ture = Eszeet should be changed to "ss" in the translations
	private static boolean regexCheck = false; // true = regex check is performed on the terms in the current file
		
	
	
	
	
	
	
	
	
	
	//// Language reference sets for different countries
    private static final Map<String, Map<String, String>> countryToLanguageRefSets = new HashMap<>();

    static {
        countryToLanguageRefSets.put("AT", Map.of(
                "de", "21000234103"
        ));
        countryToLanguageRefSets.put("AU", Map.of(
                "en", "32570271000036106"
        ));
        countryToLanguageRefSets.put("BE", Map.of(
                "fr", "21000172104",
                "nl", "31000172101"
        ));
        countryToLanguageRefSets.put("GB", Map.of(
                "en", "900000000000508004"
        ));
        countryToLanguageRefSets.put("US", Map.of(
                "en", "900000000000509007"
        ));
        countryToLanguageRefSets.put("NZ", Map.of(
                "en", "271000210107"
        ));
        countryToLanguageRefSets.put("IE", Map.of(
                "en", "21000220103"
        ));
        countryToLanguageRefSets.put("DK", Map.of(
                "da", "554461000005103"
        ));
        countryToLanguageRefSets.put("FR", Map.of(
                "fr", "10031000315102"
        ));
        countryToLanguageRefSets.put("CH", Map.of(
                "de", "2041000195100",
                "fr", "2021000195106",
                "it", "2031000195108"
        ));
        countryToLanguageRefSets.put("NO", Map.of(
                "no", "61000202103"
        ));
        countryToLanguageRefSets.put("EE", Map.of(
                "et", "71000181105"
        ));
        countryToLanguageRefSets.put("KR", Map.of(
                "kr", "21000267104"
        ));
        countryToLanguageRefSets.put("NL", Map.of(
                "nl", "31000146106"
        ));
        countryToLanguageRefSets.put("SE", Map.of(
                "sv", "46011000052107"
        ));
    }

    private Map<String, String> getLanguageRefSets(String countryCode) {
        return countryToLanguageRefSets.getOrDefault(countryCode.toUpperCase(), Collections.emptyMap());
    }

    public String getLanguageRefSetId(String languageCode) {
        return getLanguageRefSets(countryCode).get(languageCode.toLowerCase());
    }
    
    public static String getSERVER_URL() {
    	return SERVER_URL;
    }
    
    public static String getUSERNAME() {
    	return USERNAME;
    }
    
    public static String getPASSWORD() {
		return PASSWORD;
	}
    
    public String getFilePathCurrent() {
		return filePathCurrent;
	}
    
    public String getFilePathPrevious() {
		return filePathPrevious;
	}
	
	public String getDestination() {
		return destination;
	}
	
	public boolean isTransformEszett() {
		return transformEszett;
	}
	
	public static String getCountryCode() {
		return countryCode;
	}
	
	public boolean checkRegex() {
		return regexCheck;
	}

}
