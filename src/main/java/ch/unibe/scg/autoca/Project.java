package ch.unibe.scg.autoca;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Project {
    private static final Logger logger = LoggerFactory.getLogger(DataSet.class);	
	
	private int id;
	private Path projectPath;
	private String name;
	private Language language;
	private List<Path> filePaths;
	
	
	public Project(Path projectPath, Language language){
		this.language = language;
		this.projectPath = projectPath;
		this.name = projectPath.getFileName().toString();
		
		loadFilePaths();
	}
	
	
	public List<Path> getProjectFilePaths(){
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
	
	public int getFileCount() {
		return filePaths.size();
	}

	private void loadFilePaths() {
		try {
			Files.walkFileTree( getProjectPath(), new SourceFileVisitor());
		} catch (IOException e) {
			logger.error("", e);
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
