package translation.check;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class RegexValidator {
	private static final Pattern UPPERCASE_PATTERN = Pattern.compile("^\\p{Lu}"); // First letter upper case/lower case
	private static final Pattern QUOTES_PATTERN = Pattern.compile("[\"“”„]");  // No Quotes    
	private static final Pattern SPACE_AROUND_SLASH_PATTERN = Pattern.compile("\\s/\\s|\\s/|/\\s"); // No space before and after slash
	private static final Pattern SOFT_HYPHEN_PATTERN = Pattern.compile("\\u00AD"); // No Soft Hyphen
	private static final Pattern APOSTROPHE_PATTERN=Pattern.compile("//`");
	

    public static List<String> validateTerm(String languageCode, String term) {
        List<String> results = new ArrayList<>();
        
        //General checks for all languages
        boolean hasQuotes = term != null && QUOTES_PATTERN.matcher(term).find();
        results.add(hasQuotes ? "Please check" : "OK");
        
        boolean hasSoftHyphen =term !=null && SOFT_HYPHEN_PATTERN.matcher(term).find();
        results.add(hasSoftHyphen ? "Please check" : "OK");
        
        boolean hasSpaceAround = term != null && SPACE_AROUND_SLASH_PATTERN.matcher(term).find();
        results.add(hasSpaceAround ? "Please check" : "OK");
        
        boolean hasApostropheNotAllowed = term != null && APOSTROPHE_PATTERN.matcher(term).find();
        results.add(hasApostropheNotAllowed ? "Please check" : "OK");

        
        if (languageCode.equals("de")) {
			//Specific checks for German language
        	boolean startsUpper = term != null && UPPERCASE_PATTERN.matcher(term).find();
        	results.add(startsUpper ? "OK" : "Please check");
            
		} else if (languageCode.equals("fr")) {
        	boolean startsUpper = term != null && UPPERCASE_PATTERN.matcher(term).find();
        	results.add(startsUpper ? "Please check" : "OK");
			
		} else if (languageCode.equals("it")) {
        	boolean startsUpper = term != null && UPPERCASE_PATTERN.matcher(term).find();
        	results.add(startsUpper ? "Please check" : "OK");
        	
		}  else {
			results.add("language code not recognized and therefore not checked"); // Default case for other languages
		}
        return results;
    }
}
