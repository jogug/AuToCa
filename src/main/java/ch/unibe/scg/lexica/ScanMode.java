/*
** Copyright 2013 Software Composition Group, University of Bern. All rights reserved.
*/
package ch.unibe.scg.lexica;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.Objects;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ScanMode implements IOperationMode {

    private static final Logger logger = LoggerFactory.getLogger(ScanMode.class);

    private final Path path;

    public ScanMode(Path path) {
        Objects.requireNonNull(path);
        this.path = path;
    }

    @Override
    public void execute() {
        logger.info("Scanning " + path.toString() + " with file pattern " + Configuration.getInstance().getFilePattern());

        try (Graph graph = new Graph(path, true)) {        	
            Files.walkFileTree(path, new SourceFileVisitor(graph));
            graph.print();
        } catch (IOException | ClassNotFoundException | SQLException e) {
           logger.error("Cannot walk the file tree", e);
        }                
    }
}
