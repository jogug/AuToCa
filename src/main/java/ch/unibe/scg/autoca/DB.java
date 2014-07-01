/*
 ** Copyright 2013 Software Composition Group, University of Bern. All rights reserved.
 */
package ch.unibe.scg.autoca;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles connections to H2DB
 * 
 * @author Joel
 * 
 */
public class DB {

	private static final Logger logger = LoggerFactory.getLogger(DB.class);

	// TODO adjust in statements
	private static final String FILENAME = ".autoca";
	private static final String TOKEN = "tokens";
	private static final String FILE = "files";
	private static final String PROJECT = "projects";
	private static final String TEMPORARY = "token_buffer";
	private static final String OCCURENCE = "occurences";
	private static final String LANGUAGE = "languages";

	private Connection conn;
	private Path database;

	private String insertStatementQuery;

	private PreparedStatement insertStatement;
	
	private int fileId;

	public DB(Path path) throws ClassNotFoundException, SQLException {
		database = path.resolve(FILENAME);
	}

	// TODO Test does the index cause memory ?
	public void initialize() throws SQLException, ClassNotFoundException {
		openConnection();
		
		Statement stmt = conn.createStatement();
		stmt.execute("DROP ALL OBJECTS");
		stmt = conn.createStatement();
		String query = "CREATE MEMORY TABLE IF NOT EXISTS TOKENS ("
				+ "id MEDIUMINT NOT NULL AUTO_INCREMENT, token VARCHAR(30) NOT NULL, " + "PRIMARY KEY (id));"
	
				+ "CREATE TABLE IF NOT EXISTS files ("
				+ "id MEDIUMINT NOT NULL AUTO_INCREMENT, file VARCHAR(100) NOT NULL, "
				+ "projectid MEDIUMINT NOT NULL," + "PRIMARY KEY (id));"
	
				+ "CREATE TABLE IF NOT EXISTS projects ("
				+ "id MEDIUMINT NOT NULL AUTO_INCREMENT, file VARCHAR(100) NOT NULL, "
				+ "languageid MEDIUMINT NOT NULL," + "PRIMARY KEY (id));"
	
				+ "CREATE TABLE IF NOT EXISTS languages ("
				+ "id MEDIUMINT NOT NULL AUTO_INCREMENT, file VARCHAR(100) NOT NULL, " + "PRIMARY KEY (id));"
	
				+ "CREATE TABLE IF NOT EXISTS occurences (" + "tokenid MEDIUMINT NOT NULL, fileid MEDIUMINT NOT NULL, "
				+ "orderid MEDIUMINT NOT NULL AUTO_INCREMENT, PRIMARY KEY (orderid));"
				+ "CREATE INDEX iTOKEN2 ON tokens (TOKEN);";
	
		stmt.execute(query);
		stmt.close();

		createTempTable();
		
		closeConnection();
	}

	//	public int getProjectId(String projectName) throws SQLException {
	//		Statement stmt = conn.createStatement();
	//		ResultSet rs = stmt.executeQuery("SELECT ID FROM " + PROJECT + " WHERE FILE = '" + projectName + "'");
	//		rs.next();
	//		int projectId = rs.getInt(1);
	//		rs.close();
	//		stmt.close();
	//		return projectId;
	//	}
	
		public void newLanguage(String name) throws ClassNotFoundException, SQLException {
			openConnection();
			insertLanguage(name);
			createTempTable();
			
			insertStatementQuery = "INSERT INTO " + TEMPORARY + "(token) VALUES (?)";
			insertStatement = conn.prepareStatement(insertStatementQuery);
		}

	

	/*
	 * SCANMODE
	 */
	
	//	public int getProjectId(String projectName) throws SQLException {
		//		Statement stmt = conn.createStatement();
		//		ResultSet rs = stmt.executeQuery("SELECT ID FROM " + PROJECT + " WHERE FILE = '" + projectName + "'");
		//		rs.next();
		//		int projectId = rs.getInt(1);
		//		rs.close();
		//		stmt.close();
		//		return projectId;
		//	}
		
			public void languageFinished(String name) throws SQLException {
				insertStatement.close();
				closeConnection();
			}

	// TODO Prepared Statement for inserts
	/**
	 * Inserts a token and its fileID into a table, replaces ' with '' for
	 * compatibility
	 * 
	 * @param token
	 * @param table
	 * @param file
	 * @throws SQLException
	 */
	public void newToken(String token) throws SQLException {
		insertStatement.setString(1, token);
		insertStatement.execute();
	}

