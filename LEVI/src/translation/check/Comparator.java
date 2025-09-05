package translation.check;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.stream.Collectors;

public class Comparator {
 //TODO:Methods need performance testing
	
	private ResultCollector resultCollector;
	private DbConnection dbConnection;
	private static Conf conf = new Conf();

	public Comparator(ResultCollector collector) {
	    this.resultCollector = collector;
	    this.dbConnection = new DbConnection(resultCollector);
	}

	

	public List<List<String>> createTranslationsOverview() throws IOException, ClassNotFoundException, SQLException {

		System.out.println("Creating overview of translations...");
		List<String> header = Arrays.asList("Concept ID", "GB/US FSN Term (For reference only)", "Status",
				"Preferred Term (For reference only)", "Translated Term DE", "Translated Term FR",
				"Translated Term IT");
		List<List<String>> structuredFile = new ArrayList<>();
		structuredFile.add(new ArrayList<>(header));

		// Use a Set to store unique concept IDs
		Set<String> conceptID = new HashSet<>();
		for (String conceptIDentry : resultCollector.getIdsByType("NEW_TRANSLATION_CURRENT")) { //TODO: Think of a better way to get all concept IDs, now it only works for translation addition from termspace
			conceptID.add(conceptIDentry);
		}

		dbConnection.getOverviewOfTranslationsDB(conceptID);

		// Structuring the file

		// Using HashMap to map language codes to column indices
		Map<String, Integer> languageColumnMap = new HashMap<>();
		languageColumnMap.put("en", 3);
		languageColumnMap.put("de", 4);
		languageColumnMap.put("fr", 5);
		languageColumnMap.put("it", 6);

		for (List<String> dbTerm : resultCollector.getDataByType("EXTENSION_TRANSLATION")) {

			List<String> entry = new ArrayList<>(Collections.nCopies(7, "TODO"));

			String conceptIDEntry = dbTerm.get(0);
			String status = dbTerm.get(1);
			String term = dbTerm.get(4);
			String languageCode = dbTerm.get(5).toLowerCase();
			String typeId = dbTerm.get(7);

			// Return index of the inner list of structuredFile. Now it knows on which index
			// the concept ID is.
			int indexOfSCTID = findInnerListIndex(structuredFile, conceptIDEntry);

			// If concept ID is not found, add a new row to structuredFile
			if (indexOfSCTID == -1 && structuredFile.size() > 0) {
				entry.set(0, conceptIDEntry); // Concept ID
				entry.set(2, status); // Status of concept
				if ("900000000000003001".equalsIgnoreCase(typeId)) {
					entry.set(1, term); // FSN Term
				} else {
					Integer langIndex = languageColumnMap.get(languageCode);
					if (langIndex != null) {
						entry.set(langIndex, term); // Translated Term based on language code
					}
				}
				structuredFile.add(entry);
			} else {
				// Update existing row if concept ID is found
				List<String> structuredFileElement = structuredFile.get(indexOfSCTID);
				boolean isUpdated = false;

				if ("900000000000003001".equalsIgnoreCase(typeId)) {
					structuredFileElement.set(1, term);
					isUpdated = true;
				} else {
					Integer langIndex = languageColumnMap.get(languageCode);
					if (langIndex != null) {
						String currentTranslation = structuredFileElement.get(langIndex);
						if ("TODO".equals(currentTranslation)) {
							structuredFileElement.set(langIndex, term);
							isUpdated = true;
						} else if (!currentTranslation.contains(term)) {
							structuredFileElement.set(langIndex, currentTranslation + " | " + term);
							isUpdated = true;
						}
					}
				}

				// If updated, set the modified element back to the list
				if (isUpdated) {
					structuredFile.set(indexOfSCTID, structuredFileElement);
				}
			}
		}

		System.out.println("Output data created.");

		return structuredFile;
	}

