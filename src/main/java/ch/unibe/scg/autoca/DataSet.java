/*
 ** Copyright 2013 Software Composition Group, University of Bern. All rights reserved.
 */

package ch.unibe.scg.autoca;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.ArrayList;

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
	private ArrayList<Language> languages;
	private Path outputLocation;	
	private boolean initialized;
	
	public DataSet(ArrayList<Language> language){
		//TODO when passed from command prompt
		this.languages = language;
	}
	
	public DataSet(){
		languages = new ArrayList<Language>();
		initialized = false;
	}
	
	public void loadStandardDataSet() {		
		outputLocation = Paths.get("../AuToCa/resources/");
		
		Language java = new Language("Java","*.java", Paths.get("../AuToCa/resources/java_tokens.txt"));
		java.addMultipleProjects(Paths.get("../AuToCa/resources/testprojects/java/"), java);
		languages.add(java);
		
		Language c = new Language("C","*.c", Paths.get("../AuToCa/resources/c_tokens.txt"));
		c.addMultipleProjects(Paths.get("../AuToCa/resources/testprojects/C/"), c);
		languages.add(c);	
		
		Language python = new Language("Python", "*.py", Paths.get("../AuToCa/resources/python_tokens.txt"));
		python.addMultipleProjects(Paths.get("../AuToCa/resources/testprojects/Python/"), python);
		languages.add(python);
		
		Language cpp = new Language("Cpp", "*.cpp", Paths.get("../AuToCa/resources/cpp_tokens.txt"));
		cpp.addMultipleProjects(Paths.get("../AuToCa/resources/testprojects/Cpp/"), cpp);
		languages.add(cpp);			
		
	}
	
	//TODO Put in test class
	public void loadTestDataSet(){
		outputLocation = Paths.get("C:/Users/Joel/Desktop/Testprojekte/");
		
		Language java = new Language("Test","*.java", Paths.get("../AuToCa/resources/java_tokens.txt"));
		java.addMultipleProjects(Paths.get("C:/Users/Joel/Desktop/Testprojekte/Test/"), java);
		languages.add(java);
	}
	
	/**
	 * Extracts file paths of all projects in all languages
	 */
	public void initializeProjects() {   
		for(Language language: languages){
			for(Project project: language.getProjects()){
				logger.info("found " + language.getName() + ", " + project.getName());  				 				
				try {
					Files.walkFileTree( project.getProjectPath(), new SourceFileVisitor(project,language.getFilePattern()));
				} catch (IOException | SQLException e) {
					e.printStackTrace();
				}						
			}
		}			
		initialized = true;
	}	
	
	/**
	 * Extracts only the files affected by a scan from all the project directories into 
	 * "\\Extracted"+sourceName directories. Files with the same name are copied in
	 * ascending numbered sub folders. 
	 */
	public void extractSourceFiles() {	
		logger.info("Starting extraction of source files from: " + outputLocation.toString());
		if(!initialized) initializeProjects();
		String outputExtractedSource = outputLocation.toString() + "\\Extracted" + outputLocation.getFileName().toString();

		if((new File(outputExtractedSource)).exists()){
			logger.info("Output folder for extracted source files already exists => skipped source file extraction,"+
						"try deleting: "+outputExtractedSource);
		}else{
			(new File(outputExtractedSource)).mkdirs();
				
			for(Language language: languages){
				String languagePath = outputExtractedSource + "\\" + language.getName().toString();
				createFileIfNotExists(languagePath);				
				for(Project project: language.getProjects()){
					String projectPath = languagePath + "\\" + project.getName().toString();
					createFileIfNotExists(projectPath);					
					for(Path path: project.getProjectFilePaths()){	
						int count = 0;
						String destination = projectPath + "\\" +count+"\\"+ path.getFileName().toString();
						createFileIfNotExists(projectPath + "\\" +count);						
						while((new File(destination).exists())){
							count++;
							destination = projectPath + "\\" +count+"\\"+ path.getFileName().toString();							
							createFileIfNotExists(projectPath + "\\" +count);
						}						
						try {
							Files.copy(path,Paths.get(destination));
						} catch (IOException e) {
							e.printStackTrace();
						}
					}			
				}
			}
		}				
		logger.info("Finished extraction to: " + outputExtractedSource);
	}
	
	private void createFileIfNotExists(String path){
		if(!(new File(path).exists())){
			(new File(path)).mkdirs();
		}
	}
	
	public int getFileCount(){
		int count = 0;
		for(Language i: languages){
			for(Project j: i.getProjects()){
				count += j.getProjectFilePaths().size();
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

	public  ArrayList<Language> getLanguages() {
		return languages;
	}
}
