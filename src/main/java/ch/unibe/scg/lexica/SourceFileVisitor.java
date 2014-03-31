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
import ch.unibe.scg.lexica.parser.CommentParser;

/**
 * Created for each file to be visited, handles parsing to be done
 * @author Joel
 *
 */
public class SourceFileVisitor extends SimpleFileVisitor<Path> {

    private static final Logger logger = LoggerFactory.getLogger(SourceFileVisitor.class);

    private final Graph graph;
    private final PathMatcher pathMatcher;
    private ArrayList<Integer> cflags;
    private ArrayList<Weight> weights;

    public SourceFileVisitor(Graph graph, ArrayList<Weight> weights) throws SQLException, IOException {
        Objects.requireNonNull(graph);
        this.weights = weights;
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
            	for(Weight i: weights){
                    graph.newFile(i.getTableName());
            	}            
            } catch (SQLException e) {
                logger.error("An error occured", e);

                return FileVisitResult.TERMINATE;
            }
            
            int minWL = Configuration.getInstance().getMin();
            int maxWL = Configuration.getInstance().getMax();
            
            CommentParser commentParser = new CommentParser(Configuration.getInstance().getCommentPattern(),
            												Configuration.getInstance().getIgnorePattern());
           //Parse Comments flags
            cflags = commentParser.parse(Files.newBufferedReader(file, Charset.defaultCharset()));
                        
            //Incase an Error occured while parsing the comments, cflags%2!= 0
            for(Weight i: weights){
                if(cflags.size()%2==0){  
                	//Parse Weights
                	i.getParser().parse(graph, Files.newBufferedReader(file, Charset.defaultCharset()),cflags,i.getTableName(), minWL, maxWL);
                }       
            }
            
        }
        return FileVisitResult.CONTINUE;
    }
}
