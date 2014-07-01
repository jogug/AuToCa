/*
 ** Copyright 2013 Software Composition Group, University of Bern. All rights reserved.
 */
package ch.unibe.scg.autoca;
import ch.unibe.scg.autoca.mode.ScanMode;

public final class AuToCa {

	private DataSet dataset;

	public AuToCa(String[] args) {
		dataset = new DataSet();
	}

	public void run() {
		dataset.loadStandardDataSet();

		ScanMode scanmode = new ScanMode(dataset);
	
		scanmode.execute();
	}

	public static void main(String[] args) {
		AuToCa autoca = new AuToCa(args);
		autoca.run();

	}

}
