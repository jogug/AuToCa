/*
 ** Copyright 2013 Software Composition Group, University of Bern. All rights reserved.
 */

package ch.unibe.scg.autoca;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Holds General information, Project file Locations and the Output location
 * and allows to manipulate the data of the DataSet.
 * 
 * @author Joel
 */
public class DataSet {
	private static final Logger logger = LoggerFactory.getLogger(DataSet.class);
	private List<Language> languages;
	private Path outputLocation;	

	
	public DataSet(List<Language> language){
		//TODO when passed from command prompt
		this.languages = language;
	}
	
	public DataSet(){
		languages = new ArrayList<Language>();
	}
	
	public void loadStandardDataSet() {		
		logger.info("Starting data initialization");
		outputLocation = Paths.get("../AuToCa/resources/");
		
//		Language java = new Language("Java","*.java", Paths.get("../AuToCa/resources/java_tokens.txt"));
//		java.addMultipleProjects(Paths.get("../AuToCa/resources/testprojects/Java-Small/"), java);
//		languages.add(java);

		Language java = new Language("Java","*.java", Paths.get("../AuToCa/resources/java_tokens.txt"));
		java.addMultipleProjects(Paths.get("../AuToCa/resources/testprojects/Java/"), java);
		languages.add(java);
		
		Language c = new Language("C","*.c", Paths.get("../AuToCa/resources/c_tokens.txt"));
		c.addMultipleProjects(Paths.get("../AuToCa/resources/testprojects/C/"), c);
		languages.add(c);	
		
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
	
	public Path getOutputLocation(){
		return outputLocation;
	}

	public  List<Language> getLanguages() {
		return languages;
	}
}
