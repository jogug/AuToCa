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
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.unibe.scg.autoca.JSONInterface;

/**
 * Handles connections and code links to H2DB.
 * 
 * @author Joel
 * 
 */

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
	public	final String OCCURENCE;
	private final String TOKEN;
	private final String FILE;
	private final String PROJECT;
	private final String LANGUAGE;
	private final String RESULTTABLE;
	
	//Parser
	private final String NEWLINE;
	private final String INDENT;

	
	private Connection connection;
	private Path pathDb;
	private PreparedStatement prepInsertStatement;	
	private int currentFileId;

	public DB(Path path, JSONInterface dataset) throws ClassNotFoundException, SQLException {

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
		RESULTTABLE = dataset.getRESULTTABLE();
		
		NEWLINE = dataset.getDBNEWLINE();
		INDENT = dataset.getINDENT();
		
		pathDb = path.resolve(FILENAME);
	}

	public void initialize() throws SQLException, ClassNotFoundException {
		openConnection();
		
		Statement stmt = connection.createStatement();
		stmt.execute("DROP ALL OBJECTS");
		stmt = connection.createStatement();
		String query = 
				"CREATE MEMORY TABLE IF NOT EXISTS \"" + TOKEN + "\" ("
				+ "id MEDIUMINT NOT NULL AUTO_INCREMENT, "
				+ "token VARCHAR(30) NOT NULL, "
				+ "PRIMARY KEY (id));"
	
				+ "CREATE TABLE IF NOT EXISTS \"" + FILE + "\" ("
				+ "id MEDIUMINT NOT NULL AUTO_INCREMENT, "
				+ "file VARCHAR(100) NOT NULL, "
				+ "projectid MEDIUMINT NOT NULL,"
				+ "PRIMARY KEY (id));"
	
				+ "CREATE TABLE IF NOT EXISTS \"" + PROJECT + "\" ("
				+ "id MEDIUMINT NOT NULL AUTO_INCREMENT, "
				+ "file VARCHAR(100) NOT NULL, "
				+ "languageid MEDIUMINT NOT NULL," 
				+ "PRIMARY KEY (id));"
	
				+ "CREATE TABLE IF NOT EXISTS \"" + LANGUAGE + "\" ("
				+ "id MEDIUMINT NOT NULL AUTO_INCREMENT, "
				+ "file VARCHAR(100) NOT NULL, " 
				+ "PRIMARY KEY (id));"
	
				+ "CREATE TABLE IF NOT EXISTS \"" + OCCURENCE + "\" (" 
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

	public void newLanguage(String name) throws ClassNotFoundException, SQLException {
		openConnection();
		insertLanguage(name);
		createTempTable();
		
		String prepInsertStatementQuery = "INSERT INTO \"" + TEMPORARY + "\"(token) VALUES (?)";
		prepInsertStatement = connection.prepareStatement(prepInsertStatementQuery);
	}

	public void languageFinished() throws SQLException {
		dropTableIfExists(TEMPORARY);
		prepInsertStatement.close();
		closeConnection();
	}
	
	public void newProject(String project, int langId) throws SQLException {
		Statement stmt = connection.createStatement();
		stmt.execute("INSERT INTO \"" + PROJECT + "\"(file, languageid) VALUES ('" + project + "'," + langId + ")");
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
		stmt.execute("INSERT INTO \"" + FILE + "\"(file, projectid) VALUES ('" + file + "'," + projId + ")");
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
	 * SCANMODE 
	 */

	private void insertLanguage(String language) throws SQLException {
		Statement stmt = connection.createStatement();
		stmt.execute("INSERT INTO \"" + LANGUAGE + "\"(file) VALUES ('" + language + "')");
		stmt.close();
	}

	private void insertOrderIDs() throws SQLException {
		Statement stmt = connection.createStatement();
		stmt.execute("INSERT INTO \"" + OCCURENCE + "\" ( TOKENID , FILEID) "
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
	 * FILTER MODE OPEN&CLOSE
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
	 * FILTER MODE
	 */
	public void globalKeywordMethod(String languageName, String resultTable) throws SQLException{
		Statement stmt = connection.createStatement();
		stmt.execute("CREATE MEMORY TABLE IF NOT EXISTS \"" + resultTable + "\" AS " +
				"SELECT TOKENID, PROJECTID, COUNT(TOKENID) COUNT FROM " +
				"(SELECT TOKENID,PROJECTID FROM \""+OCCURENCE+"\" OCCURENCES " + 
				"INNER JOIN \""+FILE+"\" FILES ON OCCURENCES.FILEID = FILES.ID " + 
				"INNER JOIN \""+PROJECT+"\" PROJECTS ON PROJECTS.ID = FILES.PROJECTID  " +
				"WHERE PROJECTS.LANGUAGEID = " + getLanguageId(languageName) + ") " +
				"GROUP BY TOKENID, PROJECTID ORDER BY COUNT DESC");	
		stmt.close();
	}
	
	public void coverageKeywordMethod(String languageName, String resultTable) throws SQLException{
		Statement stmt = connection.createStatement();
		stmt.execute("CREATE MEMORY TABLE IF NOT EXISTS \"" + resultTable + "\" AS " +
				"SELECT TOKENID, PROJECTID, COUNT(FILEID) COUNT FROM  " +
				"(SELECT DISTINCT TOKENID, PROJECTID, FILEID, FILES.FILE FROM \""+OCCURENCE+"\" OCCURENCES " +   
				"INNER JOIN \""+FILE+"\" FILES ON OCCURENCES.FILEID = FILES.ID " +
				"INNER JOIN \""+PROJECT+"\" PROJECTS ON PROJECTS.ID = FILES.PROJECTID " +  
				"WHERE PROJECTS.LANGUAGEID = " + getLanguageId(languageName) + ") " +
				"GROUP BY  TOKENID, PROJECTID ORDER BY COUNT DESC");	
		stmt.close();
	}
	

	public void indentKeywordMethod(String languageName, String resultTable) throws SQLException{
		int indentId = getTokenId(INDENT);
		int languageId = getLanguageId(languageName);
		Statement stmt = connection.createStatement();
		stmt.execute("CREATE MEMORY TABLE IF NOT EXISTS \"" + resultTable + "\" AS " +
				"SELECT TOKENID, PROJECTID, COUNT(TOKENID) COUNT FROM " +
				"(SELECT TOKENID, PROJECTID, ORDERID FROM \""+OCCURENCE+"\" OCCURENCES " +
				"INNER JOIN \""+FILE+"\" FILES ON OCCURENCES.FILEID = FILES.ID " +
				"INNER JOIN \""+PROJECT+"\" PROJECTS ON PROJECTS.ID = FILES.PROJECTID WHERE PROJECTS.LANGUAGEID = " + languageId + ") A " +
				"INNER JOIN (SELECT ORDERID FROM \""+OCCURENCE+"\" OCCURENCES WHERE TOKENID =  " + indentId + ") B ON A.ORDERID = B.ORDERID + 1 " +  
				"GROUP BY TOKENID, PROJECTID ORDER BY COUNT DESC");	
		stmt.close();
	}
	
	public void newlineKeywordMethod(String languageName, String resultTable) throws SQLException{
		int indentId = getTokenId(INDENT);
		int newlineId = getTokenId(NEWLINE);
		int languageId = getLanguageId(languageName);
		Statement stmt = connection.createStatement();
		stmt.execute("CREATE MEMORY TABLE IF NOT EXISTS \"" + TEMPFILTER + "\" AS " +
				"SELECT * FROM \"" + OCCURENCE + "\"");
		stmt.execute("DELETE FROM \"" + TEMPFILTER +"\" WHERE TOKENID = " + indentId);
		stmt.execute("CREATE MEMORY TABLE IF NOT EXISTS \"" + resultTable + "\" AS " +
				"SELECT TOKENID, PROJECTID, COUNT(TOKENID) COUNT FROM " +
				"(SELECT TOKENID, PROJECTID, ORDERID FROM \""+OCCURENCE+"\" OCCURENCES " +
				"INNER JOIN \""+FILE+"\" FILES ON OCCURENCES.FILEID = FILES.ID " +
				"INNER JOIN \""+PROJECT+"\" PROJECTS ON PROJECTS.ID = FILES.PROJECTID WHERE PROJECTS.LANGUAGEID = " + languageId + ") A " +
				"INNER JOIN (SELECT ORDERID FROM \""+OCCURENCE+"\" OCCURENCES WHERE TOKENID =  " + newlineId + ") B ON A.ORDERID = B.ORDERID + 1 " +  
				"GROUP BY TOKENID, PROJECTID ORDER BY COUNT DESC");	
		stmt.close();
	}
	
	public void realIndentKeywordMethod(String languageName, String resultTable) throws SQLException{
			int newlineID = getTokenId(NEWLINE);
			int indentID = getTokenId(INDENT);
			int languageID = getLanguageId(languageName);
			int projectid;
			List<String> projects = new ArrayList<>();
			List<String> matches = new ArrayList<>();
			Statement stmt = connection.createStatement();	
			dropTableIfExists(TEMPFILTER); //TODO DELETE THIS LINE 
			stmt.execute("CREATE MEMORY TABLE IF NOT EXISTS \"" + TEMPFILTER + "\" " +
					"(id MEDIUMINT NOT NULL AUTO_INCREMENT, tokenid INT NOT NULL, projectid INT NOT NULL)");
			String prepInsertStatementQuery = "INSERT INTO \"" + TEMPFILTER + "\"(tokenid, projectid) VALUES (?,?)";
			prepInsertStatement = connection.prepareStatement(prepInsertStatementQuery);
			
			ResultSet rs = stmt.executeQuery("SELECT * FROM \"" + PROJECT + "\" WHERE languageid = " + languageID);
			while(rs.next()){
				projects.add(rs.getString(2));
			}
			System.out.println("Projects finished: ");
			for(String project: projects){
				projectid = getProjectId(project);	
				System.out.print(projectid+",");
				
				//Fetch occurences from DB for project
				rs = stmt.executeQuery("SELECT GROUP_CONCAT(tokenid separator ' ') FROM " +
						 "(SELECT TOKENID, ORDERID FROM \""+OCCURENCE+"\" OCCURENCES " + 
						 "INNER JOIN \""+FILE+"\" FILES ON OCCURENCES.FILEID = FILES.ID WHERE FILES.PROJECTID = " +
						 projectid + "ORDER BY ORDERID ASC)");
				
				rs.next();
				String tokenOcc = rs.getString(1);
				rs.close();							
				
				//Prepare string for pattern extraction		
				tokenOcc = tokenOcc.replace(""+newlineID, newlineID + " \n");
				Pattern p = Pattern.compile("(.*?)(?:\\s*" + newlineID + "\\s*)*(?:" + indentID + ")");
				
				//Find Patterns	\n <?>	\n indent	
				//TODO FIXABLE couldnt find the solution http://regex101.com/r/gE5dM9/2
				//Pattern p = Pattern.compile("(.+?(?=" + newlineID + "))(?:\\s*" + newlineID + "\\s*)*(?:" + indentID + ")");				
				Matcher m = p.matcher(tokenOcc);
				
				while (m.find()) {
					if(!m.group(1).isEmpty()){
						matches.add(m.group(1));
					}
				}
				
				//Find first number on matches save in tempfilter
				p = Pattern.compile("([0-9]+)");					

					for(String match:matches){
						m = p.matcher(match);
						if(m.find()){
							prepInsertStatement.setInt(1, Integer.parseInt(m.group(1)));
							prepInsertStatement.setInt(2, projectid);
							prepInsertStatement.execute();
						}
					}			
					
				//finalize project
				stmt.execute("CREATE MEMORY TABLE IF NOT EXISTS \"" + TEMPORARY + "\" AS SELECT " +
					 "TOKENID, PROJECTID, COUNT(ID) AS COUNT FROM \"" + TEMPFILTER + 
					"\" GROUP BY TOKENID, PROJECTID");
				dropTableIfExists(TEMPFILTER);
				stmt.execute("ALTER TABLE \"" + TEMPORARY + "\" ADD id INT NOT NULL AUTO_INCREMENT");
				renameTable(TEMPORARY, TEMPFILTER);
				dropTableIfExists(TEMPORARY);
			}
			stmt.execute("CREATE MEMORY TABLE IF NOT EXISTS \"" + resultTable + "\" AS (SELECT " +
					 "TOKENID, PROJECTID, COUNT FROM \"" + TEMPFILTER + "\" ORDER BY COUNT DESC)");
			prepInsertStatement.close();
			stmt.close();
	}
	

	
	/**
	 * 
	 * @param languageName
	 * @param resultTable
	 * @param if occurs in less than occInProj , removed 
	 * @throws SQLException
	 */
	public void intersectLanguageProjects(String languageName, String resultTable, int occInProj) throws SQLException{
		Statement stmt = connection.createStatement();
		stmt.execute("CREATE MEMORY TABLE IF NOT EXISTS \"" + TEMPFILTER + "\" AS " +
				"SELECT TOKENID, COUNT(PROJECTID) COUNT FROM \"" + resultTable + "\" " +
				"GROUP BY TOKENID ORDER BY COUNT DESC;" +
				
				"DELETE FROM \"" + TEMPFILTER + "\" WHERE COUNT < " + occInProj + "; " +// TODO CONSTANT 

				"ALTER TABLE \"" + resultTable + "\" RENAME TO \"" + TEMPORARY + "\" ; " +
				
				"CREATE MEMORY TABLE IF NOT EXISTS \"" + resultTable + "\" AS " +
				"SELECT A.TOKENID, PROJECTID, A.COUNT FROM \"" + TEMPORARY + "\" A " +
				"INNER JOIN \"" + TEMPFILTER + "\" B ON A.TOKENID = B.TOKENID");	
		stmt.close();
	}
	
	public void upperCaseRemoval(String languageName, String resultTable) throws SQLException{
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
	
	public void nameOrderToken(String languageName, String resultTable) throws SQLException{
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
	
	public void specialSubRemoval(String languageName, String resultTable, String substring) throws SQLException{
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

	public void newAnalyzeLanguage() throws ClassNotFoundException, SQLException {
		openConnection();
	}

	public void analyzeLanguageFinished() throws SQLException {
		closeConnection();
	}	
	
	// TODO OUTPUT
//	//For each pattern extract Token	
//	try {
//        BufferedWriter out = new BufferedWriter(new FileWriter("../AuToCa/resources/output.txt", false));
//        	
//            for (String match: matches) {
//                out.write(match + " ");
//            }
//            out.close();
//    } catch (IOException e) {}	
	
//	public double calculateStatistics(String langName, String srcTable) throws SQLException{
//		Statement stmt = connection.createStatement();
//		double countAllRelevant = getTableCount(langName);
//		double countFoundAll = countAllRelevant; 	//TODO CountFoundAll
//		ResultSet rs = stmt.executeQuery("SELECT COUNT(SRC.TOKEN) FROM "
//				+ "(SELECT TOP " + countFoundAll + " TOKEN FROM \"" + srcTable + "\" ORDER BY COUNT DESC) SRC "
//				+ "INNER JOIN \"" +  langName + "\" DST ON SRC.TOKEN = DST.TOKEN" );
//		rs.next();
//		double count = rs.getInt(1);
//		rs.close();
//		stmt.execute("INSERT INTO \"" + RESULTTABLE + "\"(filter, precision, recall) VALUES ('" + 
//					 srcTable + "'," + 
//					 (double)(count/countFoundAll) + "," + 
//					 (double)(count/countAllRelevant)+ ")");
//		stmt.close();
//		return (double)(count/countAllRelevant);
//	}
	
	public void calculateStatisticsPerLanguage(String langName, String srcTable, String resultTable) throws SQLException{
		Statement stmt = connection.createStatement();	
		double countAllRelevant = getTableCount(langName);
		double sumToken = getCountSum(srcTable);
		stmt.execute("CREATE MEMORY TABLE IF NOT EXISTS \"" + TEMPFILTER + "\" " +
					 "(id MEDIUMINT NOT NULL AUTO_INCREMENT, tokenid INT NOT NULL, count INT NOT NULL)");
		stmt.execute("INSERT INTO \"" + TEMPFILTER + "\" (tokenid,count) " +
					 "SELECT tokenid, SUM(count) as count FROM \""+ srcTable + "\" GROUP BY TOKENID ORDER BY COUNT DESC");
		//stmt.execute("ALTER TABLE \"" + srcTable + "\" ADD id INT NOT NULL AUTO_INCREMENT");
		
		stmt.execute("CREATE MEMORY TABLE IF NOT EXISTS \"" + resultTable + "\" AS "+
					"(SELECT a.TOKEN, COUNT, b.ID FROM \"" +  langName + "\" a "+
					"LEFT JOIN " + 
					"(SELECT TOKENID, SUM(COUNT) AS COUNT, ID FROM \""+ TEMPFILTER + "\" "+
					"GROUP BY TOKENID) b " +
					"ON a.ID = b.TOKENID ORDER BY COUNT DESC)");
		
		// GET TP TN FN FP, TOP FOUND
		
		//FALSE NEGATIVES TODO
//		stmt.execute("SELECT a.TOKEN, COUNT, ID AS ORDERID FROM \"" + srcTable + "\"  a " +
//					"WHERE a.TOKEN NOT IN (SELECT TOKEN FROM \"" +  langName + "\")");
		//stmt.execute("ALTER TABLE \"" + srcTable + "\" DROP COLUMN ID");
		stmt.close();
	}
	
	/*
	 * DELETE TABLES
	 */
	public void dropTableIfExists(String tableName) throws SQLException{
		Statement stmt = connection.createStatement();
		stmt.execute("DROP TABLE IF EXISTS \"" + tableName + "\""); 
		stmt.close();
	}	
	
	/*
	 * CREATE TABLE
	 */
	public void createResulttable() throws SQLException {
		Statement stmt = connection.createStatement();
		stmt.execute("CREATE TABLE IF NOT EXISTS \"" + RESULTTABLE + "\" ("
				+ "filter VARCHAR(100) NOT NULL, "
				+ "TP DECIMAL NOT NULL,"
				+ "FN DECIMAL NOT NULL,"
				+ "PRIMARY KEY (filter));");
		stmt.close();
	}
	
	private void createTempTable() throws SQLException {
		String query = "CREATE MEMORY TABLE IF NOT EXISTS \"" + TEMPORARY + "\" "
				+ "(id MEDIUMINT NOT NULL AUTO_INCREMENT, token VARCHAR(30) NOT NULL);";
		Statement stmt = connection.createStatement();
		stmt.execute(query);
		stmt.close();
	}
	
	/*
	 * RENAME TABLE
	 */
	
	public void renameTable(String currentName, String newName) throws SQLException{
		Statement stmt = connection.createStatement();
		//stmt.execute("RENAME TABLE \"" + currentName + "\" TO \"" + newName + "\"");
		stmt.execute("ALTER TABLE \"" + currentName + "\" RENAME TO \"" + newName + "\"");
		stmt.close();
	}
	
	/*
	 * LOAD REAL TOKEN OF LANGUAGES
	 */
	
	public void newActualTokenFile() throws SQLException, ClassNotFoundException {		
		openConnection();
		dropTableIfExists(TEMPORARY);
		createTempTable();	
		String prepInsertStatementQuery = "INSERT INTO \"" + TEMPORARY + "\"(token) VALUES (?)";
		prepInsertStatement = connection.prepareStatement(prepInsertStatementQuery);		
	}

	public void actualTokenFileFinished(String resultTableName) throws SQLException{
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
	 * ACCESS IDS
	 */
	public int getCurrentFileId(String fileName) throws SQLException {
		Statement stmt = connection.createStatement();
		ResultSet rs = 
						stmt.executeQuery("SELECT TOP 1 ID FROM \"" + FILE + "\" WHERE FILE = '" + 
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
	
	public int getCountSum(String tableName) throws SQLException{
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
		rs.next();
		int tokenId = rs.getInt(1);
		rs.close();
		stmt.close();
		return tokenId;
	}	
}
