/*
** Copyright 2013 Software Composition Group, University of Bern. All rights reserved.
*/
package ch.unibe.scg.autoca.mode;

import java.io.IOException;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.unibe.scg.autoca.DB;
import ch.unibe.scg.autoca.DataSet;
import ch.unibe.scg.autoca.Language;
import ch.unibe.scg.autoca.Project;
import ch.unibe.scg.autoca.TokenHandler;
import ch.unibe.scg.autoca.tokenizer.Tokenizer;

/**
 * Scans a DataSet for files to be evaluated
 * 
 * @author Joel
 */
public final class ScanMode implements IOperationMode {
    private static final Logger logger = LoggerFactory.getLogger(ScanMode.class);
    
    private DB db;
    private TokenHandler th;
    private Tokenizer tk;
    private int langCounter;
    private int projCounter;
    
    
    //TODO calculate good number for max
    //TODO pass on creation
    private final int maxTokenLength = 1;
    private final int minTokenLength = 27;


	public void initializeScanMode(DataSet dataset) {
    	logger.info("Starting Initialization");
		Objects.requireNonNull(dataset.getLanguages());
		try {
			//Create DB
			db = new DB(dataset.getOutputLocation());
	        db.initialize();	           
	        
	        //Tokenizing&Token Handling
	        th = new TokenHandler(db, minTokenLength,maxTokenLength);
	        tk = new Tokenizer(th);
	        tk.loadDefaults();	  
	        
	        dataset.initializeProjects();
		} catch (ClassNotFoundException | SQLException e) {
			e.printStackTrace();
		}  		
		
		logger.info("Finished initialization AuToCa found: " + dataset.getLanguages().size() + 
					" Languages, " + dataset.getProjectCount() + 
					" Projects, " + dataset.getFileCount() +" Files"  );
	}
	


	//TODO maybe add timeStamps/file
	@Override
	public void execute(DataSet dataset) {
    	logger.info("Starting scan on dataset");
    	langCounter = 0;
    	projCounter = 0;

        initializeScanMode(dataset); 
        
        for(Language language: dataset.getLanguages()){
        	langCounter++;
        	scanLanguage(language);
        }
         				           		 		
		try {
			db.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
    	logger.info("Finished scan on dataset");
	}
	
	private void scanLanguage(Language language){ 	    	
    	try {
			db.insertLanguage(language.getName());
		} catch (SQLException e2) {
			e2.printStackTrace();
		}
    	
    	for(Project project: language.getProjects()){
    		projCounter++;		
    		scanProject(project);
    	}    	
	}
	
	private void scanProject(Project project){
		int fileC = 0;
		int progressStep = calculateProgressbarStepSize(project);

		//Assign each Project an ID;
		try {
			db.insertProject(project.getName(), langCounter);
		} catch (SQLException e1) {
			e1.printStackTrace();
		}				
		
		logger.info(project.getLanguage().getName() + " " + (langCounter) + ", " +
			 		project.getName() + " " + (projCounter) + "/" + project.getLanguage().getProjects().size() +
			 		", files: " + project.getProjectFilePaths().size());  
		
		for(Path path: project.getProjectFilePaths()){
			fileC++;
			th.setFile(path.getFileName().toString());
			
			if(fileC%progressStep==0){
    			System.out.print(fileC*100/project.getProjectFilePaths().size()+"%,");
			}
			
			try {													
				//Assign File ID
				db.insertFile(path.getFileName().toString(), projCounter);
				//Tokenize & Insert Tokens
				tk.tokenize(path.toFile());
				db.handleTempTable();
			} catch (SQLException e) {
				e.printStackTrace();
			}      			
		}
		System.out.println();
	}
	
	private int calculateProgressbarStepSize(Project project){
		int result = (project.getProjectFilePaths().size()+1)/10;
 		if(project.getProjectFilePaths().size()<10){
 			result = 1;         		
 		}
 		return result;
	}

}
