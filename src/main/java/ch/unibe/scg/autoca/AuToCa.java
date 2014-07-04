/*
 ** Copyright 2013 Software Composition Group, University of Bern. All rights reserved.
 */
package ch.unibe.scg.autoca;
import ch.unibe.scg.autoca.mode.AnalyzeMode;
import ch.unibe.scg.autoca.mode.ScanMode;

public final class AuToCa {

	private DataSet dataset;

	public AuToCa(String[] args) {
		dataset = new DataSet();
		dataset.loadStandardDataSet();
	}

	public void runScan() {
		ScanMode scanmode = new ScanMode(dataset);
		scanmode.execute();
	}
	
	public void runAnalyze(){
		AnalyzeMode analyzemode = new AnalyzeMode(dataset, true, true, false);
		analyzemode.execute();
	}

	public static void main(String[] args) {
		AuToCa autoca = new AuToCa(args);
		autoca.runScan();
		autoca.runAnalyze();

	}

}
