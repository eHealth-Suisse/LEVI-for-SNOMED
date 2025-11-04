package translation.check;

import java.util.Scanner;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;

public class DescriptionInactivationLoader {
	
	//TODO: Add check if the file is empty or has no rows

	public void loadAndInsertExcel(Sheet sheet, ResultCollector collector, String releaseType) {
		Row header = sheet.getRow(0);
		int rowCount = sheet.getPhysicalNumberOfRows();
        boolean hasLanguageCode = false;
        int languageCodeColumnIndex = -1;
        String language = null;

        
        if (header != null) {
            for (Cell cell : header) {
                String headerValue = getCellAsString(cell);
                if ("Language Code (require if the term is specified)".equalsIgnoreCase(headerValue)) {
                    hasLanguageCode = true;
                    languageCodeColumnIndex = cell.getColumnIndex();
                    break;
                }
            }
        }
        
        // If no 'Language Code' column is found, prompt the user for input
        if (!hasLanguageCode) {
            @SuppressWarnings("resource")
			Scanner scanner = new Scanner(System.in);
            while (true) {
                System.out.print("No 'Language Code' column found in inactivation tab. Please enter the language code (de, fr, it): ");
                language = scanner.nextLine().trim().toLowerCase();
                if (language.equals("de") || language.equals("fr") || language.equals("it")) {
                    break;
                } else {
                    System.out.println("Invalid language code. Please enter 'de', 'fr', or 'it'.");
                }
            }
        }


        for (int i = 1; i < rowCount; i++) { // skip header
            Row row = sheet.getRow(i);
            if (row == null) continue;
            
//            System.out.println("Size of row " + row.getRowNum() + ": " + row.getPhysicalNumberOfCells());

            String descriptionId = getCellAsString(row.getCell(0));
			String term = getCellAsString(row.getCell(4));
			String conceptId = getCellAsString(row.getCell(2));
			String lang = null;
			
			//if language code is present, use from the cell
			if (language != null) {
				lang = language;
			} else {
				System.out.println("There is a language code tab in the header, but no language code was found in the file. Please check the file.");
				System.exit(0);
			}
			
	        if (hasLanguageCode && languageCodeColumnIndex >= 0) {
	            String dynamicLang = getCellAsString(row.getCell(languageCodeColumnIndex));
	            if (dynamicLang != null && !dynamicLang.isEmpty()) {
	                lang = dynamicLang.toLowerCase();
	            }
	        }

            if(releaseType.equals("previous")) {
            	collector.setFullInactivationsPrevious(descriptionId, term, lang, conceptId);
			} else {
				collector.setFullInactivationsCurrent(descriptionId, term, lang, conceptId);
			}   
        }
    }


 
    private String getCellAsString(Cell cell) {
	    if (cell == null) {
	        return "";
	    }
	    switch (cell.getCellType()) {
	        case STRING:
	            return cell.getStringCellValue();
	        case NUMERIC:
	            return String.valueOf(cell.getNumericCellValue());
	        case BOOLEAN:
	            return String.valueOf(cell.getBooleanCellValue());
	        case FORMULA:
	            return cell.getCellFormula();
	        default:
	            return "";
	    }
	}	
	
}
