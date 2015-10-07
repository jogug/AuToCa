/*
 ** Copyright 2013 Software Composition Group, University of Bern. All rights reserved.
 */
package ch.unibe.scg.autoca.database;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.unibe.scg.autoca.datastructure.Dataset;
import ch.unibe.scg.autoca.datastructure.Language;
import ch.unibe.scg.autoca.datastructure.Project;

/**
 * Handles connections and code links to H2DB.
 * 
 * @author Joel
 * 
 */

public class Database {
	private static final Logger logger = LoggerFactory.getLogger(Database.class);
	
	private final String SERVERFILENAME;
	private final String DRIVER;
	private final String USER;
	private final String PASSWORD;
	private final String LOGINPREFIX;
	
	//Tables
	private final String TEMPORARY;
	private final String TEMPFILTER;
	public	final String OCCURRENCE;
	private final String TOKEN;
	private final String FILE;
	private final String PROJECT;
	private final String LANGUAGE;
	private final String RESULTTABLE;
	private final String RANK;
	private final String PRECISION;
	private final String SUMMARY;
	
	//token
	private final int MAXTOKENLENGTH;
	//prefix
	private final String PREFIXSTAT;
	
	//Parser
	private final String NEWLINE;
	private final String INDENT;
	private final String DEDENT;
	private final String DELIMITER;
	private final String COMMENT;
	
	//Ram limitation for newline
	private final int DEFAULT_RANGE_SIZE = 100000;
	private final int DEFAULT_MAX_FILTER_LENGTH = 100;
	
	private final int DEFAULT_MAX_VARCHAR_LENGTH = 100;
	
	private Connection connection;
	private Path pathDb;
	private PreparedStatement prepInsertStatement;	
	private int currentFileId;

	public Database(Path path, Dataset dataset) throws ClassNotFoundException, SQLException {
		SERVERFILENAME = dataset.getServerFilename();
		DRIVER = dataset.getDriver();
		USER = dataset.getUser();
		PASSWORD = dataset.getPassword();
		LOGINPREFIX = dataset.getLoginprefix();
		
		TEMPORARY = dataset.getTEMPORARY();
		TEMPFILTER = dataset.getTEMPFILTER();
		OCCURRENCE = dataset.getOCCURRENCE();
		TOKEN = dataset.getTOKEN();
		FILE = dataset.getFILE();
		PROJECT = dataset.getPROJECT();
		LANGUAGE = dataset.getLANGUAGE();
		RESULTTABLE = dataset.getRESULTTABLE();
		RANK = dataset.getRANK();
		PREFIXSTAT = dataset.getPREFIXSTAT();
		PRECISION = dataset.getPRECISION();
		
		NEWLINE = dataset.getDBNEWLINE();
		INDENT = dataset.getINDENT();
		DEDENT = dataset.getDEDENT();
		DELIMITER = dataset.getDELIMITER();
		COMMENT = dataset.getCOMMENT();
		SUMMARY = dataset.getSUMMARY();
		
		MAXTOKENLENGTH = dataset.getDEFAULT_MAX_TOKEN_LENGTH();
		
		pathDb = path.resolve(SERVERFILENAME);
	}

	public void initialise() throws SQLException, ClassNotFoundException {
		openConnection();
		
		Statement stmt = connection.createStatement();
		stmt.execute("DROP ALL OBJECTS");
		stmt = connection.createStatement();
		String query = 
				"CREATE MEMORY TABLE IF NOT EXISTS \"" + TOKEN + "\" ("
				+ "id MEDIUMINT NOT NULL AUTO_INCREMENT, "
				+ "token VARCHAR( " + MAXTOKENLENGTH +" ) NOT NULL, "
				+ "PRIMARY KEY (id));"
	
				+ "CREATE TABLE IF NOT EXISTS \"" + FILE + "\" ("
				+ "id MEDIUMINT NOT NULL AUTO_INCREMENT, "
				+ "file VARCHAR("+DEFAULT_MAX_VARCHAR_LENGTH+") NOT NULL, "
				+ "projectid MEDIUMINT NOT NULL,"
				+ "PRIMARY KEY (id));"
	
				+ "CREATE TABLE IF NOT EXISTS \"" + PROJECT + "\" ("
				+ "id MEDIUMINT NOT NULL AUTO_INCREMENT, "
				+ "file VARCHAR("+DEFAULT_MAX_VARCHAR_LENGTH+") NOT NULL, "
				+ "languageid MEDIUMINT NOT NULL," 
				+ "PRIMARY KEY (id));"
	
				+ "CREATE TABLE IF NOT EXISTS \"" + LANGUAGE + "\" ("
				+ "id MEDIUMINT NOT NULL AUTO_INCREMENT, "
				+ "file VARCHAR("+DEFAULT_MAX_VARCHAR_LENGTH+") NOT NULL, " 
				+ "PRIMARY KEY (id));"
	
				+ "CREATE TABLE IF NOT EXISTS \"" + OCCURRENCE + "\" (" 
				+ "tokenid MEDIUMINT NOT NULL, "
				+ "fileid MEDIUMINT NOT NULL, "
				+ "orderid MEDIUMINT NOT NULL AUTO_INCREMENT, "
				+ "PRIMARY KEY (orderid));"
								
				+ "CREATE INDEX indexToken ON \""+TOKEN+"\" (TOKEN);";	
		stmt.execute(query);
		stmt.close();	
		closeConnection();
	}
	
