/*
** Copyright 2013 Software Composition Group, University of Bern. All rights reserved.
*/
package ch.unibe.scg.autoca.executionmode;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.unibe.scg.autoca.database.Database;
import ch.unibe.scg.autoca.datastructure.Dataset;
import ch.unibe.scg.autoca.datastructure.Language;
import ch.unibe.scg.autoca.filter.FilterChain;
import ch.unibe.scg.autoca.tokenizer.Tokenizer;
import ch.unibe.scg.autoca.utilities.DefaultTokenHandler;

/**
 * Analyzes the tokens extracted from the code according to the actual tokens of a language
 * @author Joel
 *
 */
public final class AnalyzeMode implements IOperationMode {

    private static final Logger logger = LoggerFactory.getLogger(AnalyzeMode.class);

	private Database db;
	private Dataset ds;

	
    public AnalyzeMode(Dataset dataset){  	     	
    	this.ds = dataset;
    	try {
			this.db = new Database(dataset.getOutputLocation(), dataset);
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
			for(Language language:ds.getLanguages()){
				db.dropTableIfExists(ds.getPRECISION()+language.getName());
				db.createPrecisionSummaryTable(language.getName());
			}
			for(FilterChain filter: ds.getFilterChain()){
				for(String language: filter.getLanguageNames()){
					db.insertPrecision(filter.getResultName(), language);
				}
			}

		} catch (SQLException e) {
			logger.error("Database access error in summarize AnalyzeMode 1");
			e.printStackTrace();
		}
		

		try {
			for(Language language:ds.getLanguages()){
				List<String> chains = new ArrayList<String>();
				for(FilterChain chain: ds.getFilterChain()){
					for(String languageInChain: chain.getLanguageNames()){
						if(language.getName().equals(languageInChain)){
							chains.add(chain.getResultName());
						}
					}
				}
				db.dropTableIfExists(ds.getRANK()+language.getName());		
				db.createRanksSummaryTable(language.getName(), chains);
				db.createSummaryOfDatasetStatistics(language.getName());
			}
		} catch (SQLException e) {
			logger.error("Database access error in summarize AnalyzeMode 2");
			e.printStackTrace();
		}
	}

	private void analyzeDataSet() {
		//clear resulttable
		try {
			db.newAnalyseDatasetOfLanguage();
			db.dropTableIfExists(ds.getRESULTTABLE());
			db.createResulttable();
			db.analyseDatasetOfLanguageFinished();
		} catch (ClassNotFoundException | SQLException e) {
			e.printStackTrace();
		}
		
		for(FilterChain filterChain: ds.getFilterChain()){
			try {
				db.newAnalyseDatasetOfLanguage();
				filterChain.execute(db);
				db.analyseDatasetOfLanguageFinished();
			} catch (ClassNotFoundException | SQLException e) {
				e.printStackTrace();
			}
		}	
	}
	
	private void loadActualTokens() {
		DefaultTokenHandler th = new DefaultTokenHandler(db,ds.getDEFAULT_MAX_TOKEN_LENGTH(),ds.getDEFAULT_MIN_TOKEN_LENGTH());
		Tokenizer tk = new Tokenizer(th, ds);
		tk.loadDefaults();
		
		for(Language language:ds.getLanguages()){
			try {
				db.newLanguagesActualTokenFile();
				tk.tokenize(language.getTokenPath().toFile());
				db.languagesActualTokenFileFinished(language.getName());
			} catch (ClassNotFoundException | SQLException e) {
				logger.info("Couldnt load actual Token of: " + language.getName(), e);
			}
		}	
	}

}
