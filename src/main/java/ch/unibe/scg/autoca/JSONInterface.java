/*
 ** Copyright 2013 Software Composition Group, University of Bern. All rights reserved.
 */

package ch.unibe.scg.autoca;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Holds settings for the scan and analysis modes, data input and the output locations
 * 
 * @author Joel
 */
public class JSONInterface {	
	//GENERAL
	private JSONObject plainData;
	private List<Language> languages;	

	public JSONInterface(JSONObject plainData, List<Language> languages){
		this.languages = languages;
		this.plainData = plainData;
	}
	
	//TODO TABLE NAMES!!!!!
	public List<Language> getLanguages() {
		return languages;
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

	//MOVE TO CONFIGURATION
	/*
	public void loadStandardDataSet() {		
		logger.info("Starting data initialization");
		outputLocation = Paths.get("../AuToCa/resources/");
		
//		Language java = new Language("Java","*.java", Paths.get("../AuToCa/resources/java_tokens.txt"));
//		java.addMultipleProjects(Paths.get("../AuToCa/resources/testprojects/Java-Small/"), java);
//		languages.add(java);

		Language java = new Language("Java","*.java", Paths.get("../AuToCa/resources/java_tokens.txt"));
		java.addMultipleProjects(Paths.get("../AuToCa/resources/testprojects/Java/"), java);
		languages.add(java);
		
//		Language c = new Language("C","*.c", Paths.get("../AuToCa/resources/c_tokens.txt"));
//		c.addMultipleProjects(Paths.get("../AuToCa/resources/testprojects/C/"), c);
//		languages.add(c);	
//		
//		Language python = new Language("Python", "*.py", Paths.get("../AuToCa/resources/python_tokens.txt"));
//		python.addMultipleProjects(Paths.get("../AuToCa/resources/testprojects/Python/"), python);
//		languages.add(python);
//		
//		Language cpp = new Language("Cpp", "*.cpp", Paths.get("../AuToCa/resources/cpp_tokens.txt"));
//		cpp.addMultipleProjects(Paths.get("../AuToCa/resources/testprojects/Cpp/"), cpp);
//		languages.add(cpp);			

		logger.info("Finished data initialization found: " + getLanguages().size() + " Languages, "
				+ getProjectCount() + " Projects, " + getFileCount() + " Files");
	}
	
	//TODO Put in test class
	public void loadTestDataSet(){
		outputLocation = Paths.get("C:/Users/Joel/Desktop/Testprojekte/");
		
		Language java = new Language("Test","*.java", Paths.get("../AuToCa/resources/java_tokens.txt"));
		java.addMultipleProjects(Paths.get("C:/Users/Joel/Desktop/Testprojekte/Test/"), java);
		languages.add(java);
	}
	*/
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
