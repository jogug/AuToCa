/*
** Copyright 2013 Software Composition Group, University of Bern. All rights reserved.
*/
package ch.unibe.scg.autoca;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

/**
 * Holds name and the location of the actual Tokens of a Language
 * @author Joel
 *
 */
public class Language {
	private String name, filePattern;
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
	public void addMultipleProjects(Path argPath, Language argLang){
		System.out.println("aaa: " + argPath.toAbsolutePath());
		    try (DirectoryStream<Path> stream = Files.newDirectoryStream(argPath)) {
		        for (Path path : stream) {
		        	if(Files.isDirectory(path)){
		        		projects.add(new Project(path, argLang));
		        	}
		        }
		    } catch (IOException e) {
		        e.printStackTrace();
		    }	
	}
	
	public String getName(){
		return name;
	}
	
	public String getFilePattern(){
		return filePattern;
	}
	
	public ArrayList<Project> getProjects(){
		return projects;
	}
	
	public Path getTokenPath(){
		return tokenPath;
	}
}
