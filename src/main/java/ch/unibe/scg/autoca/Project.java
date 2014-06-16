package ch.unibe.scg.autoca;

import java.nio.file.Path;

public class Project {
	private static int count = 0;
	private int id;
	private Path filePath;
	private String name;
	private Language language;
	
	
	public Project(Path filePath, Language language){
		id = count;
		count++;
		this.language = language;
		this.filePath = filePath;
		this.name = filePath.getFileName().toString();
	}
	
	public int getID(){
		return id;
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
