package ch.unibe.scg.autoca.tokenizer;

import static org.junit.Assert.*;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;

import org.junit.Test;

import ch.unibe.scg.autoca.configuration.Configuration;
import ch.unibe.scg.autoca.datastructure.Dataset;
import ch.unibe.scg.autoca.tokenizer.MockHandler;
import ch.unibe.scg.autoca.tokenizer.Tokenizer;

public class TokenizerIntegrationTest {

	@Test
	public void testDataFile() throws ClassNotFoundException, URISyntaxException {
		URL fileUrl = ClassLoader.getSystemResource("data_file-short.c");
		Configuration config = new Configuration();
		Dataset dataset = config.testDataSet("resources/testing/configuration/test1.cfg");
		MockHandler mh = new MockHandler();
		Tokenizer tokenizer = new Tokenizer(mh, dataset);
		
		assertNotNull(fileUrl);
		tokenizer.tokenize(new File(fileUrl.toURI()));
		
		assertTrue(mh.tokens.size() > 0);
	}
}
