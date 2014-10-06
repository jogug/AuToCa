/*
 ** Copyright 2013 Software Composition Group, University of Bern. All rights reserved.
 */
package ch.unibe.scg.autoca.db;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.unibe.scg.autoca.DataSet;

/**
 * Handles connections and code links to H2DB.
 * 
 * @author Joel
 * 
 */

//TODO FIX INCONSISTENCY WITH QUOTES "TableName" or just TableName
public class DB {
	private static final Logger logger = LoggerFactory.getLogger(DB.class);
	
	private final String FILENAME;
	private final String DRIVER;
	private final String USER;
	private final String PASSWORD;
	private final String LOGINPREFIX;
	
	//Tables
	private final String TEMPORARY;
	private final String TEMPFILTER;
	public final String OCCURENCE;
	private final String TOKEN;
	private final String FILE;
	private final String PROJECT;
	private final String LANGUAGE;
	
	//PreFixes
	private final String GLO = "GLOBAL_"; 
	private final String COV = "COVERAGE_";
	private final String SIND = "SIMIND_";
	private final String ACT = "ACTUALT_";
	
	private Connection connection;
	private Path pathDb;
	private PreparedStatement prepInsertStatement;	
	private int currentFileId;

	public DB(Path path, DataSet dataset) throws ClassNotFoundException, SQLException {

		FILENAME = dataset.getFilename();
		DRIVER = dataset.getDriver();
		USER = dataset.getUser();
		PASSWORD = dataset.getPassword();
		LOGINPREFIX = dataset.getLoginprefix();
		
		TEMPORARY = dataset.getTEMPORARY();
		TEMPFILTER = dataset.getTEMPFILTER();
		OCCURENCE = dataset.getOCCURENCE();
		TOKEN = dataset.getTOKEN();
		FILE = dataset.getFILE();
		PROJECT = dataset.getPROJECT();
		LANGUAGE = dataset.getLANGUAGE();
		
		pathDb = path.resolve(FILENAME);
	}

	public void initialize() throws SQLException, ClassNotFoundException {
		openConnection();
		
		Statement stmt = connection.createStatement();
		stmt.execute("DROP ALL OBJECTS");
		stmt = connection.createStatement();
		String query = 
				"CREATE MEMORY TABLE IF NOT EXISTS " + TOKEN + " ("
				+ "id MEDIUMINT NOT NULL AUTO_INCREMENT, "
				+ "token VARCHAR(30) NOT NULL, "
				+ "PRIMARY KEY (id));"
	
				+ "CREATE TABLE IF NOT EXISTS " + FILE + " ("
				+ "id MEDIUMINT NOT NULL AUTO_INCREMENT, "
				+ "file VARCHAR(100) NOT NULL, "
				+ "projectid MEDIUMINT NOT NULL,"
				+ "PRIMARY KEY (id));"
	
				+ "CREATE TABLE IF NOT EXISTS " + PROJECT + " ("
				+ "id MEDIUMINT NOT NULL AUTO_INCREMENT, "
				+ "file VARCHAR(100) NOT NULL, "
				+ "languageid MEDIUMINT NOT NULL," 
				+ "PRIMARY KEY (id));"
	
				+ "CREATE TABLE IF NOT EXISTS " + LANGUAGE + " ("
				+ "id MEDIUMINT NOT NULL AUTO_INCREMENT, "
				+ "file VARCHAR(100) NOT NULL, " 
				+ "PRIMARY KEY (id));"
	
				+ "CREATE TABLE IF NOT EXISTS " + OCCURENCE + " (" 
				+ "tokenid MEDIUMINT NOT NULL, "
				+ "fileid MEDIUMINT NOT NULL, "
				+ "orderid MEDIUMINT NOT NULL AUTO_INCREMENT, "
				+ "PRIMARY KEY (orderid));"
				
				+ "CREATE INDEX iTOKEN2 ON tokens (TOKEN);";	
		stmt.execute(query);
		stmt.close();
		createTempTable();		
		closeConnection();
	}
	
	/*
	 * SCANMODE OPEN&CLOSE
	 */

