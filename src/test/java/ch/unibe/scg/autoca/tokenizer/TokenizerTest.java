package ch.unibe.scg.autoca.tokenizer;
import static org.junit.Assert.assertEquals;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import ch.unibe.scg.autoca.tokenizer.TokenType;
import ch.unibe.scg.autoca.tokenizer.Tokenizer;

public class TokenizerTest {
	private Tokenizer tokenizer;
	private MockHandler mh;

	@Before
	public void setUp() {
		mh = new MockHandler();
		tokenizer = new Tokenizer(mh);
		tokenizer.loadDefaults();
	}

	@After
	public void tearDown() {
		tokenizer = null;
		mh = null;
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

}

