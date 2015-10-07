package ch.unibe.scg.autoca.datastructure;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Holds the name of the projects and the paths to its files 
 * @author Joel
 */
public class Project {
    private static final Logger logger = LoggerFactory.getLogger(Dataset.class);	
	
	private int id;
	private Path projectPath;
	private String name;
	private Language language;
	private List<Path> filePaths = new ArrayList<>();
	
	public Project(Path projectPath, Language language){
		this.language = language;
		this.projectPath = projectPath;
		this.name = projectPath.getFileName().toString();
		
		loadFilePaths();
	}	
	
	public Project(Path projectPath,String projectName, Language language){
		this.language = language;
		this.projectPath = projectPath;
		this.name = projectName;
		
		loadFilePaths();
	}
	
	public List<Path> getProjectFilePaths(){
		return filePaths;
	}

	public int getId(){
		return id;
	}
	
	public void setId(int argId){
		id = argId;
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
	
	public int getFileCount() {
		return filePaths.size();
	}
	
	public long getSize(){
		return folderSize(new File(projectPath.toString()));
	}

	public static long folderSize(File directory) {
	    long length = 0;
	    for (File file : directory.listFiles()) {
	        if (file.isFile())
	            length += file.length();
	        else
	            length += folderSize(file);
	    }
	    return length;
	}
	
	private void loadFilePaths() {
		logger.info("Extracting paths form: " + getName());
		try {
			Files.walkFileTree( getProjectPath(), new SourceFileVisitor());
		} catch (IOException e) {
			logger.error("Error occured in project loadFilePaths", e);
		}
	}
	
	private class SourceFileVisitor extends SimpleFileVisitor<Path> {
	    private final PathMatcher pathMatcher;

	    public SourceFileVisitor() throws IOException {
	        pathMatcher = FileSystems.getDefault().getPathMatcher("glob:" + language.getFilePattern());
	    }

	    @Override
	    public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) throws IOException {
	        Path name = path.getFileName();
	        if (name != null && pathMatcher.matches(name)) {         
	            filePaths.add(path);
	        }
	        return FileVisitResult.CONTINUE;
	    }
	}
}