	public List<List<String>> generateDescriptionAdditionAndChangesDelta() throws IOException, SQLException, ClassNotFoundException {
		
		System.out.println("Starting with description addition delta...");
		String specificLanguage = null;
		
		@SuppressWarnings("resource")
		Scanner scanner = new Scanner(System.in);
		while (true) {
			    System.out.print("Is the import for a specific language? (de, fr, it) [press Enter for none]: ");
			    specificLanguage = scanner.nextLine().trim().toLowerCase();
	
			    if (specificLanguage.isEmpty()) {
			        specificLanguage = null; // oder "" je nach weiterer Verwendung
			        System.out.println("→ No specific language selected.");
			        break;
			    }
	
			    if (specificLanguage.equals("de") || specificLanguage.equals("fr") || specificLanguage.equals("it")) {
			        break;
			    } else {
			        System.out.println("Invalid language code. Please enter 'de', 'fr', or 'it', or press Enter for none.");
			    }
		}

		//////////////// Starting with translation additions
		// Step 1: Get all concept IDs from the resultCollector and fetch translations from the database
		List<String> headerAdditions = Arrays.asList("Concept ID", "GB/US FSN Term (For reference only)", "Preferred Term (For reference only)",
				"Translated Term", "Language Code", "Case significance", "TypeId", "Language reference set", "Acceptability", "Language reference set", "Acceptability", "Language reference set", "Acceptability", "Language reference set", "Acceptability", "Notes", "Quotes", "SoftHyphen", "UpperCase", "SpaceAroundSlash");
		
		Set<String> conceptID = new HashSet<>();
		for (String conceptIDentry : resultCollector.getIdsByType("NEW_TRANSLATION_CURRENT")) {
			conceptID.add(conceptIDentry);
		}

		System.out.println("Fetching translations from DB for " + conceptID.size() + " concept IDs...");		
		dbConnection.searchTranslations(conceptID); // Fetch translations from the database and populate oldTranslation
		System.out.println("Translations fetched. Starting comparison...");
		
		
		// Step 2: structure translation from DB according to ConceptId → Map with List<List<String>>
		Map<String, List<List<String>>> dbTranslationMap = new HashMap<>();
		for (List<String> oldEntry : resultCollector.getDataByType("EXTENSION_TRANSLATION")) {
		    String conceptId = oldEntry.get(0);
		    dbTranslationMap.computeIfAbsent(conceptId, k -> new ArrayList<>()).add(oldEntry);
		}
		
		// Step 3: prepare delta list
		List<List<String>> deltaTranslations = new ArrayList<>();
		deltaTranslations.add(headerAdditions); // Add header to delta list
		
		for (List<String> newEntry : resultCollector.getDataByType("NEW_TRANSLATION_CURRENT")) {
			
		    String conceptId   = newEntry.get(0);
		    String newTerm     = newEntry.get(3);
		    String newLangCode = newEntry.get(4);
		    String newTypeId   = newEntry.get(6);
		    String newLanguageRefset = newEntry.get(7);
		    String newAccept   = newEntry.get(8);
		    		    
		    if (specificLanguage != null && !specificLanguage.equalsIgnoreCase(newLangCode)) {
		        newLangCode = specificLanguage; // Set to the specific language if provided
		        newEntry.set(4, newLangCode); // Update the language code in the new entry
		    }
		    
		    if (conf.isTransformEszett() && "de".equalsIgnoreCase(newLangCode) && newTerm.contains("ß")) {
		        newTerm = newTerm.replace("ß", "ss");
		        newEntry.set(3, newTerm); // Update the term in the new entry
		    }

		    List<List<String>> oldEntriesForConcept = dbTranslationMap.getOrDefault(conceptId, Collections.emptyList());

		    boolean matchFound = false;

		    for (List<String> oldEntry : oldEntriesForConcept) {
		    	String oldTerm = oldEntry.get(4);
		        String oldLangCode = oldEntry.get(5);
		        String oldCaseSignificance = oldEntry.get(6);
		        String oldAccept = oldEntry.get(9);
		        String oldDescriptionId = oldEntry.get(10);
		        String oldDesccriptionStatus = oldEntry.get(11);
		      
		        if (!newTerm.isEmpty() && !oldTerm.isEmpty() 
		                && newTerm.equalsIgnoreCase(oldTerm) 
		                && !newLangCode.isEmpty() && !oldLangCode.isEmpty() 
		                && newLangCode.equalsIgnoreCase(oldLangCode)) {
		            matchFound = true;

		            if ("0".equals(oldDesccriptionStatus)) {
		                System.out.println("🔴 Reactivate translation: " + conceptId + " - " + newTerm);
		                
		                //TODO: Add entry to translation changes list for reactivation --> at the moment no reactivation via import file is possible
		            }		            
		            else if (!oldAccept.isEmpty() && !newAccept.isEmpty() 
		                    && !oldAccept.equalsIgnoreCase(newAccept)) {
		                System.out.println("🔁 Acceptability changed: " + conceptId + " - " + newTerm);
		                resultCollector.setFullTranslationChanges(
		                		oldDescriptionId, 
		                		"", //placeholder for preferred term
		                		newTerm,
		                		oldCaseSignificance,
		                		newTypeId,
		                		newLanguageRefset, newAccept, 
		                		"", //placeholder for language reference set 2
		                		"", //placeholder for acceptability 2
		                		"", //placeholder for language reference set 3
		                		"", //placeholder for acceptability 3
		                		"", //placeholder for language reference set 4
		                		"", //placeholder for acceptability 4
		                		"", //placeholder for language reference set 5
		                		"", //placeholder for acceptability 5
		                		"Acceptability changed from " + oldAccept + " to " + newAccept + " for concept " + conceptId
			                );
		            }
		            break;
		        }
		    }

		    if (!matchFound) {
		        List<String> copy = new ArrayList<>(newEntry);
		        
		        List<String> regexResults = RegexValidator.validateTerm(newLangCode, newTerm);
		        String quotesResult = regexResults.get(0);
		        String softHyphenResult =regexResults.get(1);
		        String upperCaseResult = regexResults.get(2);
		        String spaceAroundSlashResult = regexResults.get(3);
	        	
		        copy.add(16, quotesResult); // Add "Quotes" result
		        copy.add(17, softHyphenResult);
		        copy.add(18, upperCaseResult); // Add "UpperCase" result
		        copy.add(19, spaceAroundSlashResult); // Add "SpaceAroundSlash" result
		       
		        deltaTranslations.add(copy);
		    }
		}
		System.out.println("Delta translations created with " + deltaTranslations.size() + " entries.");
		return deltaTranslations;
	}

