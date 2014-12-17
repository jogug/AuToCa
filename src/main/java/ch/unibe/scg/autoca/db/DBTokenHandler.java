package ch.unibe.scg.autoca.db;

import java.sql.SQLException;

import ch.unibe.scg.autoca.config.JSONInterface;
import ch.unibe.scg.autoca.tokenizer.TokenType;
import ch.unibe.scg.autoca.tokenizer.TokenizerHandler;

public class DBTokenHandler implements TokenizerHandler {
	
	private final String NEWLINE;
	private final String DEDENT;
	private final String INDENT;
	private final String STRING;
	private final String COMMENT;
	private final String UNKNOWN;
	private final String LONGWORD;
	
	private DB db;
	private final int maxTokenLength, minTokenLength;

	public DBTokenHandler(DB db, JSONInterface dataset) {
		this.db = db;
		this.maxTokenLength = dataset.getDEFAULT_MAX_TOKEN_LENGTH();
		this.minTokenLength = dataset.getDEFAULT_MIN_TOKEN_LENGTH();
		
		NEWLINE = dataset.getDBNEWLINE();
		DEDENT = dataset.getDEDENT();
		INDENT = dataset.getINDENT();
		STRING = dataset.getSTRING();
		COMMENT = dataset.getCOMMENT();
		UNKNOWN = dataset.getUNKNOWN();
		LONGWORD = dataset.getLONGWORD();
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
			//LENGTH FILTER
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
