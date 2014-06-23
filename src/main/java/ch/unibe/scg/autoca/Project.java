package ch.unibe.scg.autoca;

import java.nio.file.Path;
import java.sql.SQLException;
import java.util.ArrayList;

public class Project {
	private int id;
	private Path projectPath;
	private String name;
	private Language language;
	private ArrayList<Path> filePaths;
	
	
	public Project(Path projectPath, Language language){
		this.language = language;
		this.projectPath = projectPath;
		this.name = projectPath.getFileName().toString();
		this.filePaths = new ArrayList<Path>();
	}
	
	public void assignId(DB db){
		try {
			db.insertProject(name);
			id = db.getProjectId(name);
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	public void addFile(Path path){
		filePaths.add(path);
	}
	
	public ArrayList<Path> getProjectFilePaths(){
		return filePaths;
	}
	
	public int getId(){
		return id;
	}
	
	public Path getProjectPath(){
		return projectPath;
	}
	
	public String getName(){
		return name;
	}
	
	public Language getLanguage(){
		return language;
	}
}