	public List<List<String>> generateDescriptionInactivationDelta() throws IOException, SQLException, ClassNotFoundException {

		System.out.println("Starting with description inactivation delta...");
		List<String> headerInactivation = Arrays.asList("Description ID","Language Code", "Concept ID", "Preferred Term (For reference only)", "Term (For reference only)", "Inactivation Reason", "Association Target ID 1",
				"Association Target ID 2", "Association Target ID 3", "Association Target ID 4", "Notes");
		List<List<String>> deltaInactivations = new ArrayList<>();
		deltaInactivations.add(headerInactivation);
		
		
		if(conf.isTransformEszett()) {
			for (List<String> row : resultCollector.getDataByType("TRANSLATION_INACTIVATION_CURRENT")) {
				String term = row.get(1);
				String languageCode = row.get(2);
							
				if ("de".equalsIgnoreCase(languageCode) && term.contains("ß")) {
				       term = term.replace("ß", "ss");
				       row.set(1, term); // Update the term in the new entry
				   }
			}
		}
		System.out.println("Fetching translations from DB ");		
		dbConnection.searchDescriptions(resultCollector.getDataByType("TRANSLATION_INACTIVATION_CURRENT")); // Fetch descriptions from the database and populate oldTranslation
		System.out.println("Translations fetched. Starting comparison...");
		
		for (List<String> newEntry : resultCollector.getDataByType("EXTENSION_INACTIVATION")) {
			deltaInactivations.add(newEntry);
		}
		System.out.println("Delta inactivations created with " + deltaInactivations.size() + " entries.");	
		return deltaInactivations;
	}
	
