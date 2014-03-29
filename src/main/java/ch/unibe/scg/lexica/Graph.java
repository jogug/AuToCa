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
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles the connections to H2DB
 * @author Joel
 *
 */
public class Graph implements Closeable {

    private static final Logger logger = LoggerFactory.getLogger(Graph.class);
    private static final String FILENAME = ".lexica";

    private final Connection conn;

    public Graph(Path path, boolean create) throws ClassNotFoundException, SQLException {
        Objects.requireNonNull(path);

        Path database = path.resolve(FILENAME);
          
        Class.forName("org.h2.Driver");
        conn = DriverManager.getConnection("jdbc:h2:" + database.toString(), "sa", "");
        

        if (create) {
            Statement stmt = conn.createStatement();
            stmt.execute("DROP ALL OBJECTS");

            stmt = conn.createStatement();
            stmt.execute("CREATE TABLE IF NOT EXISTS tokens (token VARCHAR NOT NULL UNIQUE, global INT NOT NULL, average REAL NOT NULL, coverage INT NOT NULL, current INT NOT NULL)");

            stmt = conn.createStatement();
            stmt.execute("CREATE TABLE IF NOT EXISTS nexts (token VARCHAR NOT NULL, next VARCHAR NOT NULL, CONSTRAINT nextskey PRIMARY KEY (token, next))");

            stmt = conn.createStatement();
            stmt.execute("CREATE TABLE IF NOT EXISTS prevs (token VARCHAR NOT NULL, prev VARCHAR NOT NULL, CONSTRAINT prevskey PRIMARY KEY (token, prev))");
            
        }
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
    

    public void newFile() throws SQLException {
        Statement stmt = conn.createStatement();
        stmt.executeUpdate("UPDATE tokens SET coverage = coverage + 1 WHERE current > 0");

        stmt = conn.createStatement();
        stmt.executeUpdate("UPDATE tokens SET average = (average + current) / 2");

        stmt = conn.createStatement();
        stmt.executeUpdate("UPDATE tokens SET current = 0");
    }

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
            stmt.executeUpdate("INSERT INTO "+ table + " VALUES('" + name + "', 1, 0, 0, 1)");
        }
    }
    
    public void analyzeInit() throws SQLException{
    	Statement stmt = conn.createStatement();
        stmt = conn.createStatement();
        stmt.execute("CREATE TABLE IF NOT EXISTS globalT (token VARCHAR NOT NULL UNIQUE, global INT NOT NULL, average REAL NOT NULL, coverage INT NOT NULL, current INT NOT NULL)");
        stmt = conn.createStatement();
        stmt.execute("CREATE TABLE IF NOT EXISTS averageT (token VARCHAR NOT NULL UNIQUE, global INT NOT NULL, average REAL NOT NULL, coverage INT NOT NULL, current INT NOT NULL)");
        stmt = conn.createStatement();
        stmt.execute("CREATE TABLE IF NOT EXISTS coverageT (token VARCHAR NOT NULL UNIQUE, global INT NOT NULL, average REAL NOT NULL, coverage INT NOT NULL, current INT NOT NULL)");
        stmt = conn.createStatement();
        stmt.execute("CREATE TABLE IF NOT EXISTS actualTokenT (token VARCHAR NOT NULL UNIQUE, global INT NOT NULL, average REAL NOT NULL, coverage INT NOT NULL, current INT NOT NULL)");
        stmt = conn.createStatement();
        stmt.execute("CREATE TABLE IF NOT EXISTS resultT (global REAL, average REAL, coverage REAL, precision DECIMAL(10,3), recall DECIMAL(5,3))");
    }
    
    public void clearAnalyzeData() throws SQLException{
    	Statement stmt = conn.createStatement();
		stmt.executeUpdate("DELETE globalT");
		stmt = conn.createStatement();
		stmt.executeUpdate("DELETE averageT");
		stmt = conn.createStatement();
		stmt.executeUpdate("DELETE coverageT");
		stmt = conn.createStatement();
		stmt.executeUpdate("DELETE resultT");
		stmt = conn.createStatement();
		stmt.executeUpdate("DELETE actualTokenT");
    }


	public void calculateFractions(int n) throws SQLException {	
		Statement stmt = conn.createStatement();
		stmt.executeUpdate("INSERT INTO globalT(token, global, average, coverage, current) SELECT TOP "+n+" token, global, average, coverage, current From tokens order by global DESC");
		stmt = conn.createStatement();
		stmt.executeUpdate("INSERT INTO averageT(token, global, average, coverage, current) SELECT TOP "+n+" token, global, average, coverage, current From tokens order by average DESC");
		stmt = conn.createStatement();
		stmt.executeUpdate("INSERT INTO coverageT(token, global, average, coverage, current) SELECT TOP "+n+" token, global, average, coverage, current From tokens order by coverage DESC");
	
		
		stmt = conn.createStatement();
		stmt.executeUpdate("INSERT INTO RESULTT( GLOBAL ,PRECISION ,RECALL ) VALUES ("+
							"SELECT count(*) FROM actualTokenT, globalt WHERE actualTokenT.token = globalt.token,"+
							"(cast(SELECT count(*) FROM actualTokenT, globalt WHERE actualTokenT.token = globalt.token as REAL)/(SELECT count(*) FROM actualTokenT)),"+
							"cast(SELECT count(*) FROM actualTokenT, globalt WHERE actualTokenT.token = globalt.token as REAL)/"+n+")");
		stmt = conn.createStatement();
		stmt.executeUpdate("INSERT INTO RESULTT( AVERAGE ,PRECISION ,RECALL ) VALUES ("+
							"SELECT count(*) FROM actualTokenT, averaget WHERE actualTokenT.token = averaget.token,"+
							"cast(SELECT count(*) FROM actualTokenT, averaget WHERE actualTokenT.token = averaget.token as REAL)/(SELECT count(*) FROM actualTokenT),"+
							"cast(SELECT count(*) FROM actualTokenT, averaget WHERE actualTokenT.token = averaget.token as REAL)/"+n+")");
		stmt = conn.createStatement();
		stmt.executeUpdate("INSERT INTO RESULTT( COVERAGE ,PRECISION ,RECALL ) VALUES ("+
							"SELECT count(*) FROM actualTokenT, coveraget WHERE actualTokenT.token = coveraget.token,"+
							"cast(SELECT count(*) FROM actualTokenT, coveraget WHERE actualTokenT.token = coveraget.token as REAL)/(SELECT count(*) FROM actualTokenT),"+
							"cast(SELECT count(*) FROM actualTokenT, coveraget WHERE actualTokenT.token = coveraget.token as REAL)/"+n+")");
	}
    
    public void print() throws SQLException {
    	Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery("SELECT top 20 * FROM tokens ORDER BY coverage asc"); 
        /*
        while (rs.next()) {
            String token = rs.getString("token");
            int global = rs.getInt("global");
            float average = rs.getFloat("average");
            int coverage = rs.getInt("coverage");

            System.out.println(token + ";"+"global "+global+" average:" + average+ " coverage" +coverage);
            System.out.print(token + ";");
            System.out.format("%d;%.2f;%d%n", "global "+global," average:" + average, " coverage" +coverage);
        }   	
        */
    }

}
