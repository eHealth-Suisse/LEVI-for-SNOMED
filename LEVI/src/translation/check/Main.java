package translation.check;

/**
 * Executes the all necessary classes to compare the terms of the CSV with the
 * SNOMED database.
 * 
 * @author Pero Grgic
 *
 */
public class Main {
	
	public static void main(String[] args) throws Exception {
		
		Conf conf = new Conf();


        String task = null;

        for (String arg : args) {
            if (arg.startsWith("--country="))           conf.setCountryCode(arg.substring(10));
            if (arg.startsWith("--transformEszett="))   conf.setTransformEszett(arg.substring(18).equalsIgnoreCase("true"));
            if (arg.startsWith("--regex="))             conf.setRegexCheck(arg.substring(8).equalsIgnoreCase("true"));
            if (arg.startsWith("--current="))           conf.setFilePathCurrent(arg.substring(10));
            if (arg.startsWith("--previous="))          conf.setFilePathPrevious(arg.substring(11));
            if (arg.startsWith("--dest="))              conf.setDestination(arg.substring(7));
            if (arg.startsWith("--task="))              task = arg.substring(7);
            if (arg.startsWith("--dbUrl="))            conf.setDbUrl(arg.substring(8));
            if (arg.startsWith("--dbUser="))           conf.setDbUsername(arg.substring(9));
            if (arg.startsWith("--dbPassword="))       conf.setDbPassword(arg.substring(13));
        }

        CompareManager compareManager = new CompareManager(conf);

        if (task == null) return;

        switch (task) {
            case "desc-inact":       compareManager.runDeltaDescInactivations(conf.getFilePathCurrent(), conf.getDestination()); break;
            case "desc-add":         compareManager.runDeltaDescAdditions(conf.getFilePathCurrent(), conf.getDestination()); break;
            case "overview":         compareManager.runTranslationOverview(conf.getFilePathCurrent(), conf.getDestination()); break;
            case "translate-delta":  compareManager.runGenerateDelta(conf.getFilePathCurrent(), conf.getDestination()); break;
            case "eszett-check":     compareManager.runCheckEszettInExtension(conf.getDestination()); break;
            case "not-published":    compareManager.runDeltaNotPublishedTranslations(conf.getFilePathCurrent(), conf.getFilePathPrevious(), conf.getDestination()); break;
        }

        //If user want to run via Eclipse without arguments
        // Uncomment the following lines and set the parameters in the Conf class above
//		compareManager.runTranslationOverview(conf.getFilePathCurrent(), conf.getDestination());
//		compareManager.runDeltaDescAdditions(conf.getFilePathCurrent(), conf.getDestination());
//		compareManager.runDeltaDescInactivations(conf.getFilePathCurrent(), conf.getDestination());
//		compareManager.runGenerateDelta(conf.getFilePathCurrent(), conf.getDestination());
//		compareManager.runCheckEszettInExtension(conf.getDestination());
//		compareManager.runDeltaNotPublishedTranslations(conf.getFilePathCurrent(), conf.getFilePathPrevious(), conf.getDestination());
	}
}