/*
** Copyright 2013 Software Composition Group, University of Bern. All rights reserved.
*/
package ch.unibe.scg.lexica.mode;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.unibe.scg.lexica.Graph;
import ch.unibe.scg.lexica.Language;
import ch.unibe.scg.lexica.Weight;
import ch.unibe.scg.lexica.parser.DelimiterParser;

/**
 * Analyzes the tokens extracted from the code according to the actual tokesn of a language
 * @author Joel
 *
 */
public final class AnalyzeMode implements IOperationMode {

    private static final Logger logger = LoggerFactory.getLogger(AnalyzeMode.class);

    private Path path;
    private Language language;
    private Graph graph;
    private ArrayList<Weight> weights;

    public AnalyzeMode(Path path, ArrayList<Weight> weights, Language language){  	     	
        Objects.requireNonNull(path);
        this.path = path;  
        this.language = language;
        this.weights = weights;             
        try {
			graph = new Graph(path);
		} catch (ClassNotFoundException | SQLException e) {
			e.printStackTrace();
		}
        loadStandardData();
    }
    
    public void loadStandardData() {
    	DelimiterParser parser = new DelimiterParser();
        try {
        	graph.initTable(language.getName());
			parser.parse(graph, Files.newBufferedReader(language.getTokenPath(),Charset.defaultCharset()), new ArrayList<Integer>(),language.getName(),1,30);
		} catch (IOException | SQLException e) {
			e.printStackTrace();
		}
    }

    @Override
    public void execute() {
        logger.info("Analyzing " + path + "with tokens from :" + language.getTokenPath());
        for(Weight i: weights){
        	try {
    			graph.clearAnalyzeData(i.getTableName());
    			graph.initWeightTables(i.getTableName());
    			graph.calculateWeightFractions(i.getTableName(), language.getName());
    		} catch (SQLException e) {
    			e.printStackTrace();
    		}
        }
    }
}
