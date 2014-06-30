/*
 ** Copyright 2013 Software Composition Group, University of Bern. All rights reserved.
 */
package ch.unibe.scg.autoca.mode;

import java.nio.file.Path;
import java.sql.SQLException;

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

	private DataSet dataset;

	// TODO calculate good number for max
	// TODO pass on creation
	private final int DEFAULT_MAX_TOKEN_LENGTH = 1;
	private final int DEFAULT_MIN_TOKEN_LENGTH = 27;

	public ScanMode(DataSet dataset) {
		this.dataset = dataset;

		initializeScanMode(dataset);
	}

	public void initializeScanMode(DataSet dataset) {
		logger.info("Starting Initialization");

		try {
			// Create DB
			db = new DB(dataset.getOutputLocation());
			db.initialize();

			// Tokenizing&Token Handling
			th = new TokenHandler(db, DEFAULT_MIN_TOKEN_LENGTH, DEFAULT_MAX_TOKEN_LENGTH);
			tk = new Tokenizer(th);
			tk.loadDefaults();

			// dataset.initializeProjects();
		} catch (ClassNotFoundException | SQLException e) {
			logger.error("Something went wrong: ", e);
		}

		logger.info("Finished initialization AuToCa found: " + dataset.getLanguages().size() + " Languages, "
				+ dataset.getProjectCount() + " Projects, " + dataset.getFileCount() + " Files");
	}

	// TODO maybe add timeStamps/file
	@Override
	public void execute() {
		logger.info("Starting scan on dataset");
		langCounter = 0;
		projCounter = 0;

		for (Language language : dataset.getLanguages()) {
			langCounter++;
			processLanguage(language);
		}

		logger.info("Finished scan on dataset");
	}

	private void processLanguage(Language language) {
		try {
			db.newLanguage(language.getName());

			for (Project project : language.getProjects()) {
				projCounter++;
				processProject(project);
			}

			db.languageFinished(language.getName());
		} catch (ClassNotFoundException | SQLException e) {
			logger.error("Couldnt scan langauge " + language.getName() + " because: " + e.toString(), e);
		}

	}

	private void processProject(Project project) {
		int progressStep = calculateProgressbarStepSize(project);
		// Assign each Project an ID;
		try {
			db.newProject(project.getName(), langCounter);

			logger.info(project.getLanguage().getName() + " " + (langCounter) + ", " + project.getName() + " "
					+ (projCounter) + "/" + project.getLanguage().getProjects().size() + ", files: "
					+ project.getProjectFilePaths().size());

			processFiles(project, progressStep);

			System.out.println();
			db.projectFinished();
		} catch (SQLException e1) {
			logger.error("scan project errore: ", e1);
		}
	}

	private void processFiles(Project project, int progressStep) {
		int fileC = 0;
		for (Path path : project.getProjectFilePaths()) {
			th.setFile(path.getFileName().toString());

			if (fileC % progressStep == 0) {
				System.out.print(fileC * 100 / project.getFileCount() + "%,");
			}

			try {

				// Assign File ID
				db.newFile(path.getFileName().toString(), projCounter);
				// Tokenize & Insert Tokens
				tk.tokenize(path.toFile());
				db.fileFinished();

				fileC++;
			} catch (SQLException e) {
				// TODO:
				e.printStackTrace();
			}
		}
	}

	private int calculateProgressbarStepSize(Project project) {
		int result = (project.getProjectFilePaths().size() + 1) / 10;
		if (project.getProjectFilePaths().size() < 10) {
			result = 1;
		}
		return result;
	}

}
