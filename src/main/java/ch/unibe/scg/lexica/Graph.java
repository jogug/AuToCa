/*
** Copyright 2013 Software Composition Group, University of Bern. All rights reserved.
*/
package ch.unibe.scg.lexica;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles connections to H2DB
 * @author Joel
 *
 */
public class Graph implements Closeable {

    private static final Logger logger = LoggerFactory.getLogger(Graph.class);
    private static final String FILENAME = ".lexica";

    private final Connection conn;

    public Graph(Path path, ArrayList<Weight> weights) throws ClassNotFoundException, SQLException {
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
    
    public Graph(Path path) throws ClassNotFoundException, SQLException {
        Objects.requireNonNull(path);

        Path database = path.resolve(FILENAME);
          
        Class.forName("org.h2.Driver");
        conn = DriverManager.getConnection("jdbc:h2:" + database.toString(), "sa", "");           
    }
    
    public void newFile(String table) throws SQLException {
        Statement stmt = conn.createStatement();
        stmt.executeUpdate("UPDATE "+table+" SET coverage = coverage + 1 WHERE current > 0");

        stmt = conn.createStatement();
        stmt.executeUpdate("UPDATE "+table+" SET current = 0");
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
	
    public void print() throws SQLException {
    	//TODO
    	/*
    	Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery("SELECT top 20 * FROM tokens ORDER BY coverage desc"); 
        
        while (rs.next()) {
            String token = rs.getString("token");
            int global = rs.getInt("global");
            int coverage = rs.getInt("coverage");

            System.out.println(token + ";"+"global "+global+ " coverage" +coverage);
        }   	
        */
    }
}