	/*
	 * SCANMODE OPEN&CLOSE
	 */

	public void newLanguage(Language language) throws ClassNotFoundException, SQLException {
		openConnection();
		insertLanguage(language.getName());
		language.setId(getLanguageId(language.getName()));
		
		createTempTable();
		String prepInsertStatementQuery = "INSERT INTO \"" + TEMPORARY + "\"(token) VALUES (?)";
		prepInsertStatement = connection.prepareStatement(prepInsertStatementQuery);
	}

	public void languageFinished() throws SQLException {
		dropTableIfExists(TEMPORARY);
		prepInsertStatement.close();
		closeConnection();
	}
	
	public void newProject(Project project) throws SQLException {
		Statement stmt = connection.createStatement();
		stmt.execute("INSERT INTO \"" + PROJECT + "\"(file, languageid) VALUES ('" + project.getName() + "'," + project.getLanguage().getId() + ")");
		project.setId(getProjectId(project.getName()));
		stmt.close();
	}
	
	public void projectFinished() {

	}


	public void newFile(String file, int projId) throws SQLException {
		createTempTable();		
		Statement stmt = connection.createStatement();
		stmt.execute("INSERT INTO \"" + FILE + "\"(file, projectid) VALUES ('" + file + "'," + projId + ")");
		currentFileId = getFileId(file);
		stmt.close();
	}
	
	public void fileFinished() throws SQLException {
		assignTokensInTempTableIDs();
		insertTemTokensOrderedInOccurrences();
		dropTableIfExists(TEMPORARY);
	}

	private void openConnection() throws ClassNotFoundException, SQLException {
		if (connection == null) {
			Class.forName(DRIVER);
			connection = DriverManager.getConnection(LOGINPREFIX + pathDb.toString(), USER , PASSWORD);
		}
	}

	private void closeConnection() throws SQLException {
		if (connection == null) {
			connection.close();
			connection = null;
		}
	}

	/*
	 * SCANMODE PROCESS 
	 */

	public void insertToken(String token) throws SQLException {
		prepInsertStatement.setString(1, token);
		prepInsertStatement.execute();
	}
	
	private void insertLanguage(String language) throws SQLException {
		Statement stmt = connection.createStatement();
		stmt.execute("INSERT INTO \"" + LANGUAGE + "\"(file) VALUES ('" + language + "')");
		stmt.close();
	}

	/*
	 * SCANMODE FINALIZE
	 */
	
	private void insertTemTokensOrderedInOccurrences() throws SQLException {
		Statement stmt = connection.createStatement();
		stmt.execute("INSERT INTO \"" + OCCURRENCE + "\" ( TOKENID , FILEID) "
				+ "SELECT TOKENS.ID, " 	+ currentFileId + " FROM \"" + TEMPORARY + "\" TEMP "
				+ "INNER JOIN \"" + TOKEN + "\"  TOKENS ON TOKENS.TOKEN = TEMP.TOKEN "
				+ "ORDER BY TEMP.ID ASC");
	}

	private void assignTokensInTempTableIDs() throws SQLException {
		Statement stmt = connection.createStatement();
		stmt.execute("CREATE INDEX indexTemporary ON \"" + TEMPORARY + "\"(TOKEN);");				
		
		stmt.execute("INSERT INTO \"" + TOKEN + "\"( TOKEN ) "
				+ "(SELECT DISTINCT SRC.TOKEN FROM  \"" + TEMPORARY + "\"  SRC"
				+ " LEFT OUTER JOIN \"" + TOKEN + "\" DST ON SRC.TOKEN = DST.TOKEN WHERE DST.TOKEN IS NULL)");
		stmt.close();
	}

	/*
	 * FILTERING OPEN&CLOSE
	 */
	
	public void newFilterTable() throws ClassNotFoundException, SQLException{
		openConnection();
	}
	
	public void filterTableFinished() throws SQLException{
		dropTableIfExists(TEMPFILTER);
		dropTableIfExists(TEMPORARY);
		closeConnection();
	}
	
	/*
	 * FILTER PROCESS
	 */
	
	public void globalMethod(String languageName, String resultTable) throws SQLException{
		Statement stmt = connection.createStatement();
		stmt.execute("CREATE MEMORY TABLE IF NOT EXISTS \"" + resultTable + "\" AS " +
				"SELECT TOKENID, PROJECTID, COUNT(TOKENID) COUNT FROM " +
				"(SELECT TOKENID,PROJECTID FROM \""+OCCURRENCE+"\" OCCURRENCES " + 
				"INNER JOIN \""+FILE+"\" FILES ON OCCURRENCES.FILEID = FILES.ID " + 
				"INNER JOIN \""+PROJECT+"\" PROJECTS ON PROJECTS.ID = FILES.PROJECTID  " +
				"WHERE PROJECTS.LANGUAGEID = " + getLanguageId(languageName) + ") " +
				"GROUP BY TOKENID, PROJECTID ORDER BY COUNT DESC");	
		stmt.close();
	}
	
