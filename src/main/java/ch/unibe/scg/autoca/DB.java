/*
** Copyright 2013 Software Composition Group, University of Bern. All rights reserved.
*/
package ch.unibe.scg.autoca;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles connections to H2DB
 * @author Joel
 *
 */
public class DB implements Closeable {

    private static final Logger logger = LoggerFactory.getLogger(DB.class);
    private static final String FILENAME = ".lexica";

    private final Connection conn;
    
    public DB(Path path) throws ClassNotFoundException, SQLException {
        Objects.requireNonNull(path);

        Path database = path.resolve(FILENAME);
          
        Class.forName("org.h2.Driver");
        conn = DriverManager.getConnection("jdbc:h2:" + database.toString(), "sa", "");       
        
    }
    
    //TODO
    // 1 DB for 1 Language
    public void initialize() throws SQLException{
    	Statement stmt = conn.createStatement();
        stmt.execute("DROP ALL OBJECTS");
        stmt = conn.createStatement();
    	String query = 	"CREATE TABLE IF NOT EXISTS tokens ("
    					+ "id MEDIUMINT NOT NULL AUTO_INCREMENT, token VARCHAR(30) NOT NULL, "
    					+ "PRIMARY KEY (id));"
    					+ "CREATE TABLE IF NOT EXISTS files ("
    					+ "id MEDIUMINT NOT NULL AUTO_INCREMENT, file VARCHAR(100) NOT NULL, "
    					+ "PRIMARY KEY (id));"
    					+ "CREATE TABLE IF NOT EXISTS projects ("
    					+ "id MEDIUMINT NOT NULL AUTO_INCREMENT, file VARCHAR(100) NOT NULL, "
    					+ "PRIMARY KEY (id));"
    					+ "CREATE TABLE IF NOT EXISTS token_buffer ("
    					+ "token VARCHAR(30) NOT NULL, file VARCHAR(100) NOT NULL);"
    					+ "CREATE TABLE IF NOT EXISTS occurences ("
    					+ "tokenid MEDIUMINT NOT NULL, fileid MEDIUMINT NOT NULL, "
    					+ "orderid MEDIUMINT NOT NULL AUTO_INCREMENT, PRIMARY KEY (orderid));"
    					+ "CREATE INDEX iTOKEN ON token_buffer (TOKEN);"
    					+ "CREATE INDEX iTOKEN2 ON tokens (TOKEN);";
    	stmt.execute(query);
    }
    
    /**
     * Deletes records from a specified table
     * @param table
     * @throws SQLException
     */
    public void deleteRecords(String table) throws SQLException{
     	Statement stmt = conn.createStatement();
    	stmt.executeUpdate("DELETE FROM "+table);   	
    }
	
    /**
     * Inserts a token into a table, replaces ' with '' for compatibility
     * 
     * @param token
     * @param table
     * @param file
     * @throws SQLException
     */
	public void insertToken(String token, String table, String file) throws SQLException{
		Statement stmt = conn.createStatement();
		stmt.execute("INSERT INTO "+table+"(token, file) SELECT '"+token+"' AS TOKEN, '"+file+"' AS FILE");
	}
	
	/**
	 * Inserts a Objectname into a table, assigns it an ID
	 * 
	 * @param file
	 * @param table
	 * @throws SQLException
	 */
	public void insertObjectName(String file, String table) throws SQLException{
		Statement stmt = conn.createStatement();
		stmt.execute("INSERT INTO "+table+"(file) SELECT '"+file+"' AS FILE");
	}
	


	public void insertOrderIDs() throws SQLException {
		Statement stmt = conn.createStatement();
		stmt.execute("INSERT INTO OCCURENCES ( TOKENID , FILEID ) SELECT SRC.ID, L0.ID FROM TOKENS SRC INNER JOIN TOKEN_BUFFER DST ON SRC.TOKEN = DST.TOKEN CROSS JOIN (SELECT TOP 1 ID FROM files ORDER BY ID DESC) L0");		
	}	
	
	public void assignTokensInTempTableIDs() throws SQLException{
		Statement stmt = conn.createStatement();
		stmt.execute("INSERT INTO TOKENS ( TOKEN ) SELECT DISTINCT SRC.TOKEN FROM TOKEN_BUFFER SRC LEFT OUTER JOIN TOKENS DST ON SRC.TOKEN = DST.TOKEN WHERE DST.TOKEN IS NULL");
	}