	public void newProject(String project, int langId) throws SQLException {
		Statement stmt = conn.createStatement();
		stmt.execute("INSERT INTO " + PROJECT + "(file, languageid) VALUES ('" + project + "'," + langId + ")");
		stmt.close();
	}
	
	public void projectFinished() {
		
	}

	public void newFile(String file, int projId) throws SQLException {
		createTempTable();
		
		Statement stmt = conn.createStatement();
		stmt.execute("INSERT INTO " + FILE + "(file, projectid) VALUES ('" + file + "'," + projId + ")");
		fileId = getFileId(file);
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
		deleteTokenBuffer();
	}

	private void openConnection() throws ClassNotFoundException, SQLException {
		if (conn == null) {
			Class.forName("org.h2.Driver");
			conn = DriverManager.getConnection("jdbc:h2:" + database.toString(), "sa", "");
		}
	}

	private void closeConnection() throws SQLException {
		conn.close();
		conn = null;
	}

	private void createTempTable() throws SQLException {
		String query = "CREATE MEMORY TABLE IF NOT EXISTS " + TEMPORARY + " (" + "token VARCHAR(30) NOT NULL" + ");";
		Statement stmt = conn.createStatement();
		stmt.execute(query);
		stmt.close();
	}

	/*
	 * SCANMODE
	 */

	private void insertLanguage(String language) throws SQLException {
		Statement stmt = conn.createStatement();
		stmt.execute("INSERT INTO " + LANGUAGE + "(file) VALUES ('" + language + "')");
		stmt.close();
	}

	private void deleteTokenBuffer() throws SQLException {
		// TODO there are temporary tables
		Statement stmt = conn.createStatement();
		
		
		stmt.executeUpdate("DROP TABLE " + TEMPORARY);
		stmt.close();
	}

	// TODO Fix changes to ScanMode, improve
	private void insertOrderIDs() throws SQLException {
		Statement stmt = conn.createStatement();
		stmt.execute("INSERT INTO "+ OCCURENCE + " ( TOKENID , FILEID) "
				+ "		SELECT SRC.ID, " + fileId + " FROM TOKENS SRC INNER JOIN " + TEMPORARY + "  DST ON SRC.TOKEN = DST.TOKEN ");
	}

	private void assignTokensInTempTableIDs() throws SQLException {
		Statement stmt = conn.createStatement();

		stmt.execute("CREATE INDEX iTOKEN ON " + TEMPORARY + " (TOKEN);");
		
		
		stmt.execute("INSERT INTO "
				+ TOKEN
				+ " ( TOKEN ) SELECT DISTINCT SRC.TOKEN FROM  " + TEMPORARY + "  SRC LEFT OUTER JOIN TOKENS DST ON SRC.TOKEN = DST.TOKEN WHERE DST.TOKEN IS NULL");
		stmt.close();
	}

	/*
	 * ANALYZE MODE
	 */

	public void analyzeGlobal() throws SQLException {
		Statement stmt = conn.createStatement();
		stmt.execute("SELECT L0.*, L1.TOKEN FROM (SELECT TOKENID, COUNT(TOKENID) AS COUNT FROM OCCURENCES GROUP BY TOKENID ORDER BY COUNT(TOKENID) DESC) L0 INNER JOIN TOKENS L1 ON L0.TOKENID = L1.ID");
		stmt.close();
	}

	public void analyzeCoverage() throws SQLException {
		Statement stmt = conn.createStatement();
		stmt.execute("SELECT L1.*, L2.TOKEN FROM (SELECT  L0.TOKENID, COUNT(L0.FILEID) AS COUNT FROM (SELECT DISTINCT TOKENID,FILEID FROM OCCURENCES) L0 GROUP BY L0.TOKENID ORDER BY COUNT(L0.FILEID)  DESC) L1 INNER JOIN ( SELECT ID, TOKEN FROM TOKENS ) L2 ON L1.TOKENID =  L2.ID");
		stmt.close();
	}

