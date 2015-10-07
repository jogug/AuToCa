package ch.unibe.scg.autoca.utilities;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;








import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.unibe.scg.autoca.datastructure.Dataset;
import ch.unibe.scg.autoca.datastructure.Language;
import ch.unibe.scg.autoca.datastructure.Project;
import ch.unibe.scg.autoca.executionmode.TokenizeMode;

public class SourceExtractor {
	private static final Logger logger = LoggerFactory.getLogger(TokenizeMode.class);

	private String outputExtractedSource;
	private static String DEFAULT_OUTPUT = "\\Extracted";
	
	/**
	 * Extracts only the source code files from a folder and its subfolders 
	 * into "\\Extracted"+sourceName directories. Files with the
	 * same name are copied in ascending numbered sub folders.
	 */
	
	public void extractSourceFiles(Dataset data) {
		logger.info("Starting extraction of source files from: " + data.getOutputLocation().toString());

		outputExtractedSource = createExtractFolder(data);

		if ((new File(outputExtractedSource)).exists()) {
			logger.info("Output folder for extracted source files already exists => skipped source file extraction,"
					+ "try deleting: " + outputExtractedSource);
			return;
		}

		new File(outputExtractedSource).mkdirs();

		for (Language language : data.getLanguages()) {
			processLanguage(language);
		}

		logger.info("Finished extraction to: " + outputExtractedSource);
	}
	
	/**
	 * Extracts the source code files for a whole language folder.
	 * @param language
	 */
	private void processLanguage(Language language) {
		String languagePath = outputExtractedSource + "\\" + language.getName().toString();
		createFileIfNotExists(languagePath);

		for (Project project : language.getProjects()) {
			String projectPath = languagePath + "\\" + project.getName().toString();
			createFileIfNotExists(projectPath);

			processProject(language, project, projectPath);
		}
	}

	/**
	 * Extracts the source code files only for one project.
	 * @param language
	 * @param project
	 * @param projectPath
	 */
	private void processProject(Language language, Project project, String projectPath) {
		for (Path path : project.getProjectFilePaths()) {
			int count = 0;
			String destination = projectPath + "\\" + count + "\\" + path.getFileName().toString();
			createFileIfNotExists(projectPath + "\\" + count);

			while ((new File(destination).exists())) {
				count++;
				destination = projectPath + "\\" + count + "\\" + path.getFileName().toString();
				createFileIfNotExists(projectPath + "\\" + count);
			}
			try {
				Files.copy(path, Paths.get(destination));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private static String createExtractFolder(Dataset data) {
		String outputExtractedSource = data.getOutputLocation().toString() + DEFAULT_OUTPUT
				+ data.getOutputLocation().getFileName().toString();
		return outputExtractedSource;
	}

	private static void createFileIfNotExists(String path) {
		if (!(new File(path).exists())) {
			(new File(path)).mkdirs();
		}
	}


}