	public void coverageMethod(String languageName, String resultTable) throws SQLException{
		Statement stmt = connection.createStatement();
		stmt.execute("CREATE MEMORY TABLE IF NOT EXISTS \"" + resultTable + "\" AS " +
				"SELECT TOKENID, PROJECTID, COUNT(FILEID) COUNT FROM  " +
				"(SELECT DISTINCT TOKENID, PROJECTID, FILEID, FILES.FILE FROM \""+OCCURRENCE+"\" OCCURRENCES " +   
				"INNER JOIN \""+FILE+"\" FILES ON OCCURRENCES.FILEID = FILES.ID " +
				"INNER JOIN \""+PROJECT+"\" PROJECTS ON PROJECTS.ID = FILES.PROJECTID " +  
				"WHERE PROJECTS.LANGUAGEID = " + getLanguageId(languageName) + ") " +
				"GROUP BY  TOKENID, PROJECTID ORDER BY COUNT DESC");	
		stmt.close();
	}
	
	public void indentSMethod(String languageName, String resultTable) throws SQLException{
		int indentId = getTokenId(INDENT);
		int languageId = getLanguageId(languageName);
		Statement stmt = connection.createStatement();
		stmt.execute("CREATE MEMORY TABLE IF NOT EXISTS \"" + resultTable + "\" AS " +
				"SELECT TOKENID, PROJECTID, COUNT(TOKENID) COUNT FROM " +
				"(SELECT TOKENID, PROJECTID, ORDERID FROM \""+OCCURRENCE+"\" OCCURRENCES " +
				"INNER JOIN \""+FILE+"\" FILES ON OCCURRENCES.FILEID = FILES.ID " +
				"INNER JOIN \""+PROJECT+"\" PROJECTS ON PROJECTS.ID = FILES.PROJECTID WHERE PROJECTS.LANGUAGEID = " + languageId + ") A " +
				"INNER JOIN (SELECT ORDERID FROM \""+OCCURRENCE+"\" OCCURRENCES WHERE TOKENID =  " + indentId + ") B ON A.ORDERID = B.ORDERID + 1 " +  
				"GROUP BY TOKENID, PROJECTID ORDER BY COUNT DESC");	
		stmt.close();
	}
	
	public void newlineMethod(String languageName, String resultTable) throws SQLException{
		int indentId = getTokenId(INDENT);
		int dedentId = getTokenId(DEDENT);
		int newlineId = getTokenId(NEWLINE);
		int delimiterId = getTokenId(DELIMITER);
		int languageId = getLanguageId(languageName);
		Statement stmt = connection.createStatement();
		stmt.execute("CREATE TABLE IF NOT EXISTS \"" + TEMPFILTER  + "\" (" 
					+ "tokenid MEDIUMINT NOT NULL, "
					+ "fileid MEDIUMINT NOT NULL, "
					+ "orderid MEDIUMINT NOT NULL AUTO_INCREMENT, "
					+ "PRIMARY KEY (orderid))");
		stmt.execute("INSERT INTO \"" + TEMPFILTER  + "\" ( TOKENID , FILEID) "
					+ "SELECT TOKENID, FILEID  FROM \"" + OCCURRENCE + "\" "
					+ "WHERE TOKENID != " + indentId + " AND TOKENID != " + dedentId  + " AND TOKENID != " + delimiterId 
					+ " ORDER BY ORDERID ASC");
		stmt.execute("CREATE TABLE IF NOT EXISTS \"" + resultTable + "\" AS " +
				"SELECT TOKENID, PROJECTID, COUNT(TOKENID) COUNT FROM " +
				"(SELECT TOKENID, PROJECTID, ORDERID FROM \"" + TEMPFILTER + "\" OCCURRENCES " +
				"INNER JOIN \""+FILE+"\" FILES ON OCCURRENCES.FILEID = FILES.ID " +
				"INNER JOIN \""+PROJECT+"\" PROJECTS ON PROJECTS.ID = FILES.PROJECTID WHERE PROJECTS.LANGUAGEID = " + languageId + ") A " +
				"INNER JOIN (SELECT ORDERID FROM \"" + TEMPFILTER + "\" OCCURRENCES WHERE TOKENID =  " + newlineId + ") B ON A.ORDERID = B.ORDERID + 1 " +  
				"GROUP BY TOKENID, PROJECTID ORDER BY COUNT DESC");	
		stmt.close();
	}
	
