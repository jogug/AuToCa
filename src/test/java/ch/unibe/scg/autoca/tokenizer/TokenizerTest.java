package ch.unibe.scg.autoca.tokenizer;
import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import ch.unibe.scg.autoca.config.Configuration;
import ch.unibe.scg.autoca.config.JSONInterface;
import ch.unibe.scg.autoca.tokenizer.TokenType;
import ch.unibe.scg.autoca.tokenizer.Tokenizer;

public class TokenizerTest {
	private Tokenizer tokenizer;
	private MockHandler mh;

	@Before
	public void setUp() {
		Configuration config = new Configuration();
		JSONInterface dataset = config.testDataSet("resources/testing/configuration/test1.cfg");
		mh = new MockHandler();
		tokenizer = new Tokenizer(mh, dataset);
		tokenizer.loadDefaults();
	}

	@After
	public void tearDown() {
		tokenizer = null;
		mh = null;
	}

	@Test
	public void testIndent3() {
		tokenizer.tokenize("hello		\n 		\n		world");

		assertEquals(5, mh.tokens.size());
		
		assertEquals("hello", mh.tokens.get(0));
		assertEquals(TokenType.WORD, mh.types.get(0));

		assertEquals(TokenType.NEWLINE, mh.types.get(1));

		assertEquals(TokenType.NEWLINE, mh.types.get(2));

		assertEquals(TokenType.INDENT, mh.types.get(3));

		assertEquals("world", mh.tokens.get(4));
		assertEquals(TokenType.WORD, mh.types.get(4));

	}	
	

	@Test
	public void testIndent4() {
		tokenizer.tokenize(
				  "foo\n"
				+ "   bar\n"
				+ "baz\n"
				+ "   quak");

		assertEquals(10, mh.tokens.size());
		
		assertEquals("foo", mh.tokens.get(0));
		//NEWLINE
		assertEquals(TokenType.INDENT, mh.types.get(2));
		assertEquals("bar", mh.tokens.get(3));
		//NEWLINE
		assertEquals(TokenType.DEDENT, mh.types.get(5));
		assertEquals("baz", mh.tokens.get(6));
		//NEWLINE
		assertEquals(TokenType.INDENT, mh.types.get(8));
		assertEquals("quak", mh.tokens.get(9));
	}	
	
	
	@Test
	public void testWord() {
		tokenizer.tokenize("hello");

		assertEquals(1, mh.tokens.size());
		assertEquals("hello", mh.tokens.get(0));
		assertEquals(TokenType.WORD, mh.types.get(0));
	}

	@Test
	public void testWords() {
		tokenizer.tokenize("hello world");

		assertEquals(2, mh.tokens.size());
		assertEquals("hello", mh.tokens.get(0));
		assertEquals(TokenType.WORD, mh.types.get(0));

		assertEquals("world", mh.tokens.get(1));
		assertEquals(TokenType.WORD, mh.types.get(1));
	}

	@Test
	public void testString() {
		tokenizer.tokenize("\"hello world\"");

		assertEquals(1, mh.tokens.size());
		assertEquals("\"hello world\"", mh.tokens.get(0));
		assertEquals(TokenType.STRING, mh.types.get(0));
	}
	

	@Test
	public void testStringMultiline() {
		tokenizer.tokenize("\"hello\n world\"");

		assertEquals(1, mh.tokens.size());
		assertEquals("\"hello\n world\"", mh.tokens.get(0));
		assertEquals(TokenType.STRING, mh.types.get(0));
	}

	@Test
	public void testNewline() {
		tokenizer.tokenize("hello\nword");

		assertEquals(3, mh.tokens.size());
		

		assertEquals("hello", mh.tokens.get(0));
		assertEquals(TokenType.WORD, mh.types.get(0));

		assertEquals(TokenType.NEWLINE, mh.types.get(1));

		assertEquals("word", mh.tokens.get(2));
		assertEquals(TokenType.WORD, mh.types.get(2));
	}
	
	@Test
	public void testIndent() {
		tokenizer.tokenize("   hello");

		assertEquals(2, mh.tokens.size());
		
		assertEquals(TokenType.INDENT, mh.types.get(0));

		assertEquals("hello", mh.tokens.get(1));
		assertEquals(TokenType.WORD, mh.types.get(1));

	}
	
	@Test
	public void testIndent2() {
		tokenizer.tokenize("hello  \n  world");

		assertEquals(4, mh.tokens.size());
		
		assertEquals("hello", mh.tokens.get(0));
		assertEquals(TokenType.WORD, mh.types.get(0));

		assertEquals(TokenType.NEWLINE, mh.types.get(1));
		assertEquals(TokenType.INDENT, mh.types.get(2));

		assertEquals("world", mh.tokens.get(3));
		assertEquals(TokenType.WORD, mh.types.get(3));

	}
	
	@Test
	public void testDedent() {
		tokenizer.tokenize("hello  \n  world\nthere");

		assertEquals(7, mh.tokens.size());
		
		assertEquals("hello", mh.tokens.get(0));
		assertEquals(TokenType.WORD, mh.types.get(0));

		assertEquals(TokenType.NEWLINE, mh.types.get(1));
		assertEquals(TokenType.INDENT, mh.types.get(2));

		assertEquals("world", mh.tokens.get(3));
		assertEquals(TokenType.WORD, mh.types.get(3));

		assertEquals(TokenType.NEWLINE, mh.types.get(4));
		assertEquals(TokenType.DEDENT, mh.types.get(5));

		assertEquals("there", mh.tokens.get(6));
		assertEquals(TokenType.WORD, mh.types.get(6));
	}
	
	
	@Test
	public void testMultiComment1() {
		tokenizer.tokenize("/* how are you \n today? */");

		assertEquals(1, mh.tokens.size());

		assertEquals(TokenType.COMMENT, mh.types.get(0));
	}
	
	@Test
	public void testMultiComment2() {
		tokenizer.tokenize("hello  /* how are you \n today? */ \n  world\nthere");

		assertEquals(8, mh.tokens.size());
		
		assertEquals("hello", mh.tokens.get(0));
		assertEquals(TokenType.WORD, mh.types.get(0));

		assertEquals(TokenType.COMMENT, mh.types.get(1));

		
		assertEquals(TokenType.NEWLINE, mh.types.get(2));
		assertEquals(TokenType.INDENT, mh.types.get(3));

		assertEquals("world", mh.tokens.get(4));
		assertEquals(TokenType.WORD, mh.types.get(4));

		assertEquals(TokenType.NEWLINE, mh.types.get(5));
		assertEquals(TokenType.DEDENT, mh.types.get(6));

		assertEquals("there", mh.tokens.get(7));
		assertEquals(TokenType.WORD, mh.types.get(7));
	}
	
	@Test
	public void testSingleComment1() { 
		tokenizer.tokenize("// how are you \n");

		assertEquals(1, mh.tokens.size());

		assertEquals(TokenType.COMMENT, mh.types.get(0));
	}

	@Test
	public void testSingleComment2() {
		tokenizer.tokenize("// how are you \ntoday");

		assertEquals(2, mh.tokens.size());

		assertEquals(TokenType.COMMENT, mh.types.get(0));
		
		assertEquals("today", mh.tokens.get(1));
		assertEquals(TokenType.WORD, mh.types.get(1));
	}

	@SuppressWarnings("resource")
	public String readFile(String filename) throws FileNotFoundException {
		return new Scanner(new File(filename)).useDelimiter("\\Z").next();
	}
}

