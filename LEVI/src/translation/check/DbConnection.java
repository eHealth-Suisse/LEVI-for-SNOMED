package translation.check;

import java.sql.*;
import java.util.*;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;


import java.io.UnsupportedEncodingException;

/**
 * The DB_connection class facilitates interactions with the SNOMED CT database,
 * enabling retrieval and processing of translations in various languages. It
 * supports querying concepts, descriptions, and translations while managing
 * database connections efficiently.
 * 
 * <p>
 * This class provides methods to:
 * <ul>
 * <li>Connect and disconnect from the database.</li>
 * <li>Search translations by language.</li>
 * <li>Handle specific linguistic features, such as terms containing "ß".</li>
 * <li>Batch-process large sets of IDs for efficient querying.</li>
 * </ul>
 * 
 * <p>
 * Author: Pero Grgic
 * </p>
 */
public class DbConnection {

	// JDBC driver and connection settings
	private Connection connection;
	private ResultCollector resultCollector;
    private final Conf conf;
	
	public DbConnection(ResultCollector collector, Conf conf) {
        this.resultCollector = collector;
        this.conf = conf;
	}
	

	/**
	 * Opens a new database connection.
	 *
	 * @throws SQLException           If a database access error occurs.
	 * @throws ClassNotFoundException If the JDBC driver class is not found.
	 */
	public void connect() throws SQLException, ClassNotFoundException {
//		Class.forName(JDBC_DRIVER);
		try {
			connection = DriverManager.getConnection(conf.getSERVER_URL(), conf.getUSERNAME(), conf.getPASSWORD());
		} catch (SQLException e) {
			System.err.println("Failed to establish connection: " + e.getMessage());
		}
	}

	/**
	 * Closes the active database connection.
	 */
	public void disconnect() {
		if (connection != null) {
			try {
				connection.close();
			} catch (SQLException e) {
				System.err.println("Failed to close connection: " + e.getMessage());
			}
		}
	}

	/**
	 * Searches for translations in the specified language, processes the results,
	 * and populates the `translated` list in the Compare class.
	 * 
	 * @param language the language code for translations (e.g., "de", "fr", "it")
	 * @throws SQLException                 if a database access error occurs
	 * @throws UnsupportedEncodingException if character encoding issues occur
	 * @throws ClassNotFoundException       if the JDBC driver is not found
	 * @throws IllegalArgumentException     if an unsupported language code is
	 *                                      provided
	 */
	public void searchTranslations(Set<String> conceptIDs) //TODO: Improve so it only returns entries from active concepts
			throws SQLException, UnsupportedEncodingException, ClassNotFoundException {
		connect();

		String baseQuery = """
				    SELECT
				        d.id,
				        d.conceptId,
				        d.term,
				        d.languageCode,
				        d.typeId,
				        d.caseSignificanceId,
				        d.effectiveTime,
				        d.active AS descriptionActive,
				        c.active AS conceptActive,
				        l.acceptabilityId
				    FROM
				        full_description d
				    LEFT JOIN (
				        SELECT
				            fc1.*
				        FROM
				            full_concept fc1
				        INNER JOIN (
				            SELECT
				                id,
				                MAX(effectiveTime) AS max_time
				            FROM
				                full_concept
				            GROUP BY
				                id
				        ) latest
				        ON fc1.id = latest.id
				        AND fc1.effectiveTime = latest.max_time
				    ) c
				    ON d.conceptId = c.id
				    LEFT JOIN
				        full_refset_Language l
				        ON d.id = l.referencedComponentId
				    WHERE
				        d.conceptId
				""";
		// Generate batched queries for concept IDs
		List<String> batchedQueries = buildBatchedQueries(conceptIDs, baseQuery);

		for (String query : batchedQueries) {
			try (PreparedStatement stmt = connection.prepareStatement(query)) {
				try (ResultSet rs = stmt.executeQuery()) {
					processTranslationResultSet("additions",rs);
				}
			}
		}
		disconnect();
	}

