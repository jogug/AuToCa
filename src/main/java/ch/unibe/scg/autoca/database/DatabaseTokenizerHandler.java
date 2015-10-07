package ch.unibe.scg.autoca.database;

import java.sql.SQLException;

import ch.unibe.scg.autoca.datastructure.Dataset;
import ch.unibe.scg.autoca.tokenizer.TokenType;
import ch.unibe.scg.autoca.tokenizer.ITokenizerHandler;

public class DatabaseTokenizerHandler implements ITokenizerHandler {
	
	private final String NEWLINE;
	private final String DEDENT;
	private final String INDENT;
	private final String STRING;
	private final String COMMENT;
	private final String DELIMITER;
	private final String LONGWORD;
	
	private Database db;
	private final int maxTokenLength, minTokenLength;

	public DatabaseTokenizerHandler(Database db, Dataset dataset) {
		this.db = db;
		this.maxTokenLength = dataset.getDEFAULT_MAX_TOKEN_LENGTH();
		this.minTokenLength = dataset.getDEFAULT_MIN_TOKEN_LENGTH();
		
		NEWLINE = dataset.getDBNEWLINE();
		DEDENT = dataset.getDEDENT();
		INDENT = dataset.getINDENT();
		STRING = dataset.getSTRING();
		COMMENT = dataset.getCOMMENT();
		DELIMITER = dataset.getDELIMITER();
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
				db.insertToken(DEDENT);
			} catch (SQLException e) {
				e.printStackTrace();
			}
			break;
		case INDENT:
			try {
				db.insertToken(INDENT);
			} catch (SQLException e) {
				e.printStackTrace();
			}
			break;
		case WORD:
			//LENGTH FILTER
			if (token.length() > minTokenLength && token.length() < maxTokenLength) {
				try {
					db.insertToken(token);
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}else{
				try {
					db.insertToken(LONGWORD);
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
			break;
		case NEWLINE:
			try {
				db.insertToken(NEWLINE);
			} catch (SQLException e) {
				e.printStackTrace();
			}
			break;
		case STRING:
			try {
				db.insertToken(STRING);
			} catch (SQLException e) {
				e.printStackTrace();
			}
			break;
		case COMMENT:
			try {
				db.insertToken(COMMENT);
			} catch (SQLException e) {
				e.printStackTrace();
			}
			break;
		case UNKNOWN:
			try {
				db.insertToken(DELIMITER);
			} catch (SQLException e) {
				e.printStackTrace();
			}
			break;
		default:
		}
	}
}
