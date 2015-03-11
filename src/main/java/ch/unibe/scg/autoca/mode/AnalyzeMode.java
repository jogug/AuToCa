/*
** Copyright 2013 Software Composition Group, University of Bern. All rights reserved.
*/
package ch.unibe.scg.autoca.mode;

import java.sql.SQLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.unibe.scg.autoca.config.JSONInterface;
import ch.unibe.scg.autoca.db.DB;
import ch.unibe.scg.autoca.db.DefaultTokenHandler;
import ch.unibe.scg.autoca.filter.FilterChain;
import ch.unibe.scg.autoca.structure.Language;
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
		loadActualTokens();
		analyzeDataSet();	
		summarize();
		logger.info("Finished AnalyzeMode");
	}

	private void summarize() {
		try {
			for(Language language:dataset.getLanguages()){
				db.dropTableIfExists(dataset.getSUMMARY()+language.getName());
				db.createSummaryTable(language.getName(), db.getRowCountOfTable(language.getName()));
			}
			
			for(FilterChain chain: dataset.getFilterChain()){
				for(String language: chain.getLanguageNames()){
					db.insertStatisticsInSummary(chain.getResultName(), language, chain.getResultName(),db.getRowCountOfTable(language));
				}
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void analyzeDataSet() {
		//clear resulttable
		try {
			db.newAnalyzeLanguage();
			db.dropTableIfExists(dataset.getRESULTTABLE());
			db.createResulttable();
			db.analyzeLanguageFinished();
		} catch (ClassNotFoundException | SQLException e) {
			e.printStackTrace();
		}
		
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