	/**
	 * Retrieves an overview of translations for a set of concept IDs, processing
	 * the results into the TranslationOverview list.
	 * 
	 * @param conceptIDs a set of concept IDs to retrieve translations for
	 * @throws SQLException                 if a database access error occurs
	 * @throws UnsupportedEncodingException if character encoding issues occur
	 * @throws ClassNotFoundException
	 */
	public void getOverviewOfTranslationsDB(Set<String> conceptIDs)
			throws SQLException, ClassNotFoundException, UnsupportedEncodingException {
		connect();
		// Base query for retrieving translation information
		String baseQuery = """
				    SELECT
				        fc.active AS conceptActive,
				        fd.conceptId,
				        fd.typeId,
				        fd.term,
				        fd.languageCode
				    FROM
				        full_concept fc
				    JOIN (
				        SELECT
				            id,
				            MAX(effectiveTime) AS max_time
				        FROM
				            full_concept
				        GROUP BY
				            id
				    ) latest
				        ON fc.id = latest.id
				        AND fc.effectiveTime = latest.max_time
				    JOIN
				        full_description fd
				        ON fc.id = fd.conceptId
				    WHERE
				        fd.active = 1
				    AND
				        fc.id
				""";

		// Generate batched queries for concept IDs
		List<String> batchedQueries = buildBatchedQueries(conceptIDs, baseQuery);

		for (String query : batchedQueries) {
			try (Statement stmt = connection.createStatement(); ResultSet rs = stmt.executeQuery(query)) {
				// Process the result set and add entries to TranslationOverview
				processTranslationResultSet("overview", rs);
			}
		}
		disconnect();
	}

	/**
	 * Searches for descriptions by ID, populating the Compare class with
	 * information about newly found and missing descriptions.
	 * 
	 * @param descriptionIds a set of description IDs and terms to search for
	 * @throws SQLException           if a database access error occurs
	 * @throws ClassNotFoundException if the JDBC driver is not found
	 */	
	public void searchDescriptions(List<List<String>> newInactivation) throws SQLException, ClassNotFoundException {
	    List<Triple<String, String, String>> termConceptPairs = new ArrayList<>();

	    for (List<String> row : newInactivation) {
	        String term = row.get(1);              // exact term
	        String conceptId = row.get(3);
	        String languageCode = row.get(2);      // language code
	        Triple<String, String, String> pair = Triple.of(term, conceptId, languageCode);
	        termConceptPairs.add(pair);
	    }

	    connect();

	    // create TEMP TABLE with languageCode
	    String createTempTable = """
	        CREATE TEMPORARY TABLE IF NOT EXISTS tmp_pairs (
	            term VARBINARY(255),
	            conceptId VARBINARY(50),
	            languageCode VARBINARY(10)
	        )
	    """;

	    try (Statement stmt = connection.createStatement()) {
	        stmt.execute(createTempTable);
	    }

	    // insert pairs
	    String insertPair = "INSERT INTO tmp_pairs (term, conceptId, languageCode) VALUES (?, ?, ?)";
	    try (PreparedStatement ps = connection.prepareStatement(insertPair)) {
	        for (Triple<String, String, String> pair : termConceptPairs) {
	            String conceptId = pair.getMiddle();
	            if (conceptId.length() > 30) {
	                System.out.println("❗ conceptId too long: " + conceptId + " (length: " + conceptId.length() + ")");
	            }
	            ps.setString(1, pair.getLeft());
	            ps.setString(2, pair.getMiddle());
	            ps.setString(3, pair.getRight());
	            ps.addBatch();
	        }
	        ps.executeBatch();
	    }

	    System.out.println("Temporary table tmp_pairs created and populated with term-concept-language pairs.");

	    String query = """
	        SELECT fd.id, fd.term, fd.conceptId, fd.active, fd.languageCode
	        FROM full_description fd
	        INNER JOIN (
	            SELECT conceptId, term, languageCode, MAX(CAST(effectiveTime AS UNSIGNED)) AS max_effectiveTime
	            FROM full_description
	            GROUP BY conceptId, term, languageCode
	        ) latest
	          ON fd.conceptId = latest.conceptId
	         AND fd.term = latest.term
	         AND fd.languageCode = latest.languageCode
	         AND CAST(fd.effectiveTime AS UNSIGNED) = latest.max_effectiveTime
	        INNER JOIN tmp_pairs tp
	          ON fd.conceptId = tp.conceptId
	         AND fd.term = tp.term
	         AND fd.languageCode = tp.languageCode
	        WHERE fd.active = 1
	    """;

	    try (PreparedStatement ps = connection.prepareStatement(query);
	         ResultSet rs = ps.executeQuery()) {
	        System.out.println("Processing translation result set for inactivations...");
	        processTranslationResultSet("inactivations", rs);
	    }

	    disconnect();
	}

