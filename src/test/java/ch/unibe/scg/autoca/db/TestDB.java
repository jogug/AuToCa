package ch.unibe.scg.autoca.db;

import static org.junit.Assert.*;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import ch.unibe.scg.autoca.Configuration;
import ch.unibe.scg.autoca.JSONInterface;

public class TestDB {
	private DB db;
	private Connection conn;
	private JSONInterface dataset;
	Path path = Paths.get("test-db");
	private PreparedStatement stmt;
	private ResultSet res;
	
	@Before
	public void setUp() throws ClassNotFoundException, SQLException {
		Configuration config = new Configuration();
		dataset = config.testDataSet();
    	db = new DB(path, dataset);
		db.initialize();
		
		openConnection();
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
	
	@Test
	public void testToken() throws SQLException, ClassNotFoundException {
		db.newLanguage("LangA");
		db.newProject("Project", 1);
		db.newFile("File1",11);
		
		db.newToken("FOO");
		
		
		res = getTempTokens();
		res.next();
		assertEquals(1, res.getInt(1));
		assertEquals("FOO", res.getString(2));
		
		res.close();
		
		db.fileFinished();
		
		
		res = getOccurences();
		res.next();
		assertEquals(1, res.getInt(1));
		assertEquals(1, res.getInt(2));
		assertEquals(1, res.getInt(3));
		
		res.close();
		
		db.projectFinished();
		db.languageFinished();
	}

	@Test
	public void testToken2() throws SQLException, ClassNotFoundException {
		db.newLanguage("LangA");
		db.newProject("Project", 1);
		db.newFile("File1",11);
		
		db.newToken("FOO");
		db.newToken("FOO2");
		db.newToken("FOO3");
		db.newToken("FOO4");
		db.newToken("FOO");
		
		
		res = getTempTokens();
		res.next();
		assertEquals(1, res.getInt(1));
		assertEquals("FOO", res.getString(2));

		res.next();
		assertEquals(2, res.getInt(1));
		assertEquals("FOO2", res.getString(2));
		
		res.next();
		res.next();
		assertEquals(4, res.getInt(1));
		assertEquals("FOO4", res.getString(2));

		res.next();
		assertEquals(5, res.getInt(1));
		assertEquals("FOO", res.getString(2));
		
		res.close();
		
		db.fileFinished();
		
		
		res = getOccurences();
		// Returns (tokenid, fileid, orderid)
		res.next();
		assertEquals(db.getTokenId("FOO"), res.getInt(1));
		assertEquals(1, res.getInt(2));
		assertEquals(1, res.getInt(3));

		res.next();
		assertEquals(db.getTokenId("FOO2"), res.getInt(1));
		assertEquals(1, res.getInt(2));
		assertEquals(2, res.getInt(3));
		res.next();
		res.next();
		res.next();
		assertEquals(db.getTokenId("FOO"), res.getInt(1));
		assertEquals(1, res.getInt(2));
		assertEquals(5, res.getInt(3));

		
		db.projectFinished();
		db.languageFinished();
	}
	
	@Test
	public void testToken3() throws SQLException, ClassNotFoundException {
		db.newLanguage("LangA");
		db.newProject("Project", 1);
		db.newFile("File1",11);
		
		db.newToken("FOO");
		db.newToken("FOO2");
		
		
		res = getTempTokens();
		res.next();
		assertEquals(1, res.getInt(1));
		assertEquals("FOO", res.getString(2));

		res.next();
		assertEquals(2, res.getInt(1));
		assertEquals("FOO2", res.getString(2));
		
		
		db.fileFinished();
		
		
		db.newFile("File2", 11);
		db.newToken("FOO2");
		db.newToken("FOO");
		
		db.fileFinished();
		
		res = getOccurences();
		// Returns (tokenid, fileid, orderid)
		res.next();
		assertEquals(db.getTokenId("FOO"), res.getInt(1));
		assertEquals(1, res.getInt(2));
		assertEquals(1, res.getInt(3));

		res.next();
		assertEquals(db.getTokenId("FOO2"), res.getInt(1));
		assertEquals(1, res.getInt(2));
		assertEquals(2, res.getInt(3));
		
		res.next();
		assertEquals(db.getTokenId("FOO2"), res.getInt(1));
		assertEquals(2, res.getInt(2));
		assertEquals(3, res.getInt(3));

		res.next();
		assertEquals(db.getTokenId("FOO"), res.getInt(1));
		assertEquals(2, res.getInt(2));
		assertEquals(4, res.getInt(3));
		
		db.projectFinished();
		db.languageFinished();
	}		
	
	private ResultSet getOccurences() throws SQLException {
		stmt = conn.prepareStatement("SELECT * FROM " + dataset.getOCCURENCE());
		return stmt.executeQuery();
	}	

	private ResultSet getTempTokens() throws SQLException {
		stmt = conn.prepareStatement("SELECT * FROM " + dataset.getTEMPORARY());
		return stmt.executeQuery();
	}
	
	private void openConnection() throws ClassNotFoundException, SQLException {
		if (conn == null) {
			Class.forName("org.h2.Driver");
			conn = DriverManager.getConnection("jdbc:h2:" + path.resolve(dataset.getFilename()).toString(), "sa", "");
		}
	}
}
