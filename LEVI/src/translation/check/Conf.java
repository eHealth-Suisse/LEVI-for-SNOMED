package translation.check;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class Conf {
	
		
	// Paths to the files and directories
	private String filePathCurrent = "PATH_TO_CURRENT_FILE"; // Path to the CSV/Excel file with the terms to be compared
	private String filePathPrevious = "PATCH_TO_PREVIOUS_FILE"; // Path to the previous CSV/Excel file with the terms to be compared
	private String destination = "DESTINATION_WEHER_TO_CREATE_FILES"; // Path where to create the three files	

	// Database connection variables
	private String SERVER_URL = "jdbc:mysql://localhost/---INSERT_DB_NAME---?useUnicode=true&characterEncoding=UTF-8";
	private String USERNAME = "root";
	private String PASSWORD = "";
	
	//default settings
	private String countryCode = "CH"; // Country code for selecting the local language reference sets
	private boolean transformEszett = true ;// ture = Eszeet should be changed to "ss" in the translations
	private boolean regexCheck = true; // true = regex check is performed on the terms in the current file
	
	
	
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

    public Conf() {
		// Default constructor
    	this.countryCode = "CH";
        this.transformEszett = true; // CH-Default
	}
       
    public void setFilePathCurrent(String filePathCurrent) {
		this.filePathCurrent = filePathCurrent;
	}
    
    public void setFilePathPrevious(String filePathPrevious) {
    	this.filePathPrevious = filePathPrevious;
    }
	
	public void setDestination(String destination) {
		this.destination = destination;
	}
    
    public void setTransformEszett(boolean transformEszett) {
        this.transformEszett = transformEszett;
    }
    
    public void setRegexCheck(boolean regexCheck) {
        this.regexCheck = regexCheck;
    }
    
    public void setDbUrl(String serverUrl) {
		this.SERVER_URL = serverUrl;
	}
    
    public void setDbUsername(String username) {
    	this.USERNAME = username;
    }
    
    public void setDbPassword(String password) {
		this.PASSWORD = password;
	}
    
    private Map<String, String> getLanguageRefSets(String countryCode) {
        return countryToLanguageRefSets.getOrDefault(countryCode.toUpperCase(), Collections.emptyMap());
    }

    public String getLanguageRefSetId(String languageCode) {
        return getLanguageRefSets(countryCode).get(languageCode.toLowerCase());
    }
    
    public  String getSERVER_URL() {
    	return SERVER_URL;
    }
    
    public  String getUSERNAME() {
    	return USERNAME;
    }
    
    public  String getPASSWORD() {
		return PASSWORD;
	}
    
    public String getFilePathCurrent() {
		return this.filePathCurrent;
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

    
    public Set<String> getLocalLanguages() {
        Map<String, String> langs =
                countryToLanguageRefSets.getOrDefault(countryCode.toUpperCase(), Map.of());
        return langs.keySet();
    }
    
    public boolean isLocalLanguage(String languageCode) {
        if (languageCode == null) return false;
        return getLocalLanguages().contains(languageCode.trim().toLowerCase());
    }

	public String getCountryCode() {
		return countryCode;
	}
	
    public void setCountryCode(String countryCode) {
        this.countryCode = countryCode;
    }
	
	public boolean checkRegex() {
		return regexCheck;
	}

}
