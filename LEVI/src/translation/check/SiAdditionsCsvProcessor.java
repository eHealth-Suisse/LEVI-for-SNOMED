package translation.check;

import java.io.IOException;
import java.util.List;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;

public class SiAdditionsCsvProcessor extends CsvProcessor{
	
	private ResultCollector collector;
	
	public SiAdditionsCsvProcessor(CSVReader csvReader, ResultCollector collector) {
        super(csvReader);
		this.collector = collector;
    }
	
	 @Override
	    public void process() throws IOException {
	        List<String[]> rows = null;
			try {
				rows = csvReader.readAll();
			} catch (IOException | CsvException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			boolean isFirstRow = true;
	        for (String[] row : rows) {
	        	
	        	if (isFirstRow) {
	        		isFirstRow = false;
	        		continue; // Skip the header row
	        	}
	        	String conceptId = row[0];
	        	String fsn = row[1];
	        	String pt = row[2];
	        	String term = row[3];
	        	String languageCode = row[4];
	        	String caseSignificance = row[5];
	        	String type = row[6];
	        	String language_reference_set = row[7];
	        	String acceptabilityId = row[8];
	        	String language_reference_set2 = row[9];
	        	String acceptabilityId2 = row[10];
	        	String language_reference_set3 = row[11];
	        	String acceptabilityId3 = row[12];
	        	String language_reference_set4 = row[13];
	        	String acceptabilityId4 = row[14];
	        	String language_reference_set5 = row[15];
	        	String acceptabilityId5 = row[16];
	        	String notes = row[17];
	        	
	        	collector.setFullNewTranslationCurrent(
		        		conceptId, fsn, pt, term, languageCode, 
		        		caseSignificance, type,
		        		language_reference_set, acceptabilityId,
		        		language_reference_set2, acceptabilityId2,
		        		language_reference_set3, acceptabilityId3,
		        		language_reference_set4, acceptabilityId4,
		        		language_reference_set5, acceptabilityId5,
		        		notes
		        		);
	        }
	    }

}
