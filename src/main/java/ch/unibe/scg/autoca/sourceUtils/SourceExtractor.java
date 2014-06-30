package ch.unibe.scg.autoca.sourceUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.unibe.scg.autoca.DataSet;
import ch.unibe.scg.autoca.Language;
import ch.unibe.scg.autoca.Project;
import ch.unibe.scg.autoca.mode.ScanMode;

public class SourceExtractor {
	private static final Logger logger = LoggerFactory.getLogger(ScanMode.class);

	private String outputExtractedSource;

	
	public static void main(String[] args) {
		// TODO:
	}
	
	/**
	 * Extracts only the files affected by a scan from all the project
	 * directories into "\\Extracted"+sourceName directories. Files with the
	 * same name are copied in ascending numbered sub folders.
	 */
	public void extractSourceFiles(DataSet data) {
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

	private void processLanguage(Language language) {
		String languagePath = outputExtractedSource + "\\" + language.getName().toString();
		createFileIfNotExists(languagePath);

		for (Project project : language.getProjects()) {
			String projectPath = languagePath + "\\" + project.getName().toString();
			createFileIfNotExists(projectPath);

			processProject(language, project, projectPath);
		}
	}

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

	private static String createExtractFolder(DataSet data) {
		String outputExtractedSource = data.getOutputLocation().toString() + "\\Extracted"
				+ data.getOutputLocation().getFileName().toString();
		return outputExtractedSource;
	}

	private static void createFileIfNotExists(String path) {
		if (!(new File(path).exists())) {
			(new File(path)).mkdirs();
		}
	}


}
