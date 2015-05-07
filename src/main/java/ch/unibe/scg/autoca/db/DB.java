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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.unibe.scg.autoca.config.JSONInterface;
import ch.unibe.scg.autoca.structure.Language;
import ch.unibe.scg.autoca.structure.Project;

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
	private final String RANK;
	private final String PRECISION;
	
	//prefix
	private final String PREFIXSTAT;
	
	//Parser
	private final String NEWLINE;
	private final String INDENT;
	private final String DEDENT;
	private final String DELIMITER;
	private final String COMMENT;
	
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
		RANK = dataset.getRANK();
		PREFIXSTAT = dataset.getPREFIXSTAT();
		PRECISION = dataset.getPRECISION();
		
		NEWLINE = dataset.getDBNEWLINE();
		INDENT = dataset.getINDENT();
		DEDENT = dataset.getDEDENT();
		DELIMITER = dataset.getDELIMITER();
		COMMENT = dataset.getCOMMENT();
		
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

	public void newToken(String token) throws SQLException {
		prepInsertStatement.setString(1, token);
		prepInsertStatement.execute();
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
	 * SCANMODE PROCESS 
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
					+ "SELECT TOKENID, FILEID  FROM \"" + OCCURENCE + "\" "
					+ "WHERE TOKENID != " + indentId + " AND TOKENID != " + dedentId  + " AND TOKENID != " + delimiterId 
					+ " ORDER BY ORDERID ASC");
		stmt.execute("CREATE TABLE IF NOT EXISTS \"" + resultTable + "\" AS " +
				"SELECT TOKENID, PROJECTID, COUNT(TOKENID) COUNT FROM " +
				"(SELECT TOKENID, PROJECTID, ORDERID FROM \"" + TEMPFILTER + "\" OCCURENCES " +
				"INNER JOIN \""+FILE+"\" FILES ON OCCURENCES.FILEID = FILES.ID " +
				"INNER JOIN \""+PROJECT+"\" PROJECTS ON PROJECTS.ID = FILES.PROJECTID WHERE PROJECTS.LANGUAGEID = " + languageId + ") A " +
				"INNER JOIN (SELECT ORDERID FROM \"" + TEMPFILTER + "\" OCCURENCES WHERE TOKENID =  " + newlineId + ") B ON A.ORDERID = B.ORDERID + 1 " +  
				"GROUP BY TOKENID, PROJECTID ORDER BY COUNT DESC");	
		stmt.close();
	}
	
	public void newlineKeywordMethodSaveRam(String languageName, String resultTable) throws SQLException{
		int indentId = getTokenId(INDENT);
		int dedentId = getTokenId(DEDENT);
		int newlineId = getTokenId(NEWLINE);
		int delimiterId = getTokenId(DELIMITER);
		int languageId = getLanguageId(languageName);
		Statement stmt = connection.createStatement();

		int nrOfOccurences = getRowCountOfTable(OCCURENCE);
		int rangeSize = 100000; //TODO MAGIC CONSTANT 

		stmt.execute("CREATE MEMORY TABLE IF NOT EXISTS \"" + TEMPFILTER  + "\" (" 
				+ "id MEDIUMINT AUTO_INCREMENT, "
				+ "TOKENID MEDIUMINT NOT NULL, "
				+ "PROJECTID MEDIUMINT NOT NULL, "
				+ "COUNT MEDIUMINT NOT NULL, "
				+ "PRIMARY KEY (id))");
		for(int curLeft = 1; curLeft < nrOfOccurences; curLeft += rangeSize + 1){
			logger.info("Working on :" + curLeft + " of " + nrOfOccurences);
			//CREATE TEMPFILTER
			stmt.execute("CREATE MEMORY TABLE IF NOT EXISTS \"" + TEMPFILTER+"2"  + "\" (" 
					+ "tokenid MEDIUMINT NOT NULL, "
					+ "fileid MEDIUMINT NOT NULL, "
					+ "orderid MEDIUMINT NOT NULL AUTO_INCREMENT, "
					+ "PRIMARY KEY (orderid))");
			//FETCH RANGE 
			stmt.execute("INSERT INTO \"" + TEMPFILTER + "2" + "\" ( TOKENID , FILEID) "
					+ "SELECT TOKENID, FILEID FROM \"" + OCCURENCE + "\" "
					+ "WHERE (ORDERID BETWEEN " + curLeft + " AND " 
					+ Math.min(curLeft+rangeSize, nrOfOccurences) + ") AND TOKENID != " + indentId + " AND TOKENID != " + dedentId  + " AND TOKENID != " + delimiterId 
					+ " ORDER BY ORDERID ASC");
			//CALCULATE RESULT //TODO SOME OCCURENCES GET dropped if they are at the end of a 100k segment
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
	
	/*
	public void realIndentKeywordMethod(String languageName, String resultTable) throws SQLException{
			int newlineID = getTokenId(NEWLINE);
			int indentID = getTokenId(INDENT);
			int languageID = getLanguageId(languageName);
			int projectid;
			Matcher m;
			Pattern p1 = Pattern.compile("(.*?)(?:\\s*" + newlineID + "\\s*)*(?:" + indentID + ")");
			Pattern p2 = Pattern.compile("([0-9]+)");	
			List<String> projects = new ArrayList<>();
			List<String> matches = new ArrayList<>();
			Statement stmt = connection.createStatement();	
			stmt.execute("CREATE MEMORY TABLE IF NOT EXISTS \"" + TEMPFILTER + "\" " +
					"(id MEDIUMINT NOT NULL AUTO_INCREMENT, tokenid INT NOT NULL, projectid INT NOT NULL, count INT NOT NULL, "
					+ "PRIMARY KEY (id))");

			String prepInsertStatementQuery = "INSERT INTO \"" + TEMPORARY + "\"(tokenid, projectid) VALUES (?,?)";

			
			ResultSet rs = stmt.executeQuery("SELECT * FROM \"" + PROJECT + "\" WHERE languageid = " + languageID);
			while(rs.next()){
				projects.add(rs.getString(2));
			}
			rs.close();			
			
			for(String project: projects){			
				projectid = getProjectId(project);	
				logger.info("Working on project:" + project + "," + projectid +"/"+ projects.size());
				
				//Fetch occurences from DB for project
				rs = stmt.executeQuery("SELECT GROUP_CONCAT(tokenid separator ' ') FROM " +
						 "(SELECT TOKENID, ORDERID FROM \""+OCCURENCE+"\" OCCURENCES " + 
						 "INNER JOIN \""+FILE+"\" FILES ON OCCURENCES.FILEID = FILES.ID WHERE FILES.PROJECTID = " +
						 projectid + "ORDER BY ORDERID ASC)");
				
				rs.next();
				String tokenOcc = rs.getString(1);
				rs.close();				
				
//					Added since its pssible that there are empty projects
				if(tokenOcc!=null){
					stmt.execute("CREATE MEMORY TABLE \"" + TEMPORARY + "\" " +
							"(id MEDIUMINT NOT NULL AUTO_INCREMENT, tokenid INT NOT NULL, projectid INT NOT NULL)");
					prepInsertStatement = connection.prepareStatement(prepInsertStatementQuery);
//					Prepare string for pattern extraction		
					tokenOcc = tokenOcc.replace(""+newlineID, newlineID + " \n");

					
//					Find Patterns	\n <?>	\n indent	
//					TODO FIXABLE couldnt find the solution http://regex101.com/r/gE5dM9/2
//					Pattern p = Pattern.compile("(.+?(?=" + newlineID + "))(?:\\s*" + newlineID + "\\s*)*(?:" + indentID + ")");
					m = p1.matcher(tokenOcc);
				
					while (m.find()) {
						if(!m.group(1).isEmpty()){
							matches.add(m.group(1));
						}
					}
					
					//Find first number on matches insert into tempfilter
									
					int tokenInserted = 0;
					for(String match:matches){
						m = p2.matcher(match);
						if(m.find()){
							prepInsertStatement.setInt(1, Integer.parseInt(m.group(1)));
							prepInsertStatement.setInt(2, projectid);
							prepInsertStatement.execute();
							tokenInserted++;
						}
					}		
				
					System.out.print(project + ", " + projectid + ", tokens inserted:" + tokenInserted + "\n");
					stmt.execute("INSERT INTO \"" + TEMPFILTER + "\"(tokenid, projectid, count) "
							+ "(SELECT tokenid, projectid, COUNT(ID) AS count FROM \"" + TEMPORARY + "\" "
							+ "GROUP BY TOKENID, PROJECTID)");
					//delete stuff used for this project
					matches.clear();
					m.reset();	
					dropTableIfExists(TEMPORARY);
					prepInsertStatement.close();
				}
			}
			
			//finalize 
			
			stmt.execute("CREATE MEMORY TABLE IF NOT EXISTS \"" + resultTable + "\" AS SELECT " +
				 "TOKENID, PROJECTID, COUNT FROM \"" + TEMPFILTER + "\" ORDER BY COUNT DESC");
			
			stmt.execute("ALTER TABLE \"" + resultTable + "\" ADD id INT NOT NULL AUTO_INCREMENT");
			
//			renameTable(TEMPORARY, resultTable);;	
//			
//			
//			stmt.execute("CREATE MEMORY TABLE IF NOT EXISTS \"" + resultTable + "\" AS (SELECT " +
//					 "TOKENID, PROJECTID, COUNT FROM \"" + TEMPFILTER + "\" ORDER BY COUNT DESC)");
			
			prepInsertStatement.close();
			stmt.close();
	}*/
	
	public void realIndentKeywordMethod(String languageName, String resultTable) throws SQLException{
		int newlineID = getTokenId(NEWLINE);
		int indentID = getTokenId(INDENT);
		int dedentID = getTokenId(DEDENT);
		int delimID = getTokenId(DELIMITER);
		int commentID = getTokenId(COMMENT);
		int languageID = getLanguageId(languageName);
		int savePos;
		PreparedStatement fetchFilePrepStatement;
		ResultSet rs;
		List<String> projects = new ArrayList<>();
		List<Integer> files;
		List<Integer> keywords;
		Statement stmt = connection.createStatement();	
		stmt.execute("CREATE MEMORY TABLE IF NOT EXISTS \"" + TEMPFILTER + "\" " +
				"(id MEDIUMINT NOT NULL AUTO_INCREMENT, tokenid INT NOT NULL, projectid INT NOT NULL, count INT NOT NULL, "
				+ "PRIMARY KEY (id))");

		String prepInsertStatementQuery = "INSERT INTO \"" + TEMPORARY + "\"(tokenid, projectid) VALUES (?,?)";
		
		String prepFetchFileStatementQuery = "SELECT * FROM \"" + OCCURENCE + "\" WHERE fileid = ? AND tokenid !=" + delimID + "AND tokenid !=" + dedentID + "AND tokenid !=" + commentID;
		fetchFilePrepStatement = connection.prepareStatement(prepFetchFileStatementQuery,
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
			rs = stmt.executeQuery("SELECT * FROM \"" + FILE + "\" WHERE projectid = " + projectId);
			files = new ArrayList<Integer>();
			while(rs.next()){
				files.add(rs.getInt(1));
			}
			rs.close();
			int i = 1;		
			for(Integer fileId: files){							//In each File
				System.out.print(i++ +",");
				if((i % 31) ==0){
					System.out.println();
				}
				keywords = new ArrayList<Integer>();			//stores found Keywords
				stmt.execute("CREATE MEMORY TABLE \"" + TEMPORARY + "\" " +
						"(id MEDIUMINT NOT NULL AUTO_INCREMENT, tokenid INT NOT NULL, projectid INT NOT NULL)");
				
				fetchFilePrepStatement.setInt(1, fileId);		//Fetch file in DB
				rs = fetchFilePrepStatement.executeQuery();
				

				int previousIndPos = 0;
				while(rs.next()){								//Search through file
					//System.out.println(rs.getInt(1) + ","+i++ + "," + rs.getInt(3));
					if(rs.getInt(1) == indentID){				//Find Indents
						savePos = rs.getRow(); 					//Save Row position to later skip back to it
						
						int newlCount = 0;
						int newl2Pos = 0;
						boolean stage1 = false;
						boolean check = false;
						boolean found  = false;
						while(!found){ //find 2/n$
							//System.out.println(rs.getInt(1));
							if(!rs.previous()||rs.getInt(3)<previousIndPos-1){
								rs.absolute(savePos);
								break;
							}
							//System.out.println(rs.getInt(1));
							//Search the file backwards for newlines
							if(rs.getInt(1)==newlineID){
								if(!stage1){					//Stage 0: find 2 newlines for a check
									newlCount++;
									if(newlCount==2){
										newl2Pos = rs.getInt(3);
										stage1 = true;
										check = true;	
									}
								}else {							//Stage 1: only find 1 newline for a check
									newl2Pos = rs.getInt(3);
									check = true;
								}
							}
							//System.out.println("oID" + rs.getInt(3));
							if(check){								//Check try to find token
								rs.next();
								//System.out.println("nlP2 " + newl2Pos + ", nlP1 " + newl1Pos+ " , " + rs.getInt(3));
								if(rs.getInt(1)==newlineID){	
									if(rs.getInt(3)==newl2Pos){
										found = true;
										rs.absolute(savePos);
									}else{
										rs.previous();					//found empty line keep going back
										check = false;					//keep on looking
									}
								}else if(rs.getInt(1)==indentID){
									if(rs.getInt(3)==previousIndPos){ //previous Indent found -> break
										rs.absolute(savePos);
										found = true;
										break;
									}else{
										rs.next();						//skip indents and comments while searching for first on newline
									}
								}else{								// Found a possible token
									keywords.add(rs.getInt(1));
									found = true;
									rs.absolute(savePos);			//jump back			
								}
							}
							//System.out.println(rs.getInt(1));
						}
						previousIndPos = rs.getInt(3);
					}
				}
				prepInsertStatement = connection.prepareStatement(prepInsertStatementQuery);
				for(Integer keyword: keywords){
					prepInsertStatement.setInt(1, keyword);
					prepInsertStatement.setInt(2, projectId);
					prepInsertStatement.execute();
				}
				//Finalize File
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


	public void intersectLanguageProjects(String languageName, String resultTable, int minOccInProjects) throws SQLException{
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
				"(SELECT DISTINCT TOKENID FROM \""+OCCURENCE+"\" OCCURENCES " +
				"INNER JOIN \""+FILE+"\" FILES ON OCCURENCES.FILEID = FILES.ID " +
				"INNER JOIN \""+PROJECT+"\" PROJECTS ON PROJECTS.ID = FILES.PROJECTID WHERE PROJECTS.LANGUAGEID = " + getLanguageId(langName) +")")
				-lowestFoundToken;

		stmt.execute("INSERT INTO \""+ RESULTTABLE + "\"(filter,TP, FP, TN, FN, TOKnotInSET) VALUES ('"+resultTable+"',"+tp+","+fp+","+tn+","+fn+","+tnip+")");

		stmt.close();
	}	
	
	/*
	 * SUMMARY
	 */
	public void newSummary() throws ClassNotFoundException, SQLException {
		openConnection();
	}

	public void SummaryFinished() throws SQLException {
		closeConnection();
	}	
	
	
	public void createPrecisionTable(String language) throws SQLException {
		Statement stmt = connection.createStatement();
		int nrOfKeywords = getRowCountOfTable(language);
		String query = "CREATE TABLE IF NOT EXISTS \"" + PRECISION+ language + "\" ("
				+ "FILTER VARCHAR(30) ";
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
	
	
	public void summarizeRanks(String language, List<String> filterChains) throws SQLException {
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
	
	/*
	 * LOAD REAL TOKEN OPEN&CLOSE
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
				+ "(id MEDIUMINT NOT NULL AUTO_INCREMENT, token VARCHAR(30) NOT NULL);";
		Statement stmt = connection.createStatement();
		stmt.execute(query);
		stmt.close();
	}
	
/**
 * Rename table from currentName to newName
 * @param currentName
 * @param newName
 * @throws SQLException
 */
	public void renameTable(String currentName, String newName) throws SQLException{
		Statement stmt = connection.createStatement();
		//stmt.execute("RENAME TABLE \"" + currentName + "\" TO \"" + newName + "\"");
		stmt.execute("ALTER TABLE \"" + currentName + "\" RENAME TO \"" + newName + "\"");
		stmt.close();
	}
	
	/*
	 * COMMON ID'S ACCESSED
	 */
	
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
		rs.next();
		int tokenId = rs.getInt(1);
		rs.close();
		stmt.close();
		return tokenId;
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
	
}
