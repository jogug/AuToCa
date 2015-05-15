/*
** Copyright 2013 Software Composition Group, University of Bern. All rights reserved.
*/
package ch.unibe.scg.autoca.structure;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Holds the name and the paths to the languages, projects 
 * @author Joel
 */
public class Language {
    private static final Logger logger = LoggerFactory.getLogger(Language.class);	
	
    private int id;
	private String name; 
	private String filePattern;
	private Path tokenPath;
	private long projectSizeLimit;
	private int minAmountOfProjects;
	private ArrayList<Project> projects;
	
	public Language(String name,String filePattern, Path tokenPath, long projectSizeLimit, int minAmountOfProjects){
		this.filePattern = filePattern;
		this.name = name;
		this.tokenPath = tokenPath;
		this.projects = new ArrayList<Project>();
		this.projectSizeLimit = projectSizeLimit;
		this.minAmountOfProjects = minAmountOfProjects;
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
	    	
	    	//Project Picker
	    	//select project where >2 but <size
	    	List<Project> temp = new ArrayList<Project>();
	        for (Path path : stream) {
	        	if(Files.isDirectory(path)){		        
	        		temp.add(new Project(path, this));	        		
	        	}
	        }
	        
        	long availableData= 0;
        	for(Project x: temp){
        		availableData+=x.getSize();
        	}
	        
	        int i = 1;
	        boolean found = false;
	        Random random = new Random(System.currentTimeMillis());
	        List<Integer> set = new ArrayList<Integer>();
	        
	        if(availableData<projectSizeLimit||(temp.size()==1&&minAmountOfProjects==1)){
	        	for(int a = 0;a< temp.size();a++){
		        	set.add(a);
	        	}
	        	found = true;
	        	logger.info("Found subset of size: " + availableData + " with " + set.size() + " projects, on " + i + " try");
	        }else{
		        while(i<10&&!found){
		        	long size = 0;	       
		        	set.clear();
		        	while(size<projectSizeLimit&&set.size()<temp.size()){
		        		int next =  random.nextInt(temp.size()-1);
		        		Project toAdd = temp.get(next);
		        		size += toAdd.getSize();
		        		if(toAdd.getSize()<projectSizeLimit/minAmountOfProjects && !set.contains(next)){
			        		set.add(next);
		        		}
		        	}
		        	if(set.size()>=minAmountOfProjects){
		        		found = true;
		        		logger.info("Found subset of size: " + size + " with " + set.size() + " projects, on " + i + " try");
		        	}
		        	i++;
		        }	
	        }
        
	        
	        if(!found){
	        	logger.debug("couldn't find projects with this settings size limit settings");
	        }else{
		        for(int nr: set){
		        	projects.add(temp.get(nr));
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
	
	public void setId(int argId){
		id = argId;
	}
	
	public int getId(){
		return id;
	}
}