	public int getFileId(String fileName) throws SQLException {
		Statement stmt = conn.createStatement();
		ResultSet rs = stmt.executeQuery("SELECT TOP 1 ID FROM " + FILE + " WHERE FILE = '" + fileName + "' ORDER BY ID DESC");
		int id = 0;
		
		if (rs.next())
				id = rs.getInt(1);
		else {
			logger.error("OUPS, something is wrong!");
		}
		
		rs.close();
		stmt.close();
		return id;
	}

//	public int getProjectId(String projectName) throws SQLException {
//		Statement stmt = conn.createStatement();
//		ResultSet rs = stmt.executeQuery("SELECT ID FROM " + PROJECT + " WHERE FILE = '" + projectName + "'");
//		rs.next();
//		int projectId = rs.getInt(1);
//		rs.close();
//		stmt.close();
//		return projectId;
//	}


	/*
	 * public DB(Path path, ArrayList<Weight> weights) throws
	 * ClassNotFoundException, SQLException { Objects.requireNonNull(path);
	 * 
	 * Path database = path.resolve(FILENAME);
	 * 
	 * Class.forName("org.h2.Driver"); conn =
	 * DriverManager.getConnection("jdbc:h2:" + database.toString(), "sa", "");
	 * 
	 * Statement stmt = conn.createStatement();
	 * stmt.execute("DROP ALL OBJECTS");
	 * 
	 * for(Weight i:weights){ stmt = conn.createStatement();
	 * stmt.execute("CREATE TABLE IF NOT EXISTS "+i.getTableName()+
	 * " (token VARCHAR NOT NULL UNIQUE, global INT NOT NULL, coverage INT NOT NULL, current INT NOT NULL)"
	 * ); }
	 * 
	 * //TODO exceptions in weights with multiple tables.. stmt =
	 * conn.createStatement(); stmt.execute(
	 * "CREATE TABLE IF NOT EXISTS nexts (token VARCHAR NOT NULL, next VARCHAR NOT NULL, CONSTRAINT nextskey PRIMARY KEY (token, next))"
	 * );
	 * 
	 * stmt = conn.createStatement(); stmt.execute(
	 * "CREATE TABLE IF NOT EXISTS prevs (token VARCHAR NOT NULL, prev VARCHAR NOT NULL, CONSTRAINT prevskey PRIMARY KEY (token, prev))"
	 * ); }
	 */

