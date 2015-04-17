package ch.unibe.scg.autoca.tokenizer;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import ch.unibe.scg.autoca.config.JSONInterface;

public class Tokenizer {
	public final String DEFAULT_LS;
	public final String DEFAULT_WORD;
	public final String DEFAULT_STRING;
	public final String DEFAULT_MULTI_COMMENT;
	public final String DEFAULT_SINGLE_COMMENT;

	
	public final String PYTHON_LIKE_COMMENT;
	
	private final String WHITESPACE;
	private final String START_OF_LINE;
	private final String NEWLINE;
	private final String EMPTYLINE;
	private final String TABSPACE;

	private TokenizerHandler th;
	private String ls;

	private List<String> words = new ArrayList<String>();
	private List<String> strings = new ArrayList<String>();
	private List<String> comments= new ArrayList<String>();

	private List<TokenPositions> tpWords = new ArrayList<TokenPositions>();
	private List<TokenPositions> tpStrings = new ArrayList<TokenPositions>();
	private List<TokenPositions> tpComments = new ArrayList<TokenPositions>();

	private TokenPositions tpStartOfLine;
	private TokenPositions tpWhitespace;
	private TokenPositions tpNewline;
	private TokenPositions tpEmptyline;
	
	private int position;
	private int indent;
	
	public Tokenizer(TokenizerHandler th, JSONInterface dataset) {
		this.th = th;
		DEFAULT_LS = dataset.getDEFAULT_LS();
		DEFAULT_WORD = dataset.getDEFAULT_WORD();
		DEFAULT_STRING = dataset.getDEFAULT_STRING();
		DEFAULT_MULTI_COMMENT = dataset.getDEFAULT_MULTI_COMMENT();
		DEFAULT_SINGLE_COMMENT = dataset.getDEFAULT_SINGLE_COMMENT();
		
		PYTHON_LIKE_COMMENT = dataset.getPYTHON_LIKE_COMMENT();
		
		WHITESPACE = dataset.getWHITESPACE();
		START_OF_LINE = dataset.getSTART_OF_LINE();
		NEWLINE = dataset.getNEWLINE();
		TABSPACE = dataset.getTABSPACE();
		EMPTYLINE = "(?m)^[ \t]*\n";
		ls = DEFAULT_LS;
	}

	public void loadDefaults() {
		addWord(DEFAULT_WORD);
		addString(DEFAULT_STRING);
		addComment(DEFAULT_MULTI_COMMENT);
		addComment(DEFAULT_SINGLE_COMMENT);
		
		// TODO: HACK ALERT?
		addComment(PYTHON_LIKE_COMMENT);
	}

	public void addWord(String word) {
		words.add(word);
	}

	public void addString(String string) {
		strings.add(string);
	}

	public void addComment(String string) {
		comments.add(string);
	}
	
	public void setLineSeparator(String ls) {
		this.ls = ls;
	}

	public void tokenize(File f) {
		String s = fileToString(f);
		tokenize(s);
	}

	public void tokenize(String s) {
		initializeTokenizing(s);

		int oldIndent = -1;
		int oldPosition = -1;
		while (!atEnd(s)) {

			checkProgress(oldIndent, oldPosition);
			oldIndent = indent;
			oldPosition = position;

			if (tryEmptyline()) 
				continue;
			if (tryNewline()) 
				continue;
			if (tryIndent())
				continue;
			if (tryDedent())
				continue;
			if (tryWhitespace())
				continue;
			if (tryComments())
				continue;
			if (tryWords())
				continue;
			if (tryStrings())
				continue;
			if (tryAny(s))
				continue;

			throw new IllegalStateException("Should not happen");
		}

	}

	private void checkProgress(int oldIndent, int oldPosition) {
		if (position == oldPosition && indent == oldIndent) {
			throw new IllegalStateException("no progress in a loop!");
		}
	}

	private void initializeTokenizing(String s) {
		position = 0;

		initializeTokenPositions(tpWords, words, s);
		initializeTokenPositions(tpStrings, strings, s);
		initializeTokenPositions(tpComments, comments, s);

		tpWhitespace = new TokenPositions(WHITESPACE, s);
		tpStartOfLine = new TokenPositions(START_OF_LINE, s);
		tpNewline = new TokenPositions(NEWLINE, s);
		tpEmptyline = new TokenPositions(EMPTYLINE, s);
	}

	private void initializeTokenPositions(List<TokenPositions> tps,
			List<String> patterns, String s) {
		tps.clear();
		for (String pattern : patterns) {
			tps.add(new TokenPositions(pattern, s));
		}
	}

	private boolean atEnd(String s) {
		return position == s.length();
	}

	private boolean tryWords() {
		return tryTokens(tpWords, TokenType.WORD);
	}

	private boolean tryStrings() {
		return tryTokens(tpStrings, TokenType.STRING);
	}

	private boolean tryComments() {
		return tryTokens(tpComments, TokenType.COMMENT);
	}

	
	private boolean tryTokens(List<TokenPositions> tps, TokenType type) {
		for (TokenPositions tp : tps) {
			if (tryToken(tp, type)) {
				return true;
			}
		}
		return false;
	}

	private boolean tryIndent() {
		if (tpStartOfLine.find(position) == position)
		{
			int newIndent = tpStartOfLine.token().replaceAll("\t", TABSPACE).length();


			if (indent < newIndent) {
				th.token("#indent", TokenType.INDENT);
				
				position = tpStartOfLine.end();
				
				indent = newIndent;
				//System.out.println("!indent, new column: " + newIndent);
				
				return true;
			}
		}
		return false;
	}

	private boolean tryDedent() {
		if (tpStartOfLine.find(position) == position)
		{
			int newIndent = tpStartOfLine.token().replaceAll("\t", TABSPACE).length();
			
			if (indent > newIndent) {
				th.token("#dedent", TokenType.DEDENT);
				
				position = tpStartOfLine.end();
				
				indent = newIndent;
				//System.out.println("!dedent, new column: " + newIndent);
				
				return true;
			}
		}
		return false;
	}


	private boolean tryEmptyline() {
		return tryToken(tpEmptyline, TokenType.NEWLINE);
	}
	
	private boolean tryNewline() {
		return tryToken(tpNewline, TokenType.NEWLINE);
	}
	
	private boolean tryToken(TokenPositions tp, TokenType type) {
		if (tp.find(position) == position) {
			String token = tp.token();
			position = tp.end();

			//System.out.println(token);
			th.token(token, type);
			return true;
		}

		return false;
	}

	private boolean tryAny(String s) {
		String token = s.substring(position, position + 1);
		position++;
		th.token(token, TokenType.UNKNOWN);
		return true;
	}

	private boolean tryWhitespace() {

		if (tpWhitespace.find(position) == position) {
			// String token = tpWhitespace.token();
			position = tpWhitespace.end();

			// Whitespace is skipped
			// th.token(token, TokenType.WORD);
			return true;
		}

		return false;
	}

	/**
	 * Using string might be slow, but more flexible. Hopefully a good choice
	 * for a beginning.
	 * 
	 * @param f
	 * @return
	 */
	private String fileToString(File f) {
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new FileReader(f));
			String line;
			StringBuilder stringBuilder = new StringBuilder();

			while ((line = reader.readLine()) != null) {
				stringBuilder.append(line);
				stringBuilder.append(ls);
			}

			return stringBuilder.toString();
		} catch (IOException e) {
			// TODO: Use Logger
			e.printStackTrace();
		} finally {
			if (reader != null) {
				try {
					reader.close();
				} catch (IOException e) {
					// TODO Maybe log it?
				}
			}
		}
		return "";
	}
}
