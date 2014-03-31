/*
 ** Copyright 2013 Software Composition Group, University of Bern. All rights reserved.
 */

package ch.unibe.scg.lexica;

import java.nio.file.Path;
import java.util.ArrayList;

import ch.unibe.scg.lexica.mode.AnalyzeMode;
import ch.unibe.scg.lexica.mode.ScanMode;

/**
 * Holds information on a test done on one code project to be scanned
 * @author Joel
 *
 */
public class Test {
	private Language language;
	private Path projectPath;
	private ArrayList<Weight> weights;
	
	public Test(Language language, Path projectPath, Path tokenPath, ArrayList<Weight> weights){
		this.language = language;
		this.projectPath = projectPath;
		this.weights = weights;
	}
	
	public void scan(){
		ScanMode scanMode = new ScanMode(projectPath, weights);
		scanMode.execute();		
	}
	
	public void analyze(){
		AnalyzeMode analyzeMode = new AnalyzeMode(projectPath, weights, language);
		analyzeMode.execute();		
	}
}
