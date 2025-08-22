package translation.check;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

public class CompareManager {

	private final ResultCollector resultCollector = new ResultCollector();
	private final Comparator comparator = new Comparator(resultCollector);
	private final FileReaderUtil reader = new FileReaderUtil(resultCollector);
	private final FileWriterUtil writer = new FileWriterUtil();

	public void runTranslationOverview(String path, String destination)
			throws IOException, ClassNotFoundException, SQLException {
		reader.readFile(path);
		writer.writeToFile(destination + "\\TranslationOverview.tsv", comparator.createTranslationsOverview());
	}

	public void runDeltaDescAdditions(String path, String destination)
			throws IOException, ClassNotFoundException, SQLException {
		reader.readFile(path);
		writer.writeToFile(destination + "\\DeltaDescAdditions.tsv", comparator.generateDescriptionAdditionAndChangesDelta());
		if(resultCollector.containsType("TRANSLATION_CHANGES")) {
			writer.writeToFile(destination + "\\DeltaDescChanges.tsv", comparator.generateDescriptionChangesDelta());
		}
	}
	
	public void runDeltaDescInactivations(String path, String destination) throws ClassNotFoundException, IOException, SQLException {
		reader.readFile(path);
		writer.writeToFile(destination + "\\DeltaDescInactivations.tsv", comparator.generateDescriptionInactivationDelta());
	}
	
	public void runGenerateDelta(String path, String destination) throws ClassNotFoundException, IOException, SQLException {
		reader.readFile(path);
		
		writer.writeToFile(destination + "\\DeltaDescAdditions.tsv", comparator.generateDescriptionAdditionAndChangesDelta());
		
		if(resultCollector.containsType("TRANSLATION_CHANGES")) {
			writer.writeToFile(destination + "\\DeltaDescChanges.tsv", comparator.generateDescriptionChangesDelta());
		}
		
		writer.writeToFile(destination + "\\DeltaDescInactivations.tsv", comparator.generateDescriptionInactivationDelta());
	}
	
	public void runCheckEszettInExtension(String destination) throws ClassNotFoundException, IOException, SQLException {
		comparator.checkEszettInExtension();
		String fileName = "\\EszettInactivations.tsv";
		int i = 0;
		
		for (List<List<String>> entry : comparator.checkEszettInExtension()) {
			if (i > 0) {
				fileName = "\\EszettAdditions.tsv";
			}
			writer.writeToFile(destination + fileName, entry);
			i++;
		}
	}

	public void runDeltaNotPublishedTranslations (String pathCurrent, String pathPrevious, String destination) throws IOException, ClassNotFoundException, SQLException {
		reader.readFile(pathCurrent);
		reader.readFile(pathPrevious);
		
		writer.writeToFile(destination + "\\DeltaNotPublishedTranslations.tsv", comparator.generateDeltaOfNotPublishedTranslations());
		
	}
}
