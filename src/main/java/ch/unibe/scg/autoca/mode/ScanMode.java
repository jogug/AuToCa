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
		int langC = 0;
		int projC = 0;
        initializeScanMode(dataset); 
        
        for(Language language: dataset.getLanguages()){
        	langC++;
        	for(Project project: language.getProjects()){
        		projC++;
        		int fileC = 0;
				//Assign each Project an ID;
				try {
					db.insertProject(project.getName());
				} catch (SQLException e1) {
					e1.printStackTrace();
				}				
				
				logger.info(language.getName() + " " + (langC) + "/" + dataset.getLanguages().size() + ", " +
					 		project.getName() + " " + (projC) + "/" + language.getProjects().size() +
					 		", files: " + project.getProjectFilePaths().size());  
        		for(Path path: project.getProjectFilePaths()){
        			fileC++;
        			System.out.print(fileC+",");
        			
					try {													
						//Assign File ID
						db.insertFile(path.getFileName().toString());
						//Tokenize & Insert Tokens
						tk.tokenize(path.toFile());
						db.handleTempTable();
					} catch (SQLException e) {
						e.printStackTrace();
					}
        			
        		}
        	}
        }
         				           		 		
		try {
			db.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
    	logger.info("Finished scan on dataset");
	}

}
