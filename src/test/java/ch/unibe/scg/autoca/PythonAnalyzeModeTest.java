package ch.unibe.scg.autoca;

import static org.junit.Assert.*;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import ch.unibe.scg.autoca.configuration.Configuration;
import ch.unibe.scg.autoca.database.Database;
import ch.unibe.scg.autoca.datastructure.Dataset;
import ch.unibe.scg.autoca.executionmode.AnalyzeMode;
import ch.unibe.scg.autoca.executionmode.TokenizeMode;

public class PythonAnalyzeModeTest {
	private Database db;
	private Connection conn;
	private Dataset dataset;
	private PreparedStatement stmt;
	private ResultSet res;
	private Configuration config;
	
	@Before
	public void setUp() throws ClassNotFoundException, SQLException {
		config = new Configuration();
		dataset = config.testDataSet("resources/testing/configuration/testPython.cfg");
    	db = new Database(dataset.getOutputLocation(), dataset);
		db.initialize();		

		TokenizeMode scanmode = new TokenizeMode(dataset);
		scanmode.execute();
		AnalyzeMode analyzemode = new AnalyzeMode(dataset);
		analyzemode.execute();
		openConnection();
	}
	
	@Test
	public void testGlobal() throws ClassNotFoundException, SQLException{
		res = getResultTable("Python_Global");
		assertEquals(db.getRowCountOfTable("Python_Global"), db.getRowCountOfTable("tokens"));
		//Occurence vector expected
		res.first();
		int[] actual={51,24,9,8,6,6,5,4,4,3,3,2,2,2,2,2,2,2,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1};
		int i = 0;
		while(!res.isAfterLast()){
			assertEquals(actual[i], res.getInt(2));
			i++;
			res.next();
		}	
	}
	
	@Test
	public void testIndent() throws ClassNotFoundException, SQLException{
		res = getResultTable("Python_RealIndent");
		int[] actual={4,1,1};
		int i = 0;
		res.first();
		while(!res.isAfterLast()){
			assertEquals(actual[i], res.getInt(2));
			assertNotEquals("#indent", res.getString(1));
			i++;
			res.next();
		}	
	}
	
	@Test
	public void testNewline() throws ClassNotFoundException, SQLException{
		res = getResultTable("Python_Newline");
		//Occurence vector expected
		int[] actual={7,4,3,3,2,1,1,1,1};
		int i = 0;
		res.first();
		while(!res.isAfterLast()){
			assertEquals(actual[i], res.getInt(2));
			assertNotEquals("#indent", res.getString(1));
			assertNotEquals("#dedent", res.getString(1));
			i++;
			res.next();
		}	
	}
	
	@Test
	public void testCoverage() throws ClassNotFoundException, SQLException{
		res = getResultTable("Python_Coverage");
		res.first();
		while(!res.isAfterLast()){
			assertEquals(1, res.getInt(2));
			res.next();
		}	
	}
	
	@After
	public void tearDown() throws SQLException {
		if (res != null) {
			res.close();
		}
		
		if (stmt != null) {
			stmt.close();
		}
		
		if (conn != null) {
			conn.close();
		}
	}

	private ResultSet getResultTable(String tableName) throws SQLException {
		stmt = conn.prepareStatement("SELECT * FROM \"" + tableName + "\"");
		return stmt.executeQuery();
	}
	
	private void openConnection() throws ClassNotFoundException, SQLException {
		if (conn == null) {
			Class.forName("org.h2.Driver");
			conn = DriverManager.getConnection("jdbc:h2:" + dataset.getOutputLocation().resolve(dataset.getServerFilename()).toString(), "sa", "");
		}
	}
}
