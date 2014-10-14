/*
** Copyright 2013 Software Composition Group, University of Bern. All rights reserved.
*/
package ch.unibe.scg.autoca.mode;

import java.sql.SQLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.unibe.scg.autoca.JSONInterface;
import ch.unibe.scg.autoca.Language;
import ch.unibe.scg.autoca.db.DB;
import ch.unibe.scg.autoca.db.DefaultTokenHandler;
import ch.unibe.scg.autoca.filter.FilterChain;
import ch.unibe.scg.autoca.tokenizer.Tokenizer;

/**
 * Analyzes the tokens extracted from the code according to the actual tokesn of a language
 * @author Joel
 *
 */
public final class AnalyzeMode implements IOperationMode {

    private static final Logger logger = LoggerFactory.getLogger(AnalyzeMode.class);

	private DB db;
	private JSONInterface dataset;

	
    public AnalyzeMode(JSONInterface dataset){  	     	
    	this.dataset = dataset;
    	try {
			this.db = new DB(dataset.getOutputLocation(), dataset);
		} catch (ClassNotFoundException | SQLException e) {
			logger.error("Error in AnalyzeMode creation",e);
		}
    }

	@Override
	public void execute() {
		logger.info("Starting AnalyzeMode: ");
		analyzeDataSet();	
		loadActualTokens();
		calculateStatisticsOnOutputTable();
		logger.info("Finished AnalyzeMode");
	}

	private void analyzeDataSet() {
		for(FilterChain filterChain: dataset.getFilterChain()){
			try {
				db.newAnalyzeLanguage();
				filterChain.execute(db);
				db.analyzeLanguageFinished();
			} catch (ClassNotFoundException | SQLException e) {
				e.printStackTrace();
			}
		}	
	}

	public void calculateStatisticsOnOutputTable(){
		for(FilterChain filterChain: dataset.getFilterChain()){
			for(String languageName: filterChain.getLanguageNames()){
				try {
					db.newLanguageStatistics();
					double result = db.calculateStatistics(languageName,languageName+filterChain.getResultName());
					logger.info("Percentage right keywords identified in top #of actual tokens from " + languageName+filterChain.getResultName() +": " + result);
					db.LanguageStatisticsFinished();
				} catch (SQLException | ClassNotFoundException e) {
					e.printStackTrace();
				}
			}
		}	
	}
	
	private void loadActualTokens() {
		DefaultTokenHandler th = new DefaultTokenHandler(db,dataset.getDEFAULT_MAX_TOKEN_LENGTH(),dataset.getDEFAULT_MIN_TOKEN_LENGTH());
		Tokenizer tk = new Tokenizer(th, dataset);
		tk.loadDefaults();
		
		for(Language language:dataset.getLanguages()){
			try {
				db.newActualTokenFile();
				tk.tokenize(language.getTokenPath().toFile());
				db.actualTokenFileFinished(language.getName());
			} catch (ClassNotFoundException | SQLException e) {
				logger.info("Couldnt load actual Token of: " + language.getName(), e);
			}
		}	
	}	
}
