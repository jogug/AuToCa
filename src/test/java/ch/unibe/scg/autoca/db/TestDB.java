package ch.unibe.scg.autoca.db;

import static org.junit.Assert.*;

import java.nio.file.Paths;
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
import ch.unibe.scg.autoca.datastructure.Language;
import ch.unibe.scg.autoca.datastructure.Project;

public class TestDB {
	private Database db;
	private Connection conn;
	private Dataset dataset;
	private PreparedStatement stmt;
	private ResultSet res;
	
	@Before
	public void setUp() throws ClassNotFoundException, SQLException {
		Configuration config = new Configuration();
		dataset = config.testDataSet("resources/testing/configuration/test1.cfg");
    	db = new Database(dataset.getOutputLocation(), dataset);
		db.initialise();
		
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
		Language langA = new Language("LangA", "*.java", Paths.get("../AuToCa/resources/java_tokens.txt"),Long.MAX_VALUE, 1);
		db.newLanguage(langA);
		Project project = new Project(Paths.get("../AuToCa/resources/testing/testprojects/empty/"), "Project" , langA);
		db.newProject(project);
		db.newFile("File1",11);
		
		db.insertToken("FOO");
		
		
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
		Language langA = new Language("LangA", "*.java", Paths.get("../AuToCa/resources/java_tokens.txt"), Long.MAX_VALUE,1);
		db.newLanguage(langA);
		Project project = new Project(Paths.get("../AuToCa/resources/testing/testprojects/empty/"), "Project" , langA);
		db.newProject(project);
		db.newFile("File1",11);
		
		db.insertToken("FOO");
		db.insertToken("FOO2");
		db.insertToken("FOO3");
		db.insertToken("FOO4");
		db.insertToken("FOO");
		
		
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
		Language langA = new Language("LangA", "*.java", Paths.get("../AuToCa/resources/java_tokens.txt"), Long.MAX_VALUE, 1);
		db.newLanguage(langA);
		Project project = new Project(Paths.get("../AuToCa/resources/testing/testprojects/empty/"), "Project" , langA);
		db.newProject(project);
		db.newFile("File1",11);
		
		db.insertToken("FOO");
		db.insertToken("FOO2");
		
		
		res = getTempTokens();
		res.next();
		assertEquals(1, res.getInt(1));
		assertEquals("FOO", res.getString(2));

		res.next();
		assertEquals(2, res.getInt(1));
		assertEquals("FOO2", res.getString(2));
		
		
		db.fileFinished();
		
		
		db.newFile("File2", 11);
		db.insertToken("FOO2");
		db.insertToken("FOO");
		
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
		stmt = conn.prepareStatement("SELECT * FROM \"" + dataset.getOCCURRENCE() + "\"");
		return stmt.executeQuery();
	}	

	private ResultSet getTempTokens() throws SQLException {
		stmt = conn.prepareStatement("SELECT * FROM \"" + dataset.getTEMPORARY() + "\"");
		return stmt.executeQuery();
	}
	
	private void openConnection() throws ClassNotFoundException, SQLException {
		if (conn == null) {
			Class.forName("org.h2.Driver");
			conn = DriverManager.getConnection("jdbc:h2:" + dataset.getOutputLocation().resolve(dataset.getServerFilename()).toString(), "sa", "");
		}
	}
}
