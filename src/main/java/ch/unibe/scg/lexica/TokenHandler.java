package ch.unibe.scg.lexica;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import ch.unibe.scg.lexica.tokenizer.TokenType;
import ch.unibe.scg.lexica.tokenizer.TokenizerHandler;

public class TokenHandler implements TokenizerHandler {
	public List<String> tokens = new ArrayList<String>();
	public List<TokenType> types = new ArrayList<TokenType>();
	public DB db;
	public String file;
	
	public TokenHandler(DB db, String file){
		this.db = db;
		this.file = file;
		try {
			db.insertFile(file, "files");
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public void token(String token, TokenType type) {
		try {
			//LENGTH FILTER
			if(token.length()<20 && token.length()>1){
			db.insertToken(token, "token_buffer", file);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		types.add(type);		
	}

	public void mergeTokens(){
		try {
			db.mergeTokens();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	public void insertIDs(){
		try {
			db.insertIDs();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	public List<String> getTokens(){
		return tokens;
	}
	
	public List<TokenType> getTokenType(){
		return types;
	}
}