	public void newLanguage(String name) throws ClassNotFoundException, SQLException {
		openConnection();
		insertLanguage(name);
		createTempTable();
		
		String prepInsertStatementQuery = "INSERT INTO " + TEMPORARY + "(token) VALUES (?)";
		prepInsertStatement = connection.prepareStatement(prepInsertStatementQuery);
	}

	public void languageFinished() throws SQLException {
		prepInsertStatement.close();
		closeConnection();
	}
	
	public void newProject(String project, int langId) throws SQLException {
		Statement stmt = connection.createStatement();
		stmt.execute("INSERT INTO " + PROJECT + "(file, languageid) VALUES ('" + project + "'," + langId + ")");
		stmt.close();
	}
	
	public void projectFinished() {
		
	}

	public void newToken(String token) throws SQLException {
		prepInsertStatement.setString(1, token);
		prepInsertStatement.execute();
	}

	public void newFile(String file, int projId) throws SQLException {
		createTempTable();		
		Statement stmt = connection.createStatement();
		stmt.execute("INSERT INTO " + FILE + "(file, projectid) VALUES ('" + file + "'," + projId + ")");
		currentFileId = getCurrentFileId(file);
		stmt.close();
	}
	
	/**
	 * Assign Token IDs Fill Occurence Table Empty token_buffer table
	 * 
	 * @throws SQLException
	 */
	public void fileFinished() throws SQLException {
		assignTokensInTempTableIDs();
		insertOrderIDs();
		dropTempTable();
	}

	private void openConnection() throws ClassNotFoundException, SQLException {
		if (connection == null) {
			Class.forName(DRIVER);
			connection = DriverManager.getConnection(LOGINPREFIX + pathDb.toString(), USER , PASSWORD);
		}
	}

	private void closeConnection() throws SQLException {
		connection.close();
		connection = null;
	}

	/*
	 * SCANMODE 
	 */

	private void insertLanguage(String language) throws SQLException {
		Statement stmt = connection.createStatement();
		stmt.execute("INSERT INTO " + LANGUAGE + "(file) VALUES ('" + language + "')");
		stmt.close();
	}

	private void insertOrderIDs() throws SQLException {
		Statement stmt = connection.createStatement();
		stmt.execute("INSERT INTO "+ OCCURENCE + " ( TOKENID , FILEID) "
				+ "SELECT TOKENS.ID, " 	+ currentFileId + " FROM " + TEMPORARY + " TEMP "
				+ "INNER JOIN " + TOKEN + "  TOKENS ON TOKENS.TOKEN = TEMP.TOKEN "
				+ "ORDER BY TEMP.ID ASC");
	}

	private void assignTokensInTempTableIDs() throws SQLException {
		Statement stmt = connection.createStatement();
		stmt.execute("CREATE INDEX iTOKEN ON " + TEMPORARY + " (TOKEN);");				
		
		stmt.execute("INSERT INTO "	+ TOKEN	+ " ( TOKEN ) "
				+ "(SELECT DISTINCT SRC.TOKEN FROM  " + TEMPORARY + "  SRC"
				+ " LEFT OUTER JOIN TOKENS DST ON SRC.TOKEN = DST.TOKEN WHERE DST.TOKEN IS NULL)");
		stmt.close();
	}

	/*
	 * FILTER MODE OPEN&CLOSE
	 */
	public void newFilterTable() throws ClassNotFoundException, SQLException{
		openConnection();
		createFilterTable();
	}
	
	public void filterTableFinished() throws SQLException{
		dropTableIfExists(TEMPFILTER);
		closeConnection();
	}
	
	
	/*
	 * FILTER MODE
	 */
	public void intersectTables(String[] tables, String table) throws SQLException{
		Statement stmt = connection.createStatement();
		String statement = "SELECT * FROM " + table + " a";
		for(String string: tables){
			statement+= " INNER JOIN " + string + "b ON a.TOKEN = b.TOKEN";
		}
		stmt.execute(statement);
		stmt.close();
	}
	
	public void removeUpperCase() throws SQLException{
		Statement stmt = connection.createStatement();
		stmt.execute("DELETE * FROM " + TEMPFILTER + " t WHERE t.TOKEN NOT LIKE LOWER(t.TOKEN)");
		stmt.close();
	}
	
