package ch.unibe.scg.autoca.db;

import java.sql.SQLException;

import ch.unibe.scg.autoca.tokenizer.TokenType;
import ch.unibe.scg.autoca.tokenizer.TokenizerHandler;

public class DBTokenHandler implements TokenizerHandler {
	
	private static final String NEWLINE = "#newline";
	private static final String DEDENT = "#dedent";
	private static final String INDENT = "#indent";
	private static final String STRING = "#string";
	private static final String COMMENT = "#comment";
	private static final String UNKNOWN = "#unknown";
	private static final String LONGWORD = "#longword";
	
	private DB db;
	private final int maxTokenLength, minTokenLength;

	public DBTokenHandler(DB db, int maxTokenLength, int minTokenLength) {
		this.db = db;
		this.maxTokenLength = maxTokenLength;
		this.minTokenLength = minTokenLength;
	}

	/**
	 * Insert a Token into tempFile table
	 */
	@Override
	public void token(String token, TokenType type) {
		switch (type) {
		case DEDENT:
			try {
				db.newToken(DEDENT);
			} catch (SQLException e) {
				e.printStackTrace();
			}
			break;
		case INDENT:
			try {
				db.newToken(INDENT);
			} catch (SQLException e) {
				e.printStackTrace();
			}
			break;
		case WORD:
			if (token.length() > minTokenLength && token.length() < maxTokenLength) {
				try {
					db.newToken(token);
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}else{
				try {
					db.newToken(LONGWORD);
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
			break;
		case NEWLINE:
			try {
				db.newToken(NEWLINE);
			} catch (SQLException e) {
				e.printStackTrace();
			}
			break;
		case STRING:
			try {
				db.newToken(STRING);
			} catch (SQLException e) {
				e.printStackTrace();
			}
			break;
		case COMMENT:
			try {
				db.newToken(COMMENT);
			} catch (SQLException e) {
				e.printStackTrace();
			}
			break;
		case UNKNOWN:
			try {
				db.newToken(UNKNOWN);
			} catch (SQLException e) {
				e.printStackTrace();
			}
			break;
		default:
		}
	}
}
