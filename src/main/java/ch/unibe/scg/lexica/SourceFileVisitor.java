/*
 ** Copyright 2013 Software Composition Group, University of Bern. All rights reserved.
 */
package ch.unibe.scg.lexica;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SourceFileVisitor extends SimpleFileVisitor<Path> {

    private static final Logger logger = LoggerFactory.getLogger(SourceFileVisitor.class);

    private final Graph graph;
    private final PathMatcher pathMatcher;
    private ArrayList<Integer> cflags;

    public SourceFileVisitor(Graph graph) throws SQLException, IOException {
        Objects.requireNonNull(graph);

        this.graph = graph;     

        pathMatcher = FileSystems.getDefault().getPathMatcher("glob:" + Configuration.getInstance().getFilePattern());
    }

    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
        Objects.requireNonNull(file);
        Objects.requireNonNull(attrs);

        Path name = file.getFileName();
        if (name != null && pathMatcher.matches(name)) {
            logger.debug("Parsing " + file.toString());

            try {
                graph.newFile();
            } catch (SQLException e) {
                logger.error("An error occured", e);

                return FileVisitResult.TERMINATE;
            }

            DelimiterParser parser = new DelimiterParser(graph);
            CommentParser commentParser = new CommentParser(Configuration.getInstance().getCommentPattern(),
            												Configuration.getInstance().getIgnorePattern());
           //Parse Comments flags
            cflags = commentParser.parse(Files.newBufferedReader(file, Charset.defaultCharset()));
           //Parse Delimiter Data
            if(cflags.size()%2==0){  
            	parser.parse(Files.newBufferedReader(file, Charset.defaultCharset()),cflags,"tokens");
            }
            
        }

        return FileVisitResult.CONTINUE;
    }

}
