package ch.unibe.scg.autoca;

import java.sql.SQLException;

import ch.unibe.scg.autoca.tokenizer.TokenType;
import ch.unibe.scg.autoca.tokenizer.TokenizerHandler;

public class TokenHandler implements TokenizerHandler {
	private DB db;
	private String file, tempTable, fileTable;
	
	public TokenHandler(DB db, String file, String tempTable, String fileTable){
		this.tempTable = tempTable;
		this.fileTable = fileTable;
		this.db = db;
		this.file = file;
		assignFileID();
		deleteTempTable();
	}
	
	/**
	 * Insert a Token into tempFile table
	 */
	@Override
	public void token(String token, TokenType type) {
		try {
			token = token.replace("'", "''");
			//TODO
			if(token.length()<27 && token.length()>1){
			db.insertToken(token, tempTable , file);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}	
	}
	
	public void insertFileIntoDB(){
		assignTokensIDs();
		saveOrderOfTokensInTable();
	}
	
	private void assignTokensIDs(){
		try {
			db.assignTokensInTempTableIDs();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	private void saveOrderOfTokensInTable(){
		try {
			db.insertOrderIDs();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	private void assignFileID(){
		try {
			db.insertObjectName(file, fileTable);
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * clear temporary values table before starting a new file
	 */
	private void deleteTempTable(){
        try {
			db.deleteRecords(tempTable);
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
}
