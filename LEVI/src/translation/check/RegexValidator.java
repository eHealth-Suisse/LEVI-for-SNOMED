package translation.check;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class RegexValidator {
	private static final Pattern UPPERCASE_PATTERN = Pattern.compile("^\\p{Lu}"); // First letter upper case/lower case
	private static final Pattern QUOTES_PATTERN = Pattern.compile("[\"“”„]");  // No Quotes    
	private static final Pattern SPACE_AROUND_SLASH_PATTERN = Pattern.compile("\\s/\\s|\\s/|/\\s"); // No space before and after slash
	private static final Pattern SOFT_HYPHEN_PATTERN = Pattern.compile("\\u00AD"); // No Soft Hyphen
	
    public static List<String> validateTerm(String languageCode, String term) {
        List<String> results = new ArrayList<>();
        
        //General checks for all languages
        boolean hasQuotes = term != null && QUOTES_PATTERN.matcher(term).find();
        results.add(hasQuotes ? "Bitte überprüfen" : "OK");
        
        boolean hasSoftHyphen =term !=null && SOFT_HYPHEN_PATTERN.matcher(term).find();
        results.add(hasSoftHyphen ? "Bitte überprüfen" : "OK");

        
        if (languageCode.equals("de")) {
			//Specific checks for German language
        	boolean startsUpper = term != null && UPPERCASE_PATTERN.matcher(term).find();
        	results.add(startsUpper ? "OK" : "Bitte überprüfen");
            boolean hasSpaceAround = term != null && SPACE_AROUND_SLASH_PATTERN.matcher(term).find();
            results.add(hasSpaceAround ? "Bitte überprüfen" : "OK");
            
		} else if (languageCode.equals("fr")) {
			results.add("nein"); // French does not require uppercase at the start
			
		} else if (languageCode.equals("it")) {
			results.add("nein"); // Italian does not require uppercase at the start
		}  else {
			results.add("language code not recognized and therefore not checked"); // Default case for other languages
		}
        return results;
    }
}
