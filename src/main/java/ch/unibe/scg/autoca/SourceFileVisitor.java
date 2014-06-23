/*
 ** Copyright 2013 Software Composition Group, University of Bern. All rights reserved.
 */
package ch.unibe.scg.autoca;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.sql.SQLException;
import java.util.Objects;

/**
 * Visits File
 * @author Joel
 *
 */
public class SourceFileVisitor extends SimpleFileVisitor<Path> {
    
    private final Project project;
    private final PathMatcher pathMatcher;

    public SourceFileVisitor( Project project, String filePattern) throws SQLException, IOException {
    	this.project = project;
        pathMatcher = FileSystems.getDefault().getPathMatcher("glob:" + filePattern);
    }

    @Override
    public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) throws IOException {
        Objects.requireNonNull(path);
        Objects.requireNonNull(attrs);        
        Path name = path.getFileName();
        if (name != null && pathMatcher.matches(name)) {         
            project.addFile(path);
        }
        return FileVisitResult.CONTINUE;
    }
}
