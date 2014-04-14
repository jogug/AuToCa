/*
** Copyright 2013 Software Composition Group, University of Bern. All rights reserved.
*/
package ch.unibe.scg.lexica.mode;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.unibe.scg.lexica.Configuration;
import ch.unibe.scg.lexica.DB;
import ch.unibe.scg.lexica.SourceFileVisitor;
import ch.unibe.scg.lexica.Weight;

/**
 * Scans a filesystem for files to be scanned
 * @author Joel
 *
 */
public final class ScanMode implements IOperationMode {

    private static final Logger logger = LoggerFactory.getLogger(ScanMode.class);

    private final Path path;
    private ArrayList<Weight> weights;

    public ScanMode(Path path, ArrayList<Weight> weights) {
        Objects.requireNonNull(path);
        this.weights = weights;
        this.path = path;
    }

    @Override
    public void execute() {
        logger.info("Scanning " + path.toString() + " with file pattern " + Configuration.getInstance().getFilePattern());

        try (DB graph = new DB(path)) {     
        	graph.initialize();
            Files.walkFileTree(path, new SourceFileVisitor(graph));
            graph.print();
        } catch (IOException | ClassNotFoundException | SQLException e) {
           logger.error("Cannot walk the file tree", e);
        }                
    }
}