	public void removeSpecialSub(String substring) throws SQLException{
		Statement stmt = connection.createStatement();
		stmt.execute("DELETE * FROM " + TEMPFILTER + " t WHERE LOCATE('" + substring + "' , t.TOKEN) > 0)");
		stmt.close();
	}
	
	/*
	 * ANALYZE MODE OPEN&CLOSE
	 */
	
	public void newExtractLanguage() throws ClassNotFoundException, SQLException {
		openConnection();
	}

	public void extractLanguageFinished() throws SQLException {
		closeConnection();
	}	
	
	public void newAnalyzeLanguage() throws ClassNotFoundException, SQLException {
		openConnection();
	}

	public void analyzeLanguageFinished() throws SQLException {
		closeConnection();
	}	
	
	/*
	 * ANALYZE MODE
	 */
	public void analyzeGlobalPerProject(String resultTableName, String projectName, String langName) throws SQLException{
		resultTableName = GLO+resultTableName;
		dropTableIfExists(resultTableName);
		Statement stmt = connection.createStatement();
		stmt.execute("CREATE MEMORY TABLE IF NOT EXISTS \"" + resultTableName + "\" AS " +
				"SELECT TOKEN,COUNT(TOKENID) COUNT FROM " +
				"(SELECT TOKENID,ORDERID FROM OCCURENCES " +
				"INNER JOIN FILES ON OCCURENCES.FILEID = FILES.ID "+
				"INNER JOIN PROJECTS ON PROJECTS.ID = FILES.PROJECTID WHERE PROJECTS.LANGUAGEID = " + 
				getLanguageId(langName) + " AND PROJECTS.ID = " + getProjectId(projectName) + ") L0 " + 
				"INNER JOIN TOKENS L1 ON L0.TOKENID = L1.ID GROUP BY  TOKEN ORDER BY COUNT DESC");
		stmt.close();
	}
	
	public void analyzeGlobalPerLanguage(String resultTableName, String langName) throws SQLException{
		resultTableName = GLO+resultTableName;
		dropTableIfExists(resultTableName);
		Statement stmt = connection.createStatement();
		stmt.execute("CREATE MEMORY TABLE IF NOT EXISTS \"" + resultTableName + "\" AS " +
				"SELECT TOKEN,COUNT(TOKENID) COUNT FROM " +
				"(SELECT TOKENID,ORDERID FROM OCCURENCES " +
				"INNER JOIN FILES ON OCCURENCES.FILEID = FILES.ID " +
				"INNER JOIN PROJECTS ON PROJECTS.ID = FILES.PROJECTID " +
				"WHERE PROJECTS.LANGUAGEID = " + getLanguageId(langName) + ") L0 " +
				"INNER JOIN TOKENS L1 ON L0.TOKENID = L1.ID GROUP BY  TOKEN ORDER BY COUNT DESC");
				stmt.close();
	}
	
	public void analyzeCoveragePerProject(String resultTableName, String projectName, String langName) throws SQLException{
		resultTableName = COV+resultTableName;
		dropTableIfExists(resultTableName);
		Statement stmt = connection.createStatement();
		stmt.execute("CREATE MEMORY TABLE IF NOT EXISTS \"" + resultTableName + "\" AS " + 
				"SELECT TOKEN, COUNT(FILEID) COUNT FROM " + 
				"(SELECT DISTINCT TOKENID, FILEID, FILES.FILE FROM OCCURENCES  " + 
				"INNER JOIN FILES ON OCCURENCES.FILEID = FILES.ID " + 
				"INNER JOIN PROJECTS ON PROJECTS.ID = FILES.PROJECTID " + 
				"WHERE PROJECTS.LANGUAGEID = " + 
				getLanguageId(langName) + " AND PROJECTS.ID = " + getProjectId(projectName) + ") L0 "  + 
				"INNER JOIN TOKENS L1 ON L0.TOKENID = L1.ID GROUP BY  TOKENID ORDER BY COUNT DESC");		
		stmt.close();
	}
	
