/*
 ** Copyright 2013 Software Composition Group, University of Bern. All rights reserved.
 */
package ch.unibe.scg.autoca.mode;

import java.nio.file.Path;
import java.sql.SQLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.unibe.scg.autoca.config.JSONInterface;
import ch.unibe.scg.autoca.db.DB;
import ch.unibe.scg.autoca.db.DBTokenHandler;
import ch.unibe.scg.autoca.structure.Language;
import ch.unibe.scg.autoca.structure.Project;
import ch.unibe.scg.autoca.tokenizer.Tokenizer;

/**
 * A scanmode of a dataSet extracts all possible tokens into a database at the 
 * output location
 * 
 * @author Joel
 */
public final class ScanMode implements IOperationMode {
	private static final Logger logger = LoggerFactory.getLogger(ScanMode.class);

	private DB db;
	private DBTokenHandler th;
	private Tokenizer tk;
	private int langCounter;
	private int projCounter;

	private JSONInterface dataset;

	public ScanMode(JSONInterface dataset) {
		this.dataset = dataset;
		initializeScanMode(dataset);
	}

	public void initializeScanMode(JSONInterface dataset) {
		logger.info("ScanMode Initialization");
		try {
			//open and clear Database
			db = new DB(dataset.getOutputLocation(), dataset);
			db.initialize();

			//Set tokenizier and tokenhandler properties
			th = new DBTokenHandler(db, dataset);
			tk = new Tokenizer(th, dataset);
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
			processLanguages(language);
		}
		logger.info("\nFinished Scan on dataset");
	}
	
	//TODO move counter dependency to db
	private void processLanguages(Language language) {
		try {
			db.newLanguage(language.getName());

			for (Project project : language.getProjects()) {
				projCounter++;
				processProjects(project);
			}

			db.languageFinished();
		} catch (ClassNotFoundException | SQLException e) {
			logger.error("Couldnt scan langauge " + language.getName() + " because: " + e.toString(), e);
		}

	}
	
	//TODO move counter dependency to db
	private void processProjects(Project project) {
		int progressStep = calcOutputProjectProgressStep(project);
		
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

	private int calcOutputProjectProgressStep(Project project) {
		int result = (project.getProjectFilePaths().size()) / dataset.getDEFAULT_PROGRESS_STEPS();
		if (project.getProjectFilePaths().size() < dataset.getDEFAULT_PROGRESS_STEPS()) {
			result = 1;
		}
		return result;
	}
}