	/**
	 * Retrieves and processes concepts containing the character "ß" in their German
	 * translations, adding them to the `EszettList` in the Compare class.
	 * 
	 * @throws SQLException                 if a database access error occurs
	 * @throws ClassNotFoundException       if the JDBC driver is not found
	 * @throws UnsupportedEncodingException if character encoding issues occur
	 */
	public void searchEszett() throws SQLException, ClassNotFoundException, UnsupportedEncodingException {

		connect();
		String query = """
			    SELECT
			        fd.id,
			        fd.active AS descriptionActive,
			        fd.conceptId,
			        fd.typeId,
			        fd.term,
			        fd.caseSignificanceId,
			        fr.acceptabilityId
			    FROM full_description fd
			    INNER JOIN (
			        SELECT
			            conceptId,
			            term,
			            languageCode,
			            MAX(CAST(effectiveTime AS UNSIGNED)) AS max_effectiveTime
			        FROM full_description
			        GROUP BY conceptId, term, languageCode
			    ) latest
			      ON fd.conceptId = latest.conceptId
			     AND fd.term = latest.term
			     AND fd.languageCode = latest.languageCode
			     AND CAST(fd.effectiveTime AS UNSIGNED) = latest.max_effectiveTime
			    INNER JOIN full_refset_Language fr
			      ON fd.id = fr.referencedComponentId
			    WHERE fd.languageCode = 'de'
			      AND fd.term REGEXP 'ß'
			      AND fd.active = 1
			""";
		
		System.out.println("Eszett check: Starting with query...");
		try (Statement stmt = connection.createStatement(); ResultSet rs = stmt.executeQuery(query)) {		
			processTranslationResultSet("searchEszett", rs);
		}
		disconnect();
	}

	/**
	 * Constructs batched SQL queries for a set of IDs, optimizing query execution
	 * by dividing large sets into manageable chunks.
	 * 
	 * @param ids       a set of IDs to query (e.g., concept IDs or description IDs)
	 * @param baseQuery the base SQL query without filtering (e.g., "SELECT ...
	 *                  WHERE")
	 * @param type      the type of ID to query ("conceptId" or "descriptionId")
	 * @return a list of SQL query strings, each containing up to `batchSize` IDs
	 * @throws IllegalArgumentException if an invalid `idType` is provided
	 */
	public static List<String> buildBatchedQueries(Set<String> ids, String baseQuery) {
		List<String> queries = new ArrayList<>();
		int batchSize = 500; // Adjust based on DB limitations
		List<String> batch = new ArrayList<>(batchSize);

		// Create query batches based on the ID type
		for (String id : ids) {
			batch.add("'" + id + "'");
			if (batch.size() == batchSize) {
				// Use the idType (conceptId or descriptionId) in the IN clause
				queries.add(baseQuery + " IN (" + String.join(",", batch) + ")");
				batch.clear();
			}
		}

		// Add remaining IDs in the last batch if any
		if (!batch.isEmpty()) {
			queries.add(baseQuery + " IN (" + String.join(",", batch) + ")");
		}

		return queries;
	}

	public static List<String> buildBatchedTermConceptIdQueries(Set<Pair<String, String>> termConceptPairs,
			String baseQuery) {
		List<String> queries = new ArrayList<>();
		int batchSize = 500;
		List<String> conditions = new ArrayList<>(batchSize);

		for (Pair<String, String> pair : termConceptPairs) {
			String condition = "(term = '" + pair.getLeft() + "' AND conceptId = '" + pair.getRight() + "')";
			conditions.add(condition);

			if (conditions.size() == batchSize) {
				queries.add(baseQuery + String.join(" OR ", conditions));
				conditions.clear();
			}
		}

		if (!conditions.isEmpty()) {
			queries.add(baseQuery + String.join(" OR ", conditions));
		}

		return queries;
	}