	public void analyzeCoveragePerLanguage(String resultTableName,String langName) throws SQLException{
		resultTableName = COV+resultTableName;
		dropTableIfExists(resultTableName);
		Statement stmt = connection.createStatement();
		stmt.execute("CREATE MEMORY TABLE IF NOT EXISTS \"" + resultTableName + "\" AS " + 
				"SELECT TOKEN, COUNT(FILEID) COUNT FROM " + 
				"(SELECT DISTINCT TOKENID, FILEID, FILES.FILE FROM OCCURENCES  " + 
				"INNER JOIN FILES ON OCCURENCES.FILEID = FILES.ID " + 
				"INNER JOIN PROJECTS ON PROJECTS.ID = FILES.PROJECTID " + 
				"WHERE PROJECTS.LANGUAGEID = " + getLanguageId(langName) + ") L0 " + 
				"INNER JOIN TOKENS L1 ON L0.TOKENID = L1.ID GROUP BY  TOKENID ORDER BY COUNT DESC");		
		stmt.close();
	}

	public void analyzeSimpleIndentPerLanguage(String resultTableName,String langName) throws SQLException{		
		resultTableName = SIND+resultTableName;
		dropTableIfExists(resultTableName);
		Statement stmt = connection.createStatement();
		stmt.execute("CREATE MEMORY TABLE IF NOT EXISTS \"" + resultTableName + "\" AS "+
				"SELECT TOKEN, COUNT(TOKENID) COUNT FROM " + 
				"(SELECT TOKENID,ORDERID FROM OCCURENCES " +
				"INNER JOIN FILES ON OCCURENCES.FILEID = FILES.ID " + 
				"INNER JOIN PROJECTS ON PROJECTS.ID = FILES.PROJECTID WHERE PROJECTS.LANGUAGEID = " + 
				getLanguageId(langName) + ") A " +
				"INNER JOIN (SELECT ORDERID FROM OCCURENCES WHERE TOKENID = " + 
				getTokenId("#indent") + ") B ON A.ORDERID = B.ORDERID + 1 " + 
				"INNER JOIN TOKENS ON TOKENS.ID = A.TOKENID " +
				"GROUP BY TOKENID " +
				"ORDER BY COUNT DESC");
		stmt.close();
		System.out.println("STATISTICS: " + calculateStatistics(resultTableName, SIND, langName));
	}
	
	public void analyzeSimpleIndentPerProject(String resultTableName,String langName, String projectName) throws SQLException{	
		resultTableName = SIND+resultTableName;
		dropTableIfExists(resultTableName);
		Statement stmt = connection.createStatement();
		stmt.execute("CREATE MEMORY TABLE IF NOT EXISTS \"" + resultTableName + "\" AS "+
				"SELECT TOKEN, COUNT(TOKENID) COUNT FROM " + 
				"(SELECT TOKENID,ORDERID FROM OCCURENCES " +
				"INNER JOIN FILES ON OCCURENCES.FILEID = FILES.ID " + 
				"INNER JOIN PROJECTS ON PROJECTS.ID = FILES.PROJECTID WHERE PROJECTS.LANGUAGEID = " + 
				getLanguageId(langName) + "AND PROJECTS.ID=" + getProjectId(projectName) + ") A " +
				"INNER JOIN (SELECT ORDERID FROM OCCURENCES WHERE TOKENID = " + getTokenId("#indent") + 
				") B ON A.ORDERID = B.ORDERID + 1 " + 
				"INNER JOIN TOKENS ON TOKENS.ID = A.TOKENID " +
				"GROUP BY TOKENID " +
				"ORDER BY COUNT DESC");
		stmt.close();
	}
	
	public double calculateStatistics(String srcTable,String method, String langName) throws SQLException{
		Statement stmt = connection.createStatement();
		double countAll = getTableCount(ACT+langName);
		ResultSet rs = stmt.executeQuery("SELECT COUNT(SRC.TOKEN) FROM "
				+ "(SELECT TOP " + countAll + " TOKEN FROM \"" + srcTable + "\" ORDER BY COUNT DESC) SRC "
				+ "INNER JOIN \"" + ACT + langName + "\" DST ON SRC.TOKEN = DST.TOKEN" );
		rs.next();
		double count = rs.getInt(1);
		rs.close();
		stmt.close();
		return (double)(count/countAll);
	}
	
	
	/*
	 * DELETE TABLES
	 */
	public void dropTableIfExists(String tableName) throws SQLException{
		Statement stmt = connection.createStatement();
		stmt.execute("DROP TABLE IF EXISTS \"" + tableName + "\""); 
		stmt.close();
	}
	
