package ch.unibe.scg.autoca;

import java.nio.file.Path;

public class Project {
	private int id;
	private Path filePath;
	private String name;
	private Language language;
	
	
	public Project(Path filePath, Language language){
		this.language = language;
		this.filePath = filePath;
		this.name = filePath.getFileName().toString();
	}
	
	public int getId(){
		return id;
	}
	
	public void setId(int argId){
		id = argId;		
	}
	
	public Path getFilePath(){
		return filePath;
	}
	
	public String getName(){
		return name;
	}
	
	public Language getLanguage(){
		return language;
	}
}
