package ch.unibe.scg.autoca.utilities;

import java.sql.SQLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.unibe.scg.autoca.database.Database;
import ch.unibe.scg.autoca.tokenizer.TokenType;
import ch.unibe.scg.autoca.tokenizer.ITokenizerHandler;

public class DefaultTokenHandler implements ITokenizerHandler {
	private static final Logger logger = LoggerFactory.getLogger(DefaultTokenHandler.class);
	private Database db;
	private final int maxTokenLength, minTokenLength;

	public DefaultTokenHandler(Database db, int maxTokenLength, int minTokenLength) {
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
		case WORD:
			if (token.length() > minTokenLength && token.length() < maxTokenLength) {
				try {
					db.insertToken(token);
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}else{
				logger.error("Actual token found longer or smaller than limits, Check Default Token Handler: " + token.length());
			}
			break;
		case DEDENT:
			break;
		case INDENT:
			break;
		case NEWLINE:
			break;
		case STRING:
			break;
		case UNKNOWN:
			break;
		default:
		}
	}
}
