/*
** Copyright 2013 Software Composition Group, University of Bern. All rights reserved.
*/
package ch.unibe.scg.autoca.mode;

import java.sql.SQLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.unibe.scg.autoca.DB;
import ch.unibe.scg.autoca.DataSet;
import ch.unibe.scg.autoca.Language;
import ch.unibe.scg.autoca.Project;

/**
 * Analyzes the tokens extracted from the code according to the actual tokesn of a language
 * @author Joel
 *
 */
public final class AnalyzeMode implements IOperationMode {

    private static final Logger logger = LoggerFactory.getLogger(AnalyzeMode.class);

	private DB db;
	private DataSet dataset;
	private boolean global;
	private boolean coverage;
	private boolean simpleInd;
	private final String OCC = "OCCURENCES_"; 
	private final String GLO = "GLOBAL_"; 
	private final String COV = "COVERAGE_";
	private final String SIND = "SIMIND_";
	
    public AnalyzeMode(DataSet dataset,boolean global, boolean coverage, boolean simpleInd){  	     	
    	this.dataset = dataset;
    	this.global = global;
    	this.coverage = coverage;
    	this.simpleInd = simpleInd;
    	try {
			this.db = new DB(dataset.getOutputLocation());
		} catch (ClassNotFoundException | SQLException e) {
			logger.error("Error in AnalyzeMode creation",e);
		}
    }

	@Override
	public void execute() {
		logger.info("Starting AnalyzeMode");
		// Deletes old Tables
		// Extract Language Data 
		extractLanguageData();
		analyzeDataSet();
		logger.info("Finished AnalyzeMode");
	}

	private void extractLanguageData() {
		try {
			db.newExtractLanguage();
			for(Language language:dataset.getLanguages()){	
				logger.info("extracting language table: " + language.getName());
				db.dropOldTableIfExists(OCC+language.getName());
				db.extractLanguageFromOccurences(OCC+language.getName(), language.getName());
			}
			db.extractLanguageFinished();
		} catch (ClassNotFoundException | SQLException e) {
			logger.error("Something happened during Extraction",e);
		}
		logger.info("Finished language table extraction");
	}
	
	private void analyzeDataSet(){
		try {
			db.newAnalyzeLanguage();
			for(Language language:dataset.getLanguages()){	
				logger.info("Analyzing: " + language.getName());
				analyzeLanguage(language.getName());			
				for(Project project: language.getProjects()){
					logger.info("Analyzing project: "+project.getName());
					analyzeProject(project.getName(), language.getName());
				}
			}
			db.analyzeLanguageFinished();
		} catch (ClassNotFoundException | SQLException e) {
			logger.error("Some error occured during Extraction",e);
		}
	}
	
	private void analyzeLanguage(String langName) throws SQLException{
		if(global){
			db.dropOldTableIfExists(GLO+langName);
			db.analyzeGlobalPerLanguage(GLO+langName, OCC+langName);
		}
		if(coverage){
			db.dropOldTableIfExists(COV+langName);
			db.analyzeCoveragePerLanguage(COV+langName, OCC+langName);
		}
		if(simpleInd){
			
		}
	}
	
	private void analyzeProject(String projName, String langName) throws SQLException{
		if(global){
			db.dropOldTableIfExists(GLO+projName);
			db.analyzeGlobalPerProject(GLO+projName, projName, OCC+langName);
		}
		if(coverage){
			db.dropOldTableIfExists(COV+projName);
			db.analyzeCoveragePerProject(COV+projName, projName, OCC+langName);
		}
		if(simpleInd){
			
		}
	}
	

}
