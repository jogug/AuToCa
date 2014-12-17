/*
 ** Copyright 2013 Software Composition Group, University of Bern. All rights reserved.
 */

package ch.unibe.scg.autoca.config;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.json.JSONObject;

import ch.unibe.scg.autoca.filter.FilterChain;
import ch.unibe.scg.autoca.structure.Language;
import ch.unibe.scg.autoca.structure.Project;

/**
 * Holds settings for the scan and analysis modes, data input and the output locations
 * 
 * @author Joel
 */
public class JSONInterface {	
	//GENERAL
	private JSONObject plainData;
	private List<Language> languages;	
	private List<FilterChain> filterChains;

	public JSONInterface(JSONObject plainData, List<Language> languages, List<FilterChain> filterChains){
		this.languages = languages;
		this.plainData = plainData;
		this.filterChains = filterChains;
	}
	

	//TODO TABLE NAMES!!!!!
	public List<Language> getLanguages() {
		return languages;
	}
	
	public List<FilterChain> getFilterChain() {
		return filterChains;
	}
	
	public Path getOutputLocation() {
		return Paths.get(plainData.getJSONObject("DataSet").getString("outputLocation"));
	}
	
	public String getFilename() {		
		return plainData.getJSONObject("DB").getString("FILENAME");
	}

	public String getDriver() {
		return plainData.getJSONObject("DB").getString("DRIVER");
	}

	public String getUser() {
		return plainData.getJSONObject("DB").getString("USER");
	}

	public String getPassword() {
		return plainData.getJSONObject("DB").getString("PASSWORD");
	}
	
	public String getTEMPORARY() {
		return plainData.getJSONObject("Tables").getString("TEMPORARY");
	}

	public String getTEMPFILTER() {
		return plainData.getJSONObject("Tables").getString("TEMPFILTER");
	}

	public String getOCCURENCE() {
		return plainData.getJSONObject("Tables").getString("OCCURENCE");
	}

	public String getTOKEN() {
		return plainData.getJSONObject("Tables").getString("TOKEN");
	}

	public String getFILE() {
		return plainData.getJSONObject("Tables").getString("FILE");
	}

	public String getPROJECT() {
		return plainData.getJSONObject("Tables").getString("PROJECT");
	}

	public String getLANGUAGE() {
		return plainData.getJSONObject("Tables").getString("LANGUAGE");
	}
	
	public String getRESULTTABLE() {
		return plainData.getJSONObject("Tables").getString("RESULTTABLE");
	}

	public String getLoginprefix() {
		return plainData.getJSONObject("DB").getString("LOGINPREFIX");
	}

	public int getDEFAULT_MAX_TOKEN_LENGTH() {
		return plainData.getJSONObject("DBTokenHandler").getInt("DEFAULT_MAX_TOKEN_LENGTH");
	}

	public int getDEFAULT_MIN_TOKEN_LENGTH() {
		return plainData.getJSONObject("DBTokenHandler").getInt("DEFAULT_MIN_TOKEN_LENGTH");
	}

	public int getDEFAULT_PROGRESS_STEPS() {
		return plainData.getJSONObject("ScanMode").getInt("DEFAULT_PROGRESS_STEPS");
	}

	public String getDEFAULT_LS() {
		return plainData.getJSONObject("Tokenizer").getString("DEFAULT_LS");
	}

	public String getDEFAULT_WORD() {
		return plainData.getJSONObject("Tokenizer").getString("DEFAULT_WORD");
	}

	public String getDEFAULT_STRING() {
		return plainData.getJSONObject("Tokenizer").getString("DEFAULT_STRING");
	}

	public String getDEFAULT_MULTI_COMMENT() {
		return plainData.getJSONObject("Tokenizer").getString("DEFAULT_MULTI_COMMENT");
	}

	public String getDEFAULT_SINGLE_COMMENT() {
		return plainData.getJSONObject("Tokenizer").getString("DEFAULT_SINGLE_COMMENT");
	}

	public String getPYTHON_LIKE_COMMENT() {
		return plainData.getJSONObject("Tokenizer").getString("PYTHON_LIKE_COMMENT");
	}

	public String getWHITESPACE() {
		return plainData.getJSONObject("Tokenizer").getString("WHITESPACE");
	}

	public String getSTART_OF_LINE() {
		return plainData.getJSONObject("Tokenizer").getString("START_OF_LINE");
	}

	public String getNEWLINE() {
		return plainData.getJSONObject("Tokenizer").getString("NEWLINE");
	}
	
	public String getDBNEWLINE() {
		return plainData.getJSONObject("DBTokenHandler").getString("DBNEWLINE");
	}

	public String getDEDENT() {
		return plainData.getJSONObject("DBTokenHandler").getString("DEDENT");
	}

	public String getINDENT() {
		return plainData.getJSONObject("DBTokenHandler").getString("INDENT");
	}


	public String getSTRING() {
		return plainData.getJSONObject("DBTokenHandler").getString("STRING");
	}


	public String getCOMMENT() {
		return plainData.getJSONObject("DBTokenHandler").getString("COMMENT");
	}


	public String getUNKNOWN() {
		return plainData.getJSONObject("DBTokenHandler").getString("UNKNOWN");
	}


	public String getLONGWORD() {
		return plainData.getJSONObject("DBTokenHandler").getString("LONGWORD");
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
