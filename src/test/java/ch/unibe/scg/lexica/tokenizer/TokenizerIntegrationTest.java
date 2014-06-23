package ch.unibe.scg.lexica.tokenizer;

import static org.junit.Assert.*;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;

import org.junit.Test;

import ch.unibe.scg.autoca.tokenizer.MockHandler;
import ch.unibe.scg.autoca.tokenizer.Tokenizer;

public class TokenizerIntegrationTest {
	
	
	@Test
	public void testDataFile() throws ClassNotFoundException, URISyntaxException {
		URL fileUrl = this.getClass().getResource("resources/data_file.c");
		MockHandler mh = new MockHandler();
		Tokenizer tokenizer = new Tokenizer(mh);
		
		assertNotNull(fileUrl);
		tokenizer.tokenize(new File(fileUrl.toURI()));
		
		assertTrue(mh.tokens.size() > 0);
	}
}
