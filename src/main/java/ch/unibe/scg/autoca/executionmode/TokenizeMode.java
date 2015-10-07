/*
 ** Copyright 2013 Software Composition Group, University of Bern. All rights reserved.
 */
package ch.unibe.scg.autoca.executionmode;

import java.nio.file.Path;
import java.sql.SQLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.unibe.scg.autoca.database.Database;
import ch.unibe.scg.autoca.database.DatabaseTokenizerHandler;
import ch.unibe.scg.autoca.datastructure.Dataset;
import ch.unibe.scg.autoca.datastructure.Language;
import ch.unibe.scg.autoca.datastructure.Project;
import ch.unibe.scg.autoca.tokenizer.Tokenizer;

/**
 * A tokenizeMode of a dataSet extracts all possible tokens into a database at the 
 * output location
 * 
 * @author Joel
 */
public final class TokenizeMode implements IOperationMode {
	private static final Logger logger = LoggerFactory.getLogger(TokenizeMode.class);

	private Database db;
	private DatabaseTokenizerHandler th;
	private Tokenizer tk;

	private Dataset ds;

	public TokenizeMode(Dataset ds) {
		this.ds = ds;
		initialise(ds);
	}

	public void initialise(Dataset dataset) {
		logger.info("TokenizeMode Initialization");
		try {
			//open and clear Database
			db = new Database(dataset.getOutputLocation(), dataset);
			db.initialise();

			//Set tokenizier and tokenhandler properties
			th = new DatabaseTokenizerHandler(db, dataset);
			tk = new Tokenizer(th, dataset);
			tk.loadDefaults();

			// dataset.initializeProjects();
		} catch (ClassNotFoundException | SQLException e) {
			logger.error("Something went wrong: ", e);
		}
		logger.info("Finished TokenizeMode Initialization");
	}

	@Override
	public void execute() {
		logger.info("Tokenizing dataset");

		for (Language language : ds.getLanguages()) {
			processLanguages(language);
		}
		logger.info("\nFinished tokenizing on dataset");
	}
	
	private void processLanguages(Language language) {
		try {
			db.newLanguage(language);
			
			//If Data Limit set in config randomly select 
			
			for (Project project : language.getProjects()) {
				processProjects(project);
			}

			db.languageFinished();
		} catch (ClassNotFoundException | SQLException e) {
			logger.error("Couldnt tokenize langauge " + language.getName() + " because: " + e.toString(), e);
		}

	}
	
	private void processProjects(Project project) {
		int progressStep = calcOutputProjectProgressStep(project);
		
		// Assign each Project an ID;
		try {
			db.newProject(project);
			
			logger.info(project.getLanguage().getName() + " " 
					+ (project.getLanguage().getId()) + ", " 
					+ project.getName() + " "
					+ (project.getId()) + "/" + project.getLanguage().getProjects().size() 
					+ ", files: " + project.getProjectFilePaths().size());
			
			double before = System.currentTimeMillis();
			processFiles(project, progressStep);
			System.out.println("		Average Time/File: " 
										+ (System.currentTimeMillis()-before)/project.getFileCount());
			
			db.projectFinished();
		} catch (SQLException e1) {
			logger.error("Error while processing project", e1);
		}
	}

	private void processFiles(Project project, int progressStep) {
		int fileCounter = 0;
		int lastPrint = 0;
		for (Path path : project.getProjectFilePaths()) {
			try {
				// Assign File ID
				db.newFile(path.getFileName().toString(), project.getId());
				// Tokenize & Insert Tokens
				tk.tokenize(path.toFile());
				db.fileFinished();											
			} catch (SQLException e) {
				logger.error("Error occured during processFiles", e);
				e.printStackTrace();
			}
			
			fileCounter++;
			if (fileCounter % progressStep == 0) {
				lastPrint = fileCounter * 100 / project.getFileCount();
				System.out.print(lastPrint + "%,");
			}
		}
		if(lastPrint!=100){
			System.out.print("100%,");
		}
	}

	private int calcOutputProjectProgressStep(Project project) {
		int result = (project.getProjectFilePaths().size()) / ds.getDEFAULT_PROGRESS_STEPS();
		if (project.getProjectFilePaths().size() < ds.getDEFAULT_PROGRESS_STEPS()) {
			result = 1;
		}
		return result;
	}
}
