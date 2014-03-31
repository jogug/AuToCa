/*
** Copyright 2013 Software Composition Group, University of Bern. All rights reserved.
*/
package ch.unibe.scg.lexica;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Analyzes the tokens extracted from the Code according to their weight
 * @author Joel
 *
 */
public final class AnalyzeMode implements IOperationMode {

    private static final Logger logger = LoggerFactory.getLogger(AnalyzeMode.class);

    private final Path path, actualTokenPath;
    private final Graph graph;
    private final DelimiterParser parser;
    private int n;

    public AnalyzeMode(Path path, Path actualTokenPath, int n) throws SQLException, ClassNotFoundException {  	     	
        Objects.requireNonNull(path);
        this.n = n;
        this.actualTokenPath = actualTokenPath;
        this.path = path;  
        graph = new Graph(path, false);
        parser = new DelimiterParser(graph);
        graph.analyzeInit();
    }
    
    public void loadStandardData() {
        try {
			parser.parse(Files.newBufferedReader(actualTokenPath,Charset.defaultCharset()), new ArrayList<Integer>(),"actualTokenT",1,10);
		} catch (IOException e) {
			e.printStackTrace();
		}
    }

    @Override
    public void execute() {
        logger.info("Analyzing " + path + "with tokens from :" + actualTokenPath);
        
    	try {
			graph.clearAnalyzeData();
		} catch (SQLException e1) {
			e1.printStackTrace();
		}
    	
        loadStandardData();
                
        try {
			graph.calculateFractions(n);
		} catch (SQLException e) {
			e.printStackTrace();
		}
    }
}
