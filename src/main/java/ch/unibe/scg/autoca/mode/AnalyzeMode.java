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
		loadActualTokens();
		analyzeDataSet();
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

//	private void analyzeDataSet(){
//		try {
//			db.newAnalyzeLanguage();
//			for(Language language:dataset.getLanguages()){	
//				logger.info("analyzing language: " + language.getName());
//				analyzeLanguage(language.getName());
//				for(Project project: language.getProjects()){
//					logger.info("analyzing project: "+project.getName());
//					analyzeProject(project.getName(), language.getName());
//				}			
//			}
//			db.analyzeLanguageFinished();
//		} catch (ClassNotFoundException | SQLException e) {
//			logger.error("Some error occured during Extraction",e);
//		}
//	}
//	
//	private void analyzeLanguage(String langName) throws SQLException{
//		if(global){
//			db.analyzeGlobalPerLanguage(langName, langName);
//		}
//		if(coverage){
//			db.analyzeCoveragePerLanguage(langName, langName);
//		}
//		if(simpleInd){
//			db.analyzeSimpleIndentPerLanguage(langName, langName);
//		}
//	}
//	
//	private void analyzeProject(String projName, String langName) throws SQLException{
//		if(global){
//			db.analyzeGlobalPerProject(projName, projName, langName);
//		}
//		if(coverage){
//			db.analyzeCoveragePerProject(projName, projName, langName);
//		}
//		if(simpleInd){
//			db.analyzeSimpleIndentPerProject(projName, langName, projName);
//		}
//	}	
	

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