	public void dropTempTable() throws SQLException{
		Statement stmt = connection.createStatement();
		stmt.execute("DROP TABLE IF EXISTS "+ TEMPORARY); 
		stmt.close();
	}
	
	
	/*
	 * CREATE TABLE
	 */
	private void createTempTable() throws SQLException {
		String query = "CREATE MEMORY TABLE IF NOT EXISTS " + TEMPORARY + " "
				+ "(id MEDIUMINT NOT NULL AUTO_INCREMENT, token VARCHAR(30) NOT NULL);";
		Statement stmt = connection.createStatement();
		stmt.execute(query);
		stmt.close();
	}
	
	private void createFilterTable() throws SQLException {
		String query = "CREATE MEMORY TABLE IF NOT EXISTS " + TEMPFILTER + " "
				+ "(token VARCHAR(30) NOT NULL);";
		Statement stmt = connection.createStatement();
		stmt.execute(query);
		stmt.close();
	}
	
	/*
	 * LOAD REAL TOKEN OF LANGUAGES
	 */
	
	public void newActualTokenFile() throws SQLException, ClassNotFoundException {		
		openConnection();
		dropTableIfExists(TEMPORARY);
		createTempTable();	
		String prepInsertStatementQuery = "INSERT INTO " + TEMPORARY + "(token) VALUES (?)";
		prepInsertStatement = connection.prepareStatement(prepInsertStatementQuery);		
	}
	
	public void actualTokenFileFinished(String resultTableName) throws SQLException{
		resultTableName = ACT + resultTableName;
		dropTableIfExists(resultTableName);		
		Statement stmt = connection.createStatement();
		stmt.execute("CREATE MEMORY TABLE IF NOT EXISTS \"" + resultTableName + "\" AS "+
				"SELECT TOKEN FROM " + TEMPORARY);
		prepInsertStatement.close();
		stmt.close();			
		closeConnection();
	}
	
	/*
	 * ACCESS IDS
	 */
	public int getCurrentFileId(String fileName) throws SQLException {
		Statement stmt = connection.createStatement();
		ResultSet rs = 
						stmt.executeQuery("SELECT TOP 1 ID FROM " + FILE + " WHERE FILE = '" + 
						fileName + "' ORDER BY ID DESC");
		int id = 0;
		
		if (rs.next())
				id = rs.getInt(1);
		else {
			logger.error("OUPS, something is wrong couldnt get currentFileId!");
		}
		
		rs.close();
		stmt.close();
		return id;
	}
	
	public int getTableCount(String tableName) throws SQLException{
		Statement stmt = connection.createStatement();
		ResultSet rs = stmt.executeQuery("SELECT count(*) FROM \"" + tableName + "\"");
		rs.next();
		int count = rs.getInt(1);
		rs.close();
		stmt.close();
		return count;
	}
	
	public int getLanguageId(String languageName) throws SQLException {
		Statement stmt = connection.createStatement();
		ResultSet rs = stmt.executeQuery("SELECT ID FROM " + LANGUAGE + " WHERE FILE = '" + languageName + "'");
		rs.next();
		int languageId = rs.getInt(1);
		rs.close();
		stmt.close();
		return languageId;
	}

	public int getProjectId(String projectName) throws SQLException {
		Statement stmt = connection.createStatement();
		ResultSet rs = stmt.executeQuery("SELECT ID FROM " + PROJECT + " WHERE FILE = '" + projectName + "'");
		rs.next();
		int projectId = rs.getInt(1);
		rs.close();
		stmt.close();
		return projectId;
	}
	
	public int getTokenId(String tokenName) throws SQLException {
		Statement stmt = connection.createStatement();
		ResultSet rs = stmt.executeQuery("SELECT ID FROM " + TOKEN + " WHERE TOKEN = '" + tokenName + "'");
		rs.next();
		int tokenId = rs.getInt(1);
		rs.close();
		stmt.close();
		return tokenId;
	}	
}
