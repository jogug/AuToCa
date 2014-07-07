/*
 ** Copyright 2013 Software Composition Group, University of Bern. All rights reserved.
 */
package ch.unibe.scg.autoca.mode;

import java.nio.file.Path;
import java.sql.SQLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.unibe.scg.autoca.DataSet;
import ch.unibe.scg.autoca.Language;
import ch.unibe.scg.autoca.Project;
import ch.unibe.scg.autoca.db.DB;
import ch.unibe.scg.autoca.db.TokenHandler;
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

	private DataSet dataset;

	public ScanMode(DataSet dataset) {
		this.dataset = dataset;

		initializeScanMode(dataset);
	}

	public void initializeScanMode(DataSet dataset) {
		logger.info("ScanMode Initialization");
		try {
			// Create DB
			db = new DB(dataset.getOutputLocation());
			db.initialize();

			// Tokenizing&Token Handling
			th = new TokenHandler(db, dataset.getMinTokenLength(), dataset.getMaxTokenLength());
			tk = new Tokenizer(th);
			tk.loadDefaults();

			// dataset.initializeProjects();
		} catch (ClassNotFoundException | SQLException e) {
			logger.error("Something went wrong: ", e);
		}
		logger.info("Finished ScanMode Initialization");
	}

	@Override
	public void execute() {
		logger.info("Starting Scan on dataset");
		langCounter = 0;
		projCounter = 0;

		for (Language language : dataset.getLanguages()) {
			langCounter++;
			processLanguage(language);
		}
		logger.info("Finished Scan on dataset");
	}
	
	//TODO move counter dependency to db
	private void processLanguage(Language language) {
		try {
			db.newLanguage(language.getName());

			for (Project project : language.getProjects()) {
				projCounter++;
				processProject(project);
			}

			db.languageFinished();
		} catch (ClassNotFoundException | SQLException e) {
			logger.error("Couldnt scan langauge " + language.getName() + " because: " + e.toString(), e);
		}

	}
	
	//TODO move counter dependency to db
	private void processProject(Project project) {
		int progressStep = calculateProgressbarStepSize(project);
		
		// Assign each Project an ID;
		try {
			db.newProject(project.getName(), langCounter);
			logger.info(project.getLanguage().getName() + " " + (langCounter) + ", " + project.getName() + " "
					+ (projCounter) + "/" + project.getLanguage().getProjects().size() + ", files: "
					+ project.getProjectFilePaths().size());
			
			double before = System.currentTimeMillis();
			processFiles(project, progressStep);
			System.out.println("		Average Time/File: " 
								+ (System.currentTimeMillis()-before)/project.getFileCount());
			
			db.projectFinished();
		} catch (SQLException e1) {
			logger.error("scan project errore: ", e1);
		}
	}

	private void processFiles(Project project, int progressStep) {
		int fileCounter = 0;
		int lastPrint = 0;
		for (Path path : project.getProjectFilePaths()) {
			try {
				// Assign File ID
				db.newFile(path.getFileName().toString(), projCounter);
				// Tokenize & Insert Tokens
				tk.tokenize(path.toFile());
				db.fileFinished();											
			} catch (SQLException e) {
				logger.error("Error occured during processFiles", e);
				e.printStackTrace();
			}
			
			//
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

	private int calculateProgressbarStepSize(Project project) {
		int result = (project.getProjectFilePaths().size()) / dataset.getProgressSteps();
		if (project.getProjectFilePaths().size() < dataset.getProgressSteps()) {
			result = 1;
		}
		return result;
	}

}
