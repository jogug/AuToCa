package ch.unibe.scg.autoca;

import java.sql.SQLException;

import ch.unibe.scg.autoca.tokenizer.TokenType;
import ch.unibe.scg.autoca.tokenizer.TokenizerHandler;

public class TokenHandler implements TokenizerHandler {
	private DB db;
	private final int maxTokenLength, minTokenLength;
	
	public TokenHandler(DB db, int maxTokenLength, int minTokenLength){
		this.db = db;
		this.maxTokenLength = maxTokenLength;
		this.minTokenLength = minTokenLength;
	}
	
	/**
	 * Insert a Token into tempFile table
	 */
	@Override
	public void token(String token, TokenType type) {
		if(token.length()>minTokenLength&&token.length()<maxTokenLength){
			switch(type){
				case DEDENT: 
				case INDENT: 
				case WORD: 	try {
								db.newToken(token);
							} catch (SQLException e) {
								e.printStackTrace();
							}
							break;
				case NEWLINE://TODO Not thrown yet -> remove 
							try {
								db.newToken("#newline");
							} catch (SQLException e) {
								e.printStackTrace();
							}
				break;
				case STRING: break;
				case UNKNOWN: break;
				default:		
			}
		}
	}
}