    //ANALYZE MODE
    public void analyzeGlobal() throws SQLException{
    	Statement stmt = conn.createStatement();
    	stmt.execute("SELECT L0.*, L1.TOKEN FROM (SELECT TOKENID, COUNT(TOKENID) AS COUNT FROM OCCURENCES GROUP BY TOKENID ORDER BY COUNT(TOKENID) DESC) L0 INNER JOIN TOKENS L1 ON L0.TOKENID = L1.ID");
    }
    
    public void analyzeCoverage() throws SQLException{
    	Statement stmt = conn.createStatement();
    	stmt.execute("SELECT L1.*, L2.TOKEN FROM (SELECT  L0.TOKENID, COUNT(L0.FILEID) AS COUNT FROM (SELECT DISTINCT TOKENID,FILEID FROM OCCURENCES) L0 GROUP BY L0.TOKENID ORDER BY COUNT(L0.FILEID)  DESC) L1 INNER JOIN ( SELECT ID, TOKEN FROM TOKENS ) L2 ON L1.TOKENID =  L2.ID");   	
    }
    
    @Override
    public void close() throws IOException {
        if (conn != null) {
            try {
                conn.close();
            } catch (SQLException e) {
                logger.warn("Cannot close database connection", e);
            }
        }
    }
	
    public void print() throws SQLException {
    	//TODO
    }

	public int getProjectID(String projectName) throws SQLException {
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery("SELECT ID FROM "+ "projects" + " WHERE FILE = '" + projectName + "'");
        rs.next();
		return rs.getInt(1);
	}
    
    
    /*
    public DB(Path path, ArrayList<Weight> weights) throws ClassNotFoundException, SQLException {
        Objects.requireNonNull(path);

        Path database = path.resolve(FILENAME);
          
        Class.forName("org.h2.Driver");
        conn = DriverManager.getConnection("jdbc:h2:" + database.toString(), "sa", "");
             
        Statement stmt = conn.createStatement();
        stmt.execute("DROP ALL OBJECTS");

        for(Weight i:weights){
            stmt = conn.createStatement();
            stmt.execute("CREATE TABLE IF NOT EXISTS "+i.getTableName()+" (token VARCHAR NOT NULL UNIQUE, global INT NOT NULL, coverage INT NOT NULL, current INT NOT NULL)");
        }
        
        //TODO exceptions in weights with multiple tables..
        stmt = conn.createStatement();
        stmt.execute("CREATE TABLE IF NOT EXISTS nexts (token VARCHAR NOT NULL, next VARCHAR NOT NULL, CONSTRAINT nextskey PRIMARY KEY (token, next))");

        stmt = conn.createStatement();
        stmt.execute("CREATE TABLE IF NOT EXISTS prevs (token VARCHAR NOT NULL, prev VARCHAR NOT NULL, CONSTRAINT prevskey PRIMARY KEY (token, prev))");       
    }
    */
    