	public List<List<String>> generateDescriptionChangesDelta() throws IOException, SQLException, ClassNotFoundException {
		List<String> headerChanges= Arrays.asList("Description ID", "Preferred Term (For reference only)", "Term (For reference only)",
				"Case significance","Type","Language reference set","Acceptability","Language reference set","Acceptability",
				"Language reference set","Acceptability","Language reference set","Acceptability","Language reference set",
				"Acceptability","Notes");
		
		List<List<String>> deltaChanges = new ArrayList<>();
		deltaChanges.add(headerChanges);
		
		for (List<String> newEntry : resultCollector.getDataByType("TRANSLATION_CHANGES")) {
	        deltaChanges.add(newEntry);
		}	
		return deltaChanges;
	}

	public List<List<List<String>>> checkEszettInExtension() throws ClassNotFoundException, UnsupportedEncodingException, SQLException {
		dbConnection.searchEszett();
		
		List<List<String>> eszettInactivate = new ArrayList<>();
		List<List<String>> eszettAdditions = new ArrayList<>();
		
		List<String> headerInactivate = Arrays.asList("Description ID", "Language Code", "Concept ID", "Preferred Term (For reference only)", "Term (For reference only)", "Inactivation Reason", "Association Target ID 1",
				"Association Target ID 2", "Association Target ID 3", "Association Target ID 4", "Notes");
		
		List<String> headerAddition = Arrays.asList("Concept ID", "GB/US FSN Term (For reference only)", "Preferred Term (For reference only)",
				"Translated Term", "Language Code", "Case significance", "TypeId", "Language reference set", "Acceptability", "Language reference set", "Acceptability", "Language reference set", "Acceptability", "Language reference set", "Acceptability", "Notes", "Quotes", "SoftHyphen", "UpperCase", "SpaceAroundSlash");
		
		eszettInactivate.add(headerInactivate);
		eszettAdditions.add(headerAddition);
		
		for (List<String> row : resultCollector.getDataByType("EXTENSION_TRANSLATION")) {
			String term = row.get(4);
			String languageCode = row.get(5).toLowerCase();
			String descriptionId = row.get(10);
			String conceptId = row.get(0);
			String caseSignificance = row.get(6);
			String typeId = row.get(7);
			String languageReferenceSet = row.get(8);
			String acceptability = row.get(9);
			String placeholder ="";
			
			List<String> inactivationRow = new ArrayList<>();
			
			inactivationRow.add(descriptionId);
			inactivationRow.add(languageCode);
			inactivationRow.add(conceptId);
			inactivationRow.add(placeholder); // Preferred Term (For reference only)
			inactivationRow.add(term);
			inactivationRow.add(placeholder); // Inactivation Reason
			inactivationRow.add(placeholder); // Association Target ID 1
			inactivationRow.add(placeholder); // Association Target ID 2
			inactivationRow.add(placeholder); // Association Target ID 3
			inactivationRow.add(placeholder); // Association Target ID 4
			inactivationRow.add(placeholder); // Notes
			
			eszettInactivate.add(inactivationRow);
			//////////////////

			List<String> additionRow = new ArrayList<>();
			if (term.contains("ß")) {
				term = term.replace("ß", "ss");
			  }
			additionRow.add(conceptId);
			additionRow.add(placeholder); // GB/US FSN Term (For reference only)
			additionRow.add(placeholder); // Preferred Term (For reference only)
			additionRow.add(term); // Translated Term
			additionRow.add(languageCode); // Language Code
			additionRow.add(caseSignificance); // Case significance
			additionRow.add(typeId); // TypeId
			additionRow.add(languageReferenceSet); // Language reference set
			additionRow.add(acceptability); // Acceptability
			additionRow.add(placeholder); // Language reference set 2
			additionRow.add(placeholder); // Acceptability 2
			additionRow.add(placeholder); // Language reference set 3
			additionRow.add(placeholder); // Acceptability 3
			additionRow.add(placeholder); // Language reference set 4
			additionRow.add(placeholder); // Acceptability 4
			additionRow.add(placeholder); // Notes
			
			eszettAdditions.add(additionRow);	
		}
		
		List<List<List<String>>> result = new ArrayList<>();
	    result.add(eszettInactivate);
	    result.add(eszettAdditions);
		
		return result;
		
	}
	
