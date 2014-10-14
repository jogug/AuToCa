/*
** Copyright 2013 Software Composition Group, University of Bern. All rights reserved.
*/
package ch.unibe.scg.autoca;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Holds the name and the paths to the languages, projects 
 * @author Joel
 */
public class Language {
    private static final Logger logger = LoggerFactory.getLogger(Language.class);	
	
	private String name; 
	private String filePattern;
	private Path tokenPath;
	private ArrayList<Project> projects;
	
	public Language(String name,String filePattern, Path tokenPath){
		this.filePattern = filePattern;
		this.name = name;
		this.tokenPath = tokenPath;
		this.projects = new ArrayList<Project>();
	}
	
	public void addProject(Project arg){
		projects.add(arg);
	}
	
	/**
	 * Adds all subfolders of a folder as Projects to the language
	 * @param argPath
	 * @param argLang
	 */
	public void addMultipleProjects(Path argPath){
		logger.info("Adding " + getName() + " projects:");
	    try (DirectoryStream<Path> stream = Files.newDirectoryStream(argPath)) {
	        for (Path path : stream) {
	        	if(Files.isDirectory(path)){
	        		projects.add(new Project(path, this));
	        	}
	        }
	    } catch (IOException e) {
	        e.printStackTrace();
	    }	
	}
	
	public String getName(){
		return name;
	}
	
	/**
	 * Code file pattern e.g ".java"
	 * @return
	 */
	public String getFilePattern(){
		return filePattern;
	}
	
	/**
	 * Paths to projects
	 * @return
	 */
	public ArrayList<Project> getProjects(){
		return projects;
	}
	
	/**
	 * Path to the actual tokens
	 * @return
	 */
	public Path getTokenPath(){
		return tokenPath;
	}
}