	public void newlineMethodSaveRam(String languageName, String resultTable) throws SQLException{
		int indentId = getTokenId(INDENT);
		int dedentId = getTokenId(DEDENT);
		int newlineId = getTokenId(NEWLINE);
		int delimiterId = getTokenId(DELIMITER);
		int languageId = getLanguageId(languageName);
		Statement stmt = connection.createStatement();

		int nrOfOccurrences = getRowCountOfTable(OCCURRENCE); 

		stmt.execute("CREATE MEMORY TABLE IF NOT EXISTS \"" + TEMPFILTER  + "\" (" 
				+ "id MEDIUMINT AUTO_INCREMENT, "
				+ "TOKENID MEDIUMINT NOT NULL, "
				+ "PROJECTID MEDIUMINT NOT NULL, "
				+ "COUNT MEDIUMINT NOT NULL, "
				+ "PRIMARY KEY (id))");
		for(int curLeft = 1; curLeft < nrOfOccurrences; curLeft += DEFAULT_RANGE_SIZE + 1){
			logger.info("Working on :" + curLeft + " of " + nrOfOccurrences);
			//CREATE TEMPFILTER
			stmt.execute("CREATE MEMORY TABLE IF NOT EXISTS \"" + TEMPFILTER+"2"  + "\" (" 
					+ "tokenid MEDIUMINT NOT NULL, "
					+ "fileid MEDIUMINT NOT NULL, "
					+ "orderid MEDIUMINT NOT NULL AUTO_INCREMENT, "
					+ "PRIMARY KEY (orderid))");
			//FETCH RANGE 
			stmt.execute("INSERT INTO \"" + TEMPFILTER + "2" + "\" ( TOKENID , FILEID) "
					+ "SELECT TOKENID, FILEID FROM \"" + OCCURRENCE + "\" "
					+ "WHERE (ORDERID BETWEEN " + curLeft + " AND " 
					+ Math.min(curLeft+DEFAULT_RANGE_SIZE, nrOfOccurrences) + ") AND TOKENID != " + indentId + " AND TOKENID != " + dedentId  + " AND TOKENID != " + delimiterId 
					+ " ORDER BY ORDERID ASC");
			//CALCULATE RESULT 
			stmt.execute("CREATE TABLE \"" + TEMPORARY + "\" AS " +
					"SELECT TOKENID, PROJECTID, COUNT(TOKENID) COUNT FROM " +
					"(SELECT TOKENID, PROJECTID, ORDERID FROM \"" + TEMPFILTER + "2" + "\" KEYWORDS " +
					"INNER JOIN \""+FILE+"\" FILES ON KEYWORDS.FILEID = FILES.ID " +
					"INNER JOIN \""+PROJECT+"\" PROJECTS ON PROJECTS.ID = FILES.PROJECTID WHERE PROJECTS.LANGUAGEID = " + languageId + ") A " +
					"INNER JOIN (SELECT ORDERID FROM \"" + TEMPFILTER + "2" + "\" KEYWORDS WHERE TOKENID =  " + newlineId + ") B ON A.ORDERID = B.ORDERID + 1 " +  
					"GROUP BY TOKENID, PROJECTID");
			//MERGE
			stmt.execute("INSERT INTO \"" + TEMPFILTER  + "\" (TOKENID,PROJECTID,COUNT) SELECT TOKENID,PROJECTID,COUNT FROM \"" + TEMPORARY  + "\"");
			
			//DROP TABLE
			dropTableIfExists(TEMPFILTER + "2");
			dropTableIfExists(TEMPORARY);
		}
		
		stmt.execute("CREATE TABLE \"" + resultTable + "\" AS SELECT TOKENID, PROJECTID, SUM(COUNT) COUNT FROM \"" + TEMPFILTER + "\" GROUP BY TOKENID, PROJECTID");
		
		dropTableIfExists(TEMPFILTER);


		stmt.close();
	}
	
		
	public void indentMethodFast(String languageName, String resultTable) throws SQLException{
		int newlineID = getTokenId(NEWLINE);
		int indentID = getTokenId(INDENT);
		int dedentID = getTokenId(DEDENT);
		int delimID = getTokenId(DELIMITER);
		int commentID = getTokenId(COMMENT);
		int languageID = getLanguageId(languageName);
		
		PreparedStatement fetchProjectPrepStatement;
		PreparedStatement fetchFileIdsPrepStatment;
		ResultSet rs;
		List<String> projects = new ArrayList<>();
		List<Integer> keywords;
		Statement stmt = connection.createStatement();	
		stmt.execute("CREATE MEMORY TABLE IF NOT EXISTS \"" + TEMPFILTER + "\" " +
				"(id MEDIUMINT NOT NULL AUTO_INCREMENT, tokenid INT NOT NULL, projectid INT NOT NULL, count INT NOT NULL, "
				+ "PRIMARY KEY (id))");

		String prepInsertStatementQuery = "INSERT INTO \"" + TEMPORARY + "\"(tokenid, projectid) VALUES (?,?)";
		
		String fetchProjectStatementString = "SELECT * FROM \"" + OCCURRENCE + "\" WHERE (fileid BETWEEN ? AND ?) AND tokenid !=" + delimID + "AND tokenid !=" + dedentID ;
		fetchProjectPrepStatement = connection.prepareStatement(fetchProjectStatementString);
		
		String fetchFileIdsString = "SELECT * FROM \"" + FILE + "\" WHERE projectid = ? ORDER BY id asc";
		fetchFileIdsPrepStatment = connection.prepareStatement(fetchFileIdsString,
				ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
		

				//Get project ids
		rs = stmt.executeQuery("SELECT * FROM \"" + PROJECT + "\" WHERE languageid = " + languageID);
		while(rs.next()){
			projects.add(rs.getString(2));
		}
		rs.close();			
		
		for(String project: projects){							//In each Project
			int projectId = getProjectId(project);	
			logger.info("Working on project:" + project + "," + projectId +"/"+ projects.size());
			
			
			// Get ids of the files in the project
			fetchFileIdsPrepStatment.setInt(1, projectId);
			rs = fetchFileIdsPrepStatment.executeQuery();
			
			if(rs.next()){
				int min = rs.getInt(1);		
				rs.afterLast();
				rs.previous();
				int max = rs.getInt(1);

				
					
				keywords = new ArrayList<Integer>();			//stores found Keywords
				stmt.execute("CREATE MEMORY TABLE \"" + TEMPORARY + "\" " +
							"(id MEDIUMINT NOT NULL AUTO_INCREMENT, tokenid INT NOT NULL, projectid INT NOT NULL)");
					
				fetchProjectPrepStatement.setInt(1, min);		//Fetch file in DB
				fetchProjectPrepStatement.setInt(2, max);
				rs = fetchProjectPrepStatement.executeQuery();
					
				int afterNewline = Integer.MAX_VALUE;
				int prevToken = Integer.MAX_VALUE;
					
				while(rs.next()){		
					//Search through file		
					if(prevToken == newlineID || prevToken == commentID){
						if(rs.getInt(1) == indentID){
							//found, check if there is a token before
							if(afterNewline != Integer.MAX_VALUE){
								keywords.add(afterNewline);
							}
							afterNewline = Integer.MAX_VALUE;
						}else if(rs.getInt(1) != newlineID && rs.getInt(1) != commentID){
							//save as beforeIndent token
							afterNewline = rs.getInt(1);
						}
					}else if(prevToken == indentID && rs.getInt(1) != newlineID && rs.getInt(1) != commentID){
						afterNewline = rs.getInt(1);
					}
					prevToken = rs.getInt(1);			
									
				}
				prepInsertStatement = connection.prepareStatement(prepInsertStatementQuery);
				for(Integer keyword: keywords){
					prepInsertStatement.setInt(1, keyword);
					prepInsertStatement.setInt(2, projectId);
					prepInsertStatement.execute();
				}
				//Finalize Project
				stmt.execute("INSERT INTO \"" + TEMPFILTER + "\"(tokenid, projectid, count) "
						+ "(SELECT tokenid, projectid, COUNT(ID) AS count FROM \"" + TEMPORARY + "\" "
						+ "GROUP BY TOKENID, PROJECTID)");
				dropTableIfExists(TEMPORARY);
				prepInsertStatement.close();
				rs.close();
			}		
		}
		//finalize 			
		stmt.execute("CREATE MEMORY TABLE IF NOT EXISTS \"" + resultTable + "\" AS SELECT " +
			 "TOKENID, PROJECTID, COUNT FROM \"" + TEMPFILTER + "\" ORDER BY COUNT DESC");
		
		stmt.execute("ALTER TABLE \"" + resultTable + "\" ADD id INT NOT NULL AUTO_INCREMENT");			
	
		stmt.close();
	}


	public void intersectProjectsOfLanguage(String languageName, String resultTable, int minOccInProjects) throws SQLException{
		Statement stmt = connection.createStatement();
		stmt.execute("CREATE MEMORY TABLE IF NOT EXISTS \"" + TEMPFILTER + "\" AS " +
				"SELECT TOKENID, COUNT(PROJECTID) COUNT FROM \"" + resultTable + "\" " +
				"GROUP BY TOKENID ORDER BY COUNT DESC;" +		
				
				"DELETE FROM \"" + TEMPFILTER + "\" WHERE COUNT < " + minOccInProjects + "; " +
								
				"ALTER TABLE \"" + resultTable + "\" RENAME TO \"" + TEMPORARY + "\" ; " +
				
				"CREATE MEMORY TABLE IF NOT EXISTS \"" + resultTable + "\" AS " +
				"SELECT A.TOKENID, PROJECTID, A.COUNT FROM \"" + TEMPORARY + "\" A " +
				"INNER JOIN \"" + TEMPFILTER + "\" B ON A.TOKENID = B.TOKENID");	
		stmt.close();
	}
	
	public void removeUpperCaseTokenFromResulttable(String languageName, String resultTable) throws SQLException{
		Statement stmt = connection.createStatement();
		stmt.execute("CREATE MEMORY TABLE IF NOT EXISTS \"" + TEMPORARY + "\" AS " +
				"SELECT TOKEN, L0.TOKENID, PROJECTID, COUNT FROM " +
				"(SELECT * FROM \"" + resultTable + "\") L0 " +
				"INNER JOIN \""+TOKEN+"\" L1 ON L0.TOKENID = L1.ID; " +				
				"DELETE FROM \"" + TEMPORARY + "\" t WHERE t.TOKEN NOT LIKE LOWER(t.TOKEN);"+
				"DROP TABLE \"" + resultTable + "\" ; " +
				"ALTER TABLE \"" + TEMPORARY + "\" DROP COLUMN TOKEN; " +
				"ALTER TABLE \"" + TEMPORARY + "\" RENAME TO \"" + resultTable + "\"");
		stmt.close();
	}
	
	public void orderByCountAndNameToken(String languageName, String resultTable) throws SQLException{
		Statement stmt = connection.createStatement();
		stmt.execute("CREATE MEMORY TABLE IF NOT EXISTS \"" + TEMPORARY + "\" AS " +
				"SELECT TOKEN, L0.TOKENID, PROJECTID, COUNT FROM " +
				"(SELECT * FROM \"" + resultTable + "\") L0 " +
				"INNER JOIN \""+TOKEN+"\" L1 ON L0.TOKENID = L1.ID; " +
				
				"DROP TABLE \"" + resultTable + "\" ; " +
		
				"CREATE MEMORY TABLE IF NOT EXISTS \"" + resultTable + "\" AS " +
				"SELECT TOKEN, SUM(COUNT) COUNT FROM \"" + TEMPORARY + "\" " + 
				"GROUP BY TOKEN ORDER BY COUNT DESC;");
		stmt.close();
	}
	
	public void removeTokenWithSpecialCharInSubStringFromResulttable(String languageName, String resultTable, String substring) throws SQLException{
		Statement stmt = connection.createStatement();
		stmt.execute("CREATE MEMORY TABLE IF NOT EXISTS \"" + TEMPORARY + "\" AS " +
				"SELECT TOKEN, L0.TOKENID, PROJECTID, COUNT FROM " +
				"(SELECT * FROM \"" + resultTable + "\") L0 " +
				"INNER JOIN \""+TOKEN+"\" L1 ON L0.TOKENID = L1.ID; " +
	
				"DROP TABLE \"" + resultTable + "\" ; " +
	
				"DELETE FROM \"" + TEMPORARY + "\" t WHERE LOCATE('" + substring + "' , t.TOKEN) > 0;" +
				"ALTER TABLE \"" + TEMPORARY + "\" DROP COLUMN TOKEN; " +
				"ALTER TABLE \"" + TEMPORARY + "\" RENAME TO \"" + resultTable + "\"");
		stmt.close();
	}

	/*
	 * PREPARATION OF DATASET ANALYSIS WITH FILTERS OPEN & CLOSE
	 */
	
	public void newAnalyseDatasetOfLanguage() throws ClassNotFoundException, SQLException {
		openConnection();
	}

	public void analyseDatasetOfLanguageFinished() throws SQLException {
		closeConnection();
	}	

	
	/*
	 * STATISTICS PROCESS 
	 */
	
	public void calculateStatisticsPerLanguage(String langName, String resultTable, String output) throws SQLException{
		Statement stmt = connection.createStatement();	
		stmt.execute("CREATE MEMORY TABLE IF NOT EXISTS \"" + TEMPFILTER + "\" " +
					 "(id MEDIUMINT NOT NULL AUTO_INCREMENT, tokenid INT NOT NULL, count INT NOT NULL)");
		
		stmt.execute("INSERT INTO \"" + TEMPFILTER + "\" (tokenid,count) " +
					 "SELECT tokenid, SUM(count) as count FROM \""+ resultTable + "\" GROUP BY TOKENID ORDER BY COUNT DESC");
		
		stmt.execute("CREATE MEMORY TABLE IF NOT EXISTS \"" + output + "\" AS "+
					"(SELECT a.TOKEN, COUNT, b.ID FROM \"" +  langName + "\" a "+
					"LEFT JOIN (SELECT TOKENID, SUM(COUNT) AS COUNT, ID FROM \""+ TEMPFILTER + "\" GROUP BY TOKENID) b " +
					"ON a.ID = b.TOKENID ORDER BY ID ASC)");
		
		int tp = getFirstReturnedIntOfStatement("SELECT count(*) FROM \"" + output + "\" WHERE ID IS NOT NULL");
		int lowestFoundToken = getFirstReturnedIntOfStatement("SELECT TOP 1 ID FROM \"" + output + "\" ORDER BY ID DESC");
		int fp = lowestFoundToken-tp;
		//token not in project
		int tnip = getFirstReturnedIntOfStatement("SELECT count(*) FROM \"" + langName + "\" WHERE ID IS NULL");
		int fn = getFirstReturnedIntOfStatement("SELECT count(*) FROM \"" + output + "\" WHERE ID IS NULL")-tnip;
		int tn = getFirstReturnedIntOfStatement("SELECT COUNT(TOKENID) FROM " +
				"(SELECT DISTINCT TOKENID FROM \""+OCCURRENCE+"\" OCCURRENCES " +
				"INNER JOIN \""+FILE+"\" FILES ON OCCURRENCES.FILEID = FILES.ID " +
				"INNER JOIN \""+PROJECT+"\" PROJECTS ON PROJECTS.ID = FILES.PROJECTID WHERE PROJECTS.LANGUAGEID = " + getLanguageId(langName) +")")
				-lowestFoundToken;

		stmt.execute("INSERT INTO \""+ RESULTTABLE + "\"(filter,TP, FP, TN, FN, TOKnotInSET) VALUES ('"+resultTable+"',"+tp+","+fp+","+tn+","+fn+","+tnip+")");

		stmt.close();
	}	
	
	/*
	 * SUMMARY OPEN&CLOSE
	 */
	
	public void newSummary() throws ClassNotFoundException, SQLException {
		openConnection();
	}

	public void SummaryFinished() throws SQLException {
		closeConnection();
	}	
	
	/*
	 * SUMMARY PROCESS
	 */
	
	public void createPrecisionSummaryTable(String language) throws SQLException {
		Statement stmt = connection.createStatement();
		int nrOfKeywords = getRowCountOfTable(language);
		String query = "CREATE TABLE IF NOT EXISTS \"" + PRECISION+ language + "\" ("
				+ "FILTER VARCHAR( " + DEFAULT_MAX_FILTER_LENGTH +" ) ";
		for(int i = 1; i < nrOfKeywords+1; i++){
			query += ",\"" + i + "Token\"" + " DECIMAL(5,3)";
		}		
		query += ",PRIMARY KEY (FILTER));";
		
		stmt.execute(query);
		stmt.close();
	}
	

	public void insertPrecision(String resultName, String language) throws SQLException {
		Statement stmt = connection.createStatement();
		ResultSet rs = stmt.executeQuery("SELECT * FROM "+ "\""+PREFIXSTAT+language+resultName+"\" ORDER BY ID ASC NULLS LAST");
		List<Double> numbers = new ArrayList<Double>();
		List<Double> precision = new ArrayList<Double>();
		
		while(rs.next()){
			numbers.add((double)rs.getInt(3));
		}
		
		int j = 1;
		for(int i = 1; i<numbers.size()+1;i++){
			if(numbers.get(i-1)==null||numbers.get(i-1)==0){
				precision.add((double) 0);
			}else{
				precision.add(j/numbers.get(i-1));
				j++;
			}
		}
		String sql = "INSERT INTO \"" + PRECISION+language + "\" VALUES('" + resultName+"'";
		for(Double i: precision){
			sql += ", "+i;
		}
		sql+=")";
		
		stmt.execute(sql);
		stmt.close();
	}
	
	
	public void createRanksSummaryTable(String language, List<String> filterChains) throws SQLException {
		Statement stmt = connection.createStatement();
		ResultSet rs = stmt.executeQuery("SELECT * FROM "+ "\""+language+"\"");
		List<String> keywords = new ArrayList<String>();
		
		while(rs.next()){
			keywords.add(rs.getString(1));
		}
		
		rs.close();
		String query = "CREATE TABLE \"" + RANK + language + "\" AS SELECT " + 
			"\"" + PREFIXSTAT + language + filterChains.get(0) + "\"" + ".TOKEN AS KEYWORD, " +
			"\"" + PREFIXSTAT + language + filterChains.get(0) + "\"" +	".ID AS " + filterChains.get(0);
		for(int i = 1; i < filterChains.size(); i++){
			query += "," + "\"" + PREFIXSTAT + language + filterChains.get(i) + "\"" + ".ID AS " + filterChains.get(i);			
		}
		query += " FROM " + "\"" + PREFIXSTAT + language + filterChains.get(0) + "\"";
		for(int i = 1; i < filterChains.size(); i++){
			query += " INNER JOIN " + "\"" + PREFIXSTAT + language + filterChains.get(i) + "\"" + " ON " + 
					 "\"" + PREFIXSTAT + language + filterChains.get(i) + "\"" + ".TOKEN" + " = " + "\"" + PREFIXSTAT + language + filterChains.get(0) + "\"" + ".TOKEN ";
		}
		stmt.execute(query);
	}
	
	public void createSummaryOfDatasetStatistics(String language) throws SQLException{
		Statement stmt = connection.createStatement();
		dropTableIfExists(SUMMARY);
		String summaryTable = "CREATE MEMORY TABLE IF NOT EXISTS \"" + SUMMARY + "\" ("
							+ "name VARCHAR( 30 ) NOT NULL, "
							+ "value MEDIUMINT NOT NULL AUTO_INCREMENT,"
							+ "PRIMARY KEY (name));";
		
		stmt.execute(summaryTable);
		int keywords = getRowCountOfTable(language);
		int nrOfOccurrences = getRowCountOfTable(OCCURRENCE);
		int files = getRowCountOfTable(FILE);
		int projects = getRowCountOfTable(PROJECT);
		int uniqueTokens = getRowCountOfTable(TOKEN);
		stmt.execute("INSERT INTO \"" + SUMMARY + "\"(name, value) VALUES ('"+language+" Keywords',"+keywords+")");
		stmt.execute("INSERT INTO \"" + SUMMARY + "\"(name, value) VALUES ('Projects',"+projects+")");
		stmt.execute("INSERT INTO \"" + SUMMARY + "\"(name, value) VALUES ('Files',"+files+")");
		stmt.execute("INSERT INTO \"" + SUMMARY + "\"(name, value) VALUES ('Token Occurrences',"+nrOfOccurrences+")");
		stmt.execute("INSERT INTO \"" + SUMMARY + "\"(name, value) VALUES ('Unique Tokens',"+uniqueTokens+")");
	}
	
	/*
	 * LOAD REAL TOKEN OPEN&CLOSE
	 */
	
	public void newLanguagesActualTokenFile() throws SQLException, ClassNotFoundException {		
		openConnection();
		dropTableIfExists(TEMPORARY);
		createTempTable();	
		String prepInsertStatementQuery = "INSERT INTO \"" + TEMPORARY + "\"(token) VALUES (?)";
		prepInsertStatement = connection.prepareStatement(prepInsertStatementQuery);		
	}

	public void languagesActualTokenFileFinished(String resultTableName) throws SQLException{
		dropTableIfExists(resultTableName);		
		Statement stmt = connection.createStatement();
		stmt.execute("CREATE MEMORY TABLE IF NOT EXISTS \"" + resultTableName + "\" AS "+
				"(SELECT a.TOKEN, b.ID FROM \"" + TEMPORARY + "\"  a " +
				"LEFT JOIN \"" + TOKEN + "\" b ON a.TOKEN = b.TOKEN)");
		prepInsertStatement.close();
		dropTableIfExists(TEMPORARY);
		stmt.close();			
		closeConnection();
	}
	
	
	/*
	 * COMMON TABLE OPERATIONS
	 */
	
	public void dropTableIfExists(String tableName) throws SQLException{
		Statement stmt = connection.createStatement();
		stmt.execute("DROP TABLE IF EXISTS \"" + tableName + "\""); 
		stmt.close();
	}	
	
	public void createResulttable() throws SQLException {
		Statement stmt = connection.createStatement();
		stmt.execute("CREATE TABLE IF NOT EXISTS \"" + RESULTTABLE + "\" ("
				+ "filter VARCHAR(100) NOT NULL, "
				+ "TP DECIMAL NOT NULL,"
				+ "FP DECIMAL NOT NULL,"
				+ "TN DECIMAL NOT NULL,"
				+ "FN DECIMAL NOT NULL,"
				+ "TOKnotInSET DECIMAL NOT NULL,"
				+ "PRIMARY KEY (filter));");
		stmt.close();
	}
	
	private void createTempTable() throws SQLException {
		String query = "CREATE MEMORY TABLE IF NOT EXISTS \"" + TEMPORARY + "\" "
				+ "(id MEDIUMINT NOT NULL AUTO_INCREMENT, token VARCHAR( " + MAXTOKENLENGTH +" ) NOT NULL);";
		Statement stmt = connection.createStatement();
		stmt.execute(query);
		stmt.close();
	}
	

	
	/*
	 * UTILILITIES & COMMON ID ACCESSES
	 */

	public void renameTable(String currentName, String newName) throws SQLException{
		Statement stmt = connection.createStatement();
		//stmt.execute("RENAME TABLE \"" + currentName + "\" TO \"" + newName + "\"");
		stmt.execute("ALTER TABLE \"" + currentName + "\" RENAME TO \"" + newName + "\"");
		stmt.close();
	}
	
	public int getRowCountOfTable(String tableName) throws SQLException{
		Statement stmt = connection.createStatement();
		ResultSet rs = stmt.executeQuery("SELECT count(*) FROM \"" + tableName + "\"");
		rs.next();
		int count = rs.getInt(1);
		rs.close();
		stmt.close();
		return count;
	}
	
	public int getSumOfCountColOfTable(String tableName) throws SQLException{
		Statement stmt = connection.createStatement();
		ResultSet rs = stmt.executeQuery("SELECT SUM(COUNT) FROM \"" + tableName + "\"");
		rs.next();
		int count = rs.getInt(1);
		rs.close();
		stmt.close();
		return count;
	}
	
	public int getLanguageId(String languageName) throws SQLException {
		Statement stmt = connection.createStatement();
		ResultSet rs = stmt.executeQuery("SELECT ID FROM \"" + LANGUAGE + "\" WHERE FILE = '" + languageName + "'");
		rs.next();
		int languageId = rs.getInt(1);
		rs.close();
		stmt.close();
		return languageId;
	}

	public int getProjectId(String projectName) throws SQLException {
		Statement stmt = connection.createStatement();
		ResultSet rs = stmt.executeQuery("SELECT ID FROM \"" + PROJECT + "\" WHERE FILE = '" + projectName + "'");
		rs.next();
		int projectId = rs.getInt(1);
		rs.close();
		stmt.close();
		return projectId;
	}
	
	public int getTokenId(String tokenName) throws SQLException {
		Statement stmt = connection.createStatement();
		ResultSet rs = stmt.executeQuery("SELECT ID FROM \"" + TOKEN + "\" WHERE TOKEN = '" + tokenName + "'");
		if(rs.next()){
			int tokenId = rs.getInt(1);
			rs.close();
			stmt.close();
			return tokenId;
		}else{
			logger.error("no " + tokenName + " in database");
			return Integer.MAX_VALUE;
		}
	}	
	
	public int getFileId(String fileName) throws SQLException {
		Statement stmt = connection.createStatement();
		ResultSet rs = stmt.executeQuery("SELECT TOP 1 ID FROM \"" + FILE + "\" WHERE FILE = '" + fileName + "' ORDER BY ID DESC");
		rs.next();
		int id = rs.getInt(1);		
		rs.close();
		stmt.close();
		return id;
	}
	
	private int getFirstReturnedIntOfStatement(String statement) throws SQLException{
		Statement stmt = connection.createStatement();
		ResultSet rs = stmt.executeQuery(statement);
		rs.next();
		int value = rs.getInt(1);		
		rs.close();
		stmt.close();
		return value;
	}
}
