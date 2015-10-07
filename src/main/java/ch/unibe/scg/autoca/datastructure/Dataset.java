/*
 ** Copyright 2013 Software Composition Group, University of Bern. All rights reserved.
 */

package ch.unibe.scg.autoca.datastructure;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.json.JSONObject;

import ch.unibe.scg.autoca.filter.FilterChain;

/**
 * Holds settings for the scan and analysis modes, data input and the output locations
 * 
 * @author Joel
 */
public class Dataset {	
	//GENERAL
	private JSONObject jsonObject;
	private List<Language> languages;	
	private List<FilterChain> filterChains;

	public Dataset(JSONObject jsonObject, List<Language> languages, List<FilterChain> filterChains){
		this.languages = languages;
		this.jsonObject = jsonObject;
		this.filterChains = filterChains;
	}
	

	public List<Language> getLanguages() {
		return languages;
	}
	
	public List<FilterChain> getFilterChain() {
		return filterChains;
	}
	
	public Path getOutputLocation() {
		return Paths.get(jsonObject.getJSONObject("DataSet").getString("outputLocation"));
	}
	
	public String getServerFilename() {		
		return jsonObject.getJSONObject("DB").getString("FILENAME");
	}

	public String getDriver() {
		return jsonObject.getJSONObject("DB").getString("DRIVER");
	}

	public String getUser() {
		return jsonObject.getJSONObject("DB").getString("USER");
	}

	public String getPassword() {
		return jsonObject.getJSONObject("DB").getString("PASSWORD");
	}
	
	public String getTEMPORARY() {
		return jsonObject.getJSONObject("Tables").getString("TEMPORARY");
	}

	public String getTEMPFILTER() {
		return jsonObject.getJSONObject("Tables").getString("TEMPFILTER");
	}

	public String getOCCURRENCE() {
		return jsonObject.getJSONObject("Tables").getString("OCCURENCE");
	}

	public String getTOKEN() {
		return jsonObject.getJSONObject("Tables").getString("TOKEN");
	}

	public String getFILE() {
		return jsonObject.getJSONObject("Tables").getString("FILE");
	}

	public String getPROJECT() {
		return jsonObject.getJSONObject("Tables").getString("PROJECT");
	}

	public String getLANGUAGE() {
		return jsonObject.getJSONObject("Tables").getString("LANGUAGE");
	}
	
	public String getRESULTTABLE() {
		return jsonObject.getJSONObject("Tables").getString("RESULTTABLE");
	}
	

	public String getPREFIXSTAT() {
		return jsonObject.getJSONObject("Tables").getString("PREFIXSTAT");
	}
	

	public String getPRECISION() {
		return jsonObject.getJSONObject("Tables").getString("PRECISION");
	}
	
	public String getRANK() {
		return jsonObject.getJSONObject("Tables").getString("RANK");
	}	

	public String getSUMMARY() {
		return jsonObject.getJSONObject("Tables").getString("SUMMARY");
	}

	
	public String getLoginprefix() {
		return jsonObject.getJSONObject("DB").getString("LOGINPREFIX");
	}

	public int getDEFAULT_MAX_TOKEN_LENGTH() {
		return jsonObject.getJSONObject("DBTokenHandler").getInt("DEFAULT_MAX_TOKEN_LENGTH");
	}

	public int getDEFAULT_MIN_TOKEN_LENGTH() {
		return jsonObject.getJSONObject("DBTokenHandler").getInt("DEFAULT_MIN_TOKEN_LENGTH");
	}

	public int getDEFAULT_PROGRESS_STEPS() {
		return jsonObject.getJSONObject("ScanMode").getInt("DEFAULT_PROGRESS_STEPS");
	}

	public String getDEFAULT_LS() {
		return jsonObject.getJSONObject("Tokenizer").getString("DEFAULT_LS");
	}

	public String getDEFAULT_KEYWORD() {
		return jsonObject.getJSONObject("Tokenizer").getString("DEFAULT_WORD");
	}

	public String getDEFAULT_STRING() {
		return jsonObject.getJSONObject("Tokenizer").getString("DEFAULT_STRING");
	}

	public String getDEFAULT_MULTI_COMMENT() {
		return jsonObject.getJSONObject("Tokenizer").getString("DEFAULT_MULTI_COMMENT");
	}

	public String getDEFAULT_SINGLE_COMMENT() {
		return jsonObject.getJSONObject("Tokenizer").getString("DEFAULT_SINGLE_COMMENT");
	}

	public String getPYTHON_LIKE_COMMENT() {
		return jsonObject.getJSONObject("Tokenizer").getString("PYTHON_LIKE_COMMENT");
	}

	public String getWHITESPACE() {
		return jsonObject.getJSONObject("Tokenizer").getString("WHITESPACE");
	}

	public String getSTART_OF_LINE() {
		return jsonObject.getJSONObject("Tokenizer").getString("START_OF_LINE");
	}

	public String getEMPTYLINE() {
		return jsonObject.getJSONObject("Tokenizer").getString("EMPTYLINE");
	}
	
	public String getNEWLINE() {
		return jsonObject.getJSONObject("Tokenizer").getString("NEWLINE");
	}
	
	public String getTABSPACE() {
		return jsonObject.getJSONObject("Tokenizer").getString("TABSPACE");
	}
	
	public String getDBNEWLINE() {
		return jsonObject.getJSONObject("DBTokenHandler").getString("DBNEWLINE");
	}

	public String getDEDENT() {
		return jsonObject.getJSONObject("DBTokenHandler").getString("DEDENT");
	}

	public String getINDENT() {
		return jsonObject.getJSONObject("DBTokenHandler").getString("INDENT");
	}

	public String getSTRING() {
		return jsonObject.getJSONObject("DBTokenHandler").getString("STRING");
	}


	public String getCOMMENT() {
		return jsonObject.getJSONObject("DBTokenHandler").getString("COMMENT");
	}


	public String getDELIMITER() {
		return jsonObject.getJSONObject("DBTokenHandler").getString("DELIMITER");
	}


	public String getLONGWORD() {
		return jsonObject.getJSONObject("DBTokenHandler").getString("LONGWORD");
	}
	
	public int getFileCount(){
		int count = 0;
		for(Language i: languages){
			for(Project project: i.getProjects()){
				count += project.getFileCount();
			}
		}
		return count;
	}
	
	public int getProjectCount(){
		int count = 0;
		for(Language i: languages){		
			count += i.getProjects().size();
		}
		return count;
	}
}
