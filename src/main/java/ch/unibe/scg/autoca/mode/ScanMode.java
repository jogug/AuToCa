/*
** Copyright 2013 Software Composition Group, University of Bern. All rights reserved.
*/
package ch.unibe.scg.autoca.mode;

import java.io.IOException;
import java.nio.file.Files;
import java.sql.SQLException;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.unibe.scg.autoca.Configuration;
import ch.unibe.scg.autoca.DB;
import ch.unibe.scg.autoca.Project;
import ch.unibe.scg.autoca.SourceFileVisitor;

/**
 * Scans a filesystem for files to be evaluated
 * 
 * @author Joel
 */
public final class ScanMode implements IOperationMode {

    private static final Logger logger = LoggerFactory.getLogger(ScanMode.class);

    private final Project project;

    public ScanMode(Project project) {
        Objects.requireNonNull(project);
        this.project = project;
    }

    @Override
    public void execute() {
        logger.info("Scanning " + project.getFilePath().toString() + " with file pattern " + project.getLanguage().getfilePattern());

        try (DB db = new DB(project.getFilePath())) {     
        	db.initialize();
            Files.walkFileTree(project.getFilePath(), new SourceFileVisitor(db, project));
            db.print();
        } catch (IOException | ClassNotFoundException | SQLException e) {
           logger.error("Cannot walk the file tree", e);
        }                
    }
}
