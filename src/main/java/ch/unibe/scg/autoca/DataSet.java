/*
 ** Copyright 2013 Software Composition Group, University of Bern. All rights reserved.
 */

package ch.unibe.scg.autoca;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

/**
 * Holds General information, Project file Locations and the Output location
 * @author Joel
 *
 */
public class DataSet {
	private ArrayList<Language> languages;
	private Path outputLocation;
	
	public DataSet(ArrayList<Language> language){
		//TODO when passed from command prompt
		this.languages = language;
	}
	
	public DataSet(){
		this.languages = new ArrayList<Language>();
	}
	
	public void loadStandardDataSet() {		
		outputLocation = Paths.get("C:\\Users\\Joel\\Desktop\\Testprojekte\\");
		
		Language java = new Language("Java","*.java", Paths.get("../lexica/resources/java_tokens.txt"));
		java.addMultipleProjects(Paths.get("C:\\Users\\Joel\\Desktop\\Testprojekte\\Java\\"), java);
		languages.add(java);
		
		Language c = new Language("C","*.c", Paths.get("../lexica/resources/c_tokens.txt"));
		c.addMultipleProjects(Paths.get("C:\\Users\\Joel\\Desktop\\Testprojekte\\C\\"), c);
		languages.add(c);	
		
		Language python = new Language("Python", "*.py", Paths.get("../lexica/resources/python_tokens.txt"));
		python.addMultipleProjects(Paths.get("C:\\Users\\Joel\\Desktop\\Testprojekte\\Python\\"), python);
		languages.add(python);
		
		Language cpp = new Language("Cpp", "*.cpp", Paths.get("../lexica/resources/cpp_tokens.txt"));
		cpp.addMultipleProjects(Paths.get("C:\\Users\\Joel\\Desktop\\Testprojekte\\Cpp\\"), cpp);
		languages.add(cpp);			
		
	}
	
	public void loadTestDataSet(){
		outputLocation = Paths.get("C:\\Users\\Joel\\Desktop\\Testprojekte\\");
		
		Language java = new Language("Test","*.java", Paths.get("../lexica/resources/java_tokens.txt"));
		java.addMultipleProjects(Paths.get("C:\\Users\\Joel\\Desktop\\Testprojekte\\Test\\"), java);
		languages.add(java);
	}
	
	public Path getOutputLocation(){
		return outputLocation;
	}

	public  ArrayList<Language> getLanguages() {
		return languages;
	}
	
	public int getFileCount(){
		int k = 0;
		for(Language i: languages){
			for(Project j: i.getProjects()){
				k += j.getProjectFilePaths().size();
			}
		}
		return k;
	}
	
	public int getProjectCount(){
		int k = 0;
		for(Language i: languages){		
			k += i.getProjects().size();
		}
		return k;
	}	
}