    /*
    public void newFile(String table) throws SQLException {
        Statement stmt = conn.createStatement();
        stmt.executeUpdate("UPDATE "+table+" SET coverage = coverage + 1 WHERE current > 0");

        stmt = conn.createStatement();
        stmt.executeUpdate("UPDATE "+table+" SET current = 0");
    }

    @SuppressWarnings("resource")
	public void put(String name, String prev, String table) throws SQLException {
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM "+ table + " WHERE token = '" + name + "'");
        rs.next();
        if (rs.getInt(1) == 1) {
            stmt = conn.createStatement();
            stmt.executeUpdate("UPDATE "+ table + " SET global = global + 1, current = current + 1 WHERE token = '" + name + "'");

            if (prev != null) {
                stmt = conn.createStatement();
                rs = stmt.executeQuery("SELECT COUNT(*) FROM prevs WHERE token = '" + name + "' AND prev = '" + prev + "'");
                rs.next();
                if (rs.getInt(1) == 0) {
                    stmt = conn.createStatement();
                    stmt.executeUpdate("INSERT INTO prevs VALUES('" + name + "', '" + prev + "')");
                }

                stmt = conn.createStatement();
                rs = stmt.executeQuery("SELECT COUNT(*) FROM nexts WHERE token = '" + prev + "' AND next = '" + name + "'");
                rs.next();
                if (rs.getInt(1) == 0) {
                    stmt = conn.createStatement();
                    stmt.executeUpdate("INSERT INTO nexts VALUES('" + prev + "', '" + name + "')");
                }
            }
        } else {
            stmt = conn.createStatement();
            stmt.executeUpdate("INSERT INTO "+ table + " VALUES('" + name + "', 1, 0, 1)");
        }
    }
    
    public void initTable(String name) throws SQLException{
    	Statement stmt = conn.createStatement();
        stmt.execute("CREATE TABLE IF NOT EXISTS "+name+" (token VARCHAR NOT NULL UNIQUE, global INT NOT NULL, coverage INT NOT NULL, current INT NOT NULL)");
           
    }
    
    public void deleteTable(String name) throws SQLException{
     	Statement stmt = conn.createStatement();
    	stmt.executeUpdate("DELETE IF EXISTS "+name);   	
    }
    
    public void initWeightTables(String weightName) throws SQLException{
    	Statement stmt = conn.createStatement();
    	stmt = conn.createStatement();
        stmt.execute("CREATE TABLE IF NOT EXISTS "+weightName+"_global (token VARCHAR NOT NULL UNIQUE, global INT NOT NULL, coverage INT NOT NULL, current INT NOT NULL)");
        stmt = conn.createStatement();
        stmt.execute("CREATE TABLE IF NOT EXISTS "+weightName+"_coverage (token VARCHAR NOT NULL UNIQUE, global INT NOT NULL, coverage INT NOT NULL, current INT NOT NULL)");
        stmt = conn.createStatement();
        stmt.execute("CREATE TABLE IF NOT EXISTS "+weightName+"_result (global REAL, coverage REAL, precision DECIMAL(10,3))");
    }
    
    public void clearAnalyzeData(String weightName) throws SQLException{
    	Statement stmt = conn.createStatement();
		stmt.executeUpdate("DROP TABLE IF EXISTS "+weightName+"_global");
		stmt = conn.createStatement();
		stmt.executeUpdate("DROP TABLE IF EXISTS "+weightName+"_coverage");
		stmt = conn.createStatement();
		stmt.executeUpdate("DROP TABLE IF EXISTS "+weightName+"_result");
    }


	public void calculateWeightFractions(String weightName, String language) throws SQLException {	
		Statement stmt = conn.createStatement();
		stmt.executeUpdate("INSERT INTO "+weightName+"_global(token, global, coverage, current) SELECT TOP (SELECT count(*) FROM "+language+") token, global, coverage, current From "+weightName+" order by global DESC");
		stmt = conn.createStatement();
		stmt.executeUpdate("INSERT INTO "+weightName+"_coverage(token, global, coverage, current) SELECT TOP (SELECT count(*) FROM "+language+") token, global, coverage, current From "+weightName+" order by coverage DESC");
	
		
		stmt = conn.createStatement();
		stmt.executeUpdate("INSERT INTO "+weightName+"_RESULT( GLOBAL ,PRECISION) VALUES ("+
							"SELECT count(*) FROM "+language+", "+weightName+"_global WHERE "+language+".token = "+weightName+"_global.token,"+
							"cast(SELECT count(*) FROM "+language+", "+weightName+"_global WHERE "+language+".token = "+weightName+"_global.token as REAL)/(SELECT count(*) FROM "+language+"))");
		stmt = conn.createStatement();
		stmt.executeUpdate("INSERT INTO "+weightName+"_RESULT( COVERAGE ,PRECISION) VALUES ("+
							"SELECT count(*) FROM "+language+", "+weightName+"_coverage WHERE "+language+".token = "+weightName+"_coverage.token,"+
							"cast(SELECT count(*) FROM "+language+", "+weightName+"_coverage WHERE "+language+".token = "+weightName+"_coverage.token as REAL)/(SELECT count(*) FROM "+language+"))");
	}

	@SuppressWarnings("resource")
	public void put(String name, String table) throws SQLException {
    	Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM "+ table + " WHERE token = '" + name + "'");
        rs.next();
        if (rs.getInt(1) == 1) {
            stmt = conn.createStatement();
            stmt.executeUpdate("UPDATE "+ table + " SET global = global + 1, current = current + 1 WHERE token = '" + name + "'");
        } else {
            stmt = conn.createStatement();
            stmt.executeUpdate("INSERT INTO "+ table + " VALUES('" + name + "', 1, 0, 1)");
        }
	}
	*/
}