	/*
	 * public void newFile(String table) throws SQLException { Statement stmt =
	 * conn.createStatement(); stmt.executeUpdate("UPDATE "+table+
	 * " SET coverage = coverage + 1 WHERE current > 0");
	 * 
	 * stmt = conn.createStatement();
	 * stmt.executeUpdate("UPDATE "+table+" SET current = 0"); }
	 * 
	 * @SuppressWarnings("resource") public void put(String name, String prev,
	 * String table) throws SQLException { Statement stmt =
	 * conn.createStatement(); ResultSet rs =
	 * stmt.executeQuery("SELECT COUNT(*) FROM "+ table + " WHERE token = '" +
	 * name + "'"); rs.next(); if (rs.getInt(1) == 1) { stmt =
	 * conn.createStatement(); stmt.executeUpdate("UPDATE "+ table +
	 * " SET global = global + 1, current = current + 1 WHERE token = '" + name
	 * + "'");
	 * 
	 * if (prev != null) { stmt = conn.createStatement(); rs =
	 * stmt.executeQuery("SELECT COUNT(*) FROM prevs WHERE token = '" + name +
	 * "' AND prev = '" + prev + "'"); rs.next(); if (rs.getInt(1) == 0) { stmt
	 * = conn.createStatement(); stmt.executeUpdate("INSERT INTO prevs VALUES('"
	 * + name + "', '" + prev + "')"); }
	 * 
	 * stmt = conn.createStatement(); rs =
	 * stmt.executeQuery("SELECT COUNT(*) FROM nexts WHERE token = '" + prev +
	 * "' AND next = '" + name + "'"); rs.next(); if (rs.getInt(1) == 0) { stmt
	 * = conn.createStatement(); stmt.executeUpdate("INSERT INTO nexts VALUES('"
	 * + prev + "', '" + name + "')"); } } } else { stmt =
	 * conn.createStatement(); stmt.executeUpdate("INSERT INTO "+ table +
	 * " VALUES('" + name + "', 1, 0, 1)"); } }
	 * 
	 * public void initTable(String name) throws SQLException{ Statement stmt =
	 * conn.createStatement(); stmt.execute("CREATE TABLE IF NOT EXISTS "+name+
	 * " (token VARCHAR NOT NULL UNIQUE, global INT NOT NULL, coverage INT NOT NULL, current INT NOT NULL)"
	 * );
	 * 
	 * }
	 * 
	 * public void deleteTable(String name) throws SQLException{ Statement stmt
	 * = conn.createStatement(); stmt.executeUpdate("DELETE IF EXISTS "+name); }
	 * 
	 * public void initWeightTables(String weightName) throws SQLException{
	 * Statement stmt = conn.createStatement(); stmt = conn.createStatement();
	 * stmt.execute("CREATE TABLE IF NOT EXISTS "+weightName+
	 * "_global (token VARCHAR NOT NULL UNIQUE, global INT NOT NULL, coverage INT NOT NULL, current INT NOT NULL)"
	 * ); stmt = conn.createStatement();
	 * stmt.execute("CREATE TABLE IF NOT EXISTS "+weightName+
	 * "_coverage (token VARCHAR NOT NULL UNIQUE, global INT NOT NULL, coverage INT NOT NULL, current INT NOT NULL)"
	 * ); stmt = conn.createStatement();
	 * stmt.execute("CREATE TABLE IF NOT EXISTS "
	 * +weightName+"_result (global REAL, coverage REAL, precision DECIMAL(10,3))"
	 * ); }
	 * 
	 * public void clearAnalyzeData(String weightName) throws SQLException{
	 * Statement stmt = conn.createStatement();
	 * stmt.executeUpdate("DROP TABLE IF EXISTS "+weightName+"_global"); stmt =
	 * conn.createStatement();
	 * stmt.executeUpdate("DROP TABLE IF EXISTS "+weightName+"_coverage"); stmt
	 * = conn.createStatement();
	 * stmt.executeUpdate("DROP TABLE IF EXISTS "+weightName+"_result"); }
	 * 
	 * 
	 * public void calculateWeightFractions(String weightName, String language)
	 * throws SQLException { Statement stmt = conn.createStatement();
	 * stmt.executeUpdate("INSERT INTO "+weightName+
	 * "_global(token, global, coverage, current) SELECT TOP (SELECT count(*) FROM "
	 * +language+") token, global, coverage, current From "+weightName+
	 * " order by global DESC"); stmt = conn.createStatement();
	 * stmt.executeUpdate("INSERT INTO "+weightName+
	 * "_coverage(token, global, coverage, current) SELECT TOP (SELECT count(*) FROM "
	 * +language+") token, global, coverage, current From "+weightName+
	 * " order by coverage DESC");
	 * 
	 * 
	 * stmt = conn.createStatement();
	 * stmt.executeUpdate("INSERT INTO "+weightName
	 * +"_RESULT( GLOBAL ,PRECISION) VALUES ("+
	 * "SELECT count(*) FROM "+language+
	 * ", "+weightName+"_global WHERE "+language
	 * +".token = "+weightName+"_global.token,"+
	 * "cast(SELECT count(*) FROM "+language
	 * +", "+weightName+"_global WHERE "+language
	 * +".token = "+weightName+"_global.token as REAL)/(SELECT count(*) FROM "
	 * +language+"))"); stmt = conn.createStatement();
	 * stmt.executeUpdate("INSERT INTO "
	 * +weightName+"_RESULT( COVERAGE ,PRECISION) VALUES ("+
	 * "SELECT count(*) FROM "
	 * +language+", "+weightName+"_coverage WHERE "+language
	 * +".token = "+weightName+"_coverage.token,"+
	 * "cast(SELECT count(*) FROM "+language
	 * +", "+weightName+"_coverage WHERE "+language
	 * +".token = "+weightName+"_coverage.token as REAL)/(SELECT count(*) FROM "
	 * +language+"))"); }
	 * 
	 * @SuppressWarnings("resource") public void put(String name, String table)
	 * throws SQLException { Statement stmt = conn.createStatement(); ResultSet
	 * rs = stmt.executeQuery("SELECT COUNT(*) FROM "+ table +
	 * " WHERE token = '" + name + "'"); rs.next(); if (rs.getInt(1) == 1) {
	 * stmt = conn.createStatement(); stmt.executeUpdate("UPDATE "+ table +
	 * " SET global = global + 1, current = current + 1 WHERE token = '" + name
	 * + "'"); } else { stmt = conn.createStatement();
	 * stmt.executeUpdate("INSERT INTO "+ table + " VALUES('" + name +
	 * "', 1, 0, 1)"); } }
	 */
}