	/**
	 * Processes the given `ResultSet` to extract translation data and populates the
	 * old translation data for concepts.
	 * 
	 * This method iterates through the rows of the provided `ResultSet` and
	 * retrieves relevant fields such as `conceptId`, `term`, `languageCode`,
	 * `caseSignificanceId`, and `acceptabilityId`. The extracted data is passed to
	 * the `CONCEPT.setOldTranslation` method to populate the old translation
	 * entries for a concept.
	 * 
	 * @param rs The `ResultSet` object containing translation data retrieved from
	 *           the database. Expected fields in the result set include: -
	 *           `conceptId` (String): The unique identifier for the concept. -
	 *           `term` (String): The term associated with the concept, which will
	 *           be decoded before processing. - `languageCode` (String): The
	 *           language code of the term. - `caseSignificanceId` (String):
	 *           Specifies whether the term is case-sensitive. - `acceptabilityId`
	 *           (String): Indicates the acceptability of the term in the given
	 *           language.
	 * 
	 * @throws SQLException If an error occurs while accessing the `ResultSet`
	 *                      object.
	 */
	private void processTranslationResultSet(String resultSetType, ResultSet rs) throws SQLException {
				
		ResultSetMetaData metaData = rs.getMetaData();
		int columnCount = metaData.getColumnCount();
		Set<String> availableColumns = new HashSet<>();

		for (int i = 1; i <= columnCount; i++) {
			availableColumns.add(metaData.getColumnLabel(i).toLowerCase());
		}

		while (rs.next()) {
	        String descriptionId       = getSafe(rs, "id", availableColumns);
	        String conceptId           = getSafe(rs, "conceptId", availableColumns);
	        String conceptStatus       = getSafe(rs, "conceptActive", availableColumns);
	        String descriptionStatus   = getSafe(rs, "descriptionActive", availableColumns);
	        String term                = getSafe(rs, "term", availableColumns);
	        String languageCode        = getSafe(rs, "languageCode", availableColumns);
	        String caseSignificance    = getSafe(rs, "caseSignificanceId", availableColumns);
	        String type                = getSafe(rs, "typeId", availableColumns);
	        String acceptabilityId     = getSafe(rs, "acceptabilityId", availableColumns);
	        String languageReferenceSet = conf.getLanguageRefSetId(languageCode);
	        
	        switch (resultSetType) {
	            case "additions":
	                resultCollector.setFullExtensionTranslation(
	                        conceptId, conceptStatus, "", "", term,
	                        languageCode, caseSignificance, type,
	                        languageReferenceSet, acceptabilityId,
	                        descriptionId, descriptionStatus
	                );
	                break;

	            case "overview":
					resultCollector.setFullExtensionTranslation(conceptId, conceptStatus,
							"", // FSN placeholder
							"", // Preferred Term placeholder
							term, languageCode, caseSignificance,
							type, // Type placeholder
							languageReferenceSet, // Language Reference Set placeholder
							acceptabilityId, // Acceptability ID placeholder
							descriptionId, // Description ID placeholder
							descriptionStatus); // Description Status placeholder
	            	
	            	resultCollector.setFullTranslationOverview(
	                        conceptId, term, type, languageCode, conceptStatus
	                );
	                break;

	            case "inactivations":
	                resultCollector.setFullExtensionInactivations(
	                        descriptionId, languageCode, conceptId,
	                        "", // Preferred Term placeholder
	                        term, 
	                        "", // Inactivation Reason placeholder
	                        "", // Association Target ID 1 placeholder
	                        "", // Association Target ID 2 placeholder
	                        "", // Association Target ID 3 placeholder
	                        "", // Association Target ID 4 placeholder
	                        ""); // Notes placeholder
	                break;
	                
	        	case "searchEszett":
	        		resultCollector.setFullExtensionTranslation(
	        				conceptId, conceptStatus, "", "", term,
	                        languageCode, caseSignificance, type,
	                        languageReferenceSet, acceptabilityId,
	                        descriptionId, descriptionStatus
	                );
	        		break;
	        		
	            default:
	                System.out.println("Unhandled result set type: " + resultSetType);
	        }
	    }
	}

	public boolean hasColumn(ResultSet rs, String columnName) throws SQLException {
		ResultSetMetaData metaData = rs.getMetaData();
		int columnCount = metaData.getColumnCount();

		for (int i = 1; i <= columnCount; i++) {
			if (columnName.equalsIgnoreCase(metaData.getColumnLabel(i))) {
				return true;
			}
		}
		return false;
	}

	private String getSafe(ResultSet rs, String columnName, Set<String> availableColumns) throws SQLException {
		return availableColumns.contains(columnName.toLowerCase()) ? rs.getString(columnName) : null;
	}

}