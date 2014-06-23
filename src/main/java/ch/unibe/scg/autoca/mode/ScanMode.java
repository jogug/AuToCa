/*
** Copyright 2013 Software Composition Group, University of Bern. All rights reserved.
*/
package ch.unibe.scg.autoca.mode;

import java.io.IOException;
import java.nio.file.Files;
import java.sql.SQLException;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.unibe.scg.autoca.DB;
import ch.unibe.scg.autoca.DataSet;
import ch.unibe.scg.autoca.SourceFileVisitor;
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
    private final int maxTokenLength = 1, minTokenLength = 27;


	public void initializeScanMode(DataSet dataset) {
		Objects.requireNonNull(dataset.getLanguages());
		try {
			//Create DB
			 db = new DB(dataset.getOutputLocation());
	        db.initialize();	             
	        
			//Prepare Projects	        
			for(int j= 0;j<dataset.getLanguages().size();j++){			 
				for(int i = 0;i<dataset.getLanguages().get(j).getProjects().size();i++){
					 logger.info("Initializing " + dataset.getLanguages().get(j).getName() + ", " + dataset.getLanguages().get(j).getProjects().get(i).getName());  						
					//Assign each Project an ID;
					dataset.getLanguages().get(j).getProjects().get(i).assignId(db);
					
			        //Tokenizing&Token Handling
			        th = new TokenHandler(db, minTokenLength,maxTokenLength);
			        tk = new Tokenizer(th);
			        tk.loadDefaults();	  
					
					//Extract Project Paths
					try {
						Files.walkFileTree( dataset.getLanguages().get(j).getProjects().get(i).getProjectPath(),
											new SourceFileVisitor(	dataset.getLanguages().get(j).getProjects().get(i),
																	dataset.getLanguages().get(j).getFilePattern()));
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}

		} catch (ClassNotFoundException | SQLException e) {
			e.printStackTrace();
		}  		
		
		logger.info("Initialization finished AuToCa found: " + dataset.getLanguages().size() + " Languages, " + dataset.getProjectCount() + " Projects, " + dataset.getFileCount() +" Files"  );
	}

	//TODO maybe add time/file
	@Override
	public void execute(DataSet dataset) {
        initializeScanMode(dataset); 
         					
		for(int j= 0;j<dataset.getLanguages().size();j++){
			for(int i = 0;i<dataset.getLanguages().get(j).getProjects().size();i++){	
				 logger.info(	dataset.getLanguages().get(j).getName() + " " +
					 		(j+1) + "/" + dataset.getLanguages().size() + ", " +
					 		dataset.getLanguages().get(j).getProjects().get(i).getName() + " " +
					 		(i+1) + "/" + dataset.getLanguages().get(j).getProjects().size() +
					 		", files: " + dataset.getLanguages().get(j).getProjects().get(i).getProjectFilePaths().size());  
				for(int k = 0;k<dataset.getLanguages().get(j).getProjects().get(i).getProjectFilePaths().size();k++){									
					try {													
						//Assign File ID
						db.insertFile(dataset.getLanguages().get(j).getProjects().get(i).getProjectFilePaths().get(k).getFileName().toString());
						//Tokenize & Insert Tokens
						tk.tokenize(dataset.getLanguages().get(j).getProjects().get(i).getProjectFilePaths().get(k).toFile());
						//Assign Token IDs
						db.assignTokensInTempTableIDs();
						//Fill Occurence Table
						db.insertOrderIDs();
						//Empty token_buffer table
						db.deleteTokenBuffer();
					} catch (SQLException e) {
						e.printStackTrace();
					}
				}
			}	
		}        	
  		
        
	}

}
