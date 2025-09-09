package translation.check;

import java.io.IOException;
import java.util.List;
import java.util.Scanner;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;

public class TermspaceInactivationsCsvProcessor extends CsvProcessor{
	
	private ResultCollector collector;
	
	public TermspaceInactivationsCsvProcessor(CSVReader csvReader, ResultCollector collector) {
        super(csvReader);
		this.collector = collector;
    }
	
	 @Override
	    public void process() throws IOException {
	        List<String[]> rows = null;
	        boolean isFirstRow = true;
	        String languageCode = null;
	        
	        @SuppressWarnings("resource")
			Scanner scanner = new Scanner(System.in);
            while (true) {
                System.out.print("No 'Language Code' column in Termspace inactivation CSV. Please enter the language code (de, fr, it): ");
                languageCode = scanner.nextLine().trim().toLowerCase();
                if (languageCode.equals("de") || languageCode.equals("fr") || languageCode.equals("it")) {
                    break;
                } else {
                    System.out.println("Invalid language code. Please enter 'de', 'fr', or 'it'.");
                }
            }
			try {
				rows = csvReader.readAll();
			} catch (IOException | CsvException e) {
				e.printStackTrace();
			}
	        for (String[] row : rows) {
	        	if (isFirstRow) {
	        		isFirstRow = false;
	        		continue;
	        	}
	        	
	        	String descriptionId = row[0];
	        	String term = row[2];
	        	String conceptId = row[9];
	        	
	        	collector.setFullInactivationsCurrent(
		        		descriptionId, term, languageCode, conceptId);
	        }
	    }

}