	public List<List<String>> generateDeltaOfNotPublishedTranslations() throws IOException, SQLException, ClassNotFoundException {
		System.out.println("Starting delta of not published translations...");
		List<String> headerInactivation = Arrays.asList("Description ID","Language Code", "Concept ID", "Preferred Term (For reference only)", "Term (For reference only)", "Inactivation Reason", "Association Target ID 1",
				"Association Target ID 2", "Association Target ID 3", "Association Target ID 4", "Notes");
		List<List<String>> deltaNotFoundTranslations = new ArrayList<>();
		deltaNotFoundTranslations.add(headerInactivation);
		
		List<List<String>> previousEntries = resultCollector.getDataByType("NEW_TRANSLATION_PREVIOUS");
        List<List<String>> currentEntries = resultCollector.getDataByType("NEW_TRANSLATION_CURRENT");
        List<List<String>> currentInactivationEntries = resultCollector.getDataByType("TRANSLATION_INACTIVATION_CURRENT");
		
        
        // Create sets of concept IDs for quick lookup
        List<String> currentConceptIds = currentEntries.stream().map(entry -> entry.get(0)).collect(Collectors.toList());
        List<String> inactivationConceptIds = currentInactivationEntries.stream().map(entry -> entry.get(3)).collect(Collectors.toList());

        // Process each entry in NEW_TRANSLATION_PREVIOUS
        for (List<String> previousEntry : previousEntries) {
            String conceptId = previousEntry.get(0);
            String languageCode = previousEntry.get(4);
            String term = previousEntry.get(3);

            // Check if the concept ID exists in either NEW_TRANSLATION_CURRENT or TRANSLATION_INACTIVATION_CURRENT
            if (!currentConceptIds.contains(conceptId) && !inactivationConceptIds.contains(conceptId)) {
                
                List<String> formattedEntry = new ArrayList<>();
                formattedEntry.add(""); // Description ID
                formattedEntry.add(languageCode); // Language Code
                formattedEntry.add(conceptId); // Concept ID
                formattedEntry.add(""); // FSN
                formattedEntry.add(term); // Term
                formattedEntry.add(""); // Inactivation Reason
                formattedEntry.add(""); // Association Target ID 1
                formattedEntry.add(""); // Association Target ID 2
                formattedEntry.add(""); // Association Target ID 3
                formattedEntry.add(""); // Association Target ID 4
                formattedEntry.add(""); // Notes

                // Add the formatted entry to the delta list
                deltaNotFoundTranslations.add(formattedEntry);
            }
        }

        return deltaNotFoundTranslations;
	}
	
	
	// Used to find the index of the concept that is already in the structuredList.
	private static int findInnerListIndex(List<List<String>> outerList, String element) {
		for (int i = 0; i < outerList.size(); i++) {
			List<String> innerList = outerList.get(i);
			if (innerList.contains(element)) {
				return i; // Return the index of the inner list
			}
		}
		return -1; // Return -1 if the element is not found in any inner list
	}





}
