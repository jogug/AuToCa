/*
 ** Copyright 2013 Software Composition Group, University of Bern. All rights reserved.
 */
package ch.unibe.scg.autoca;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.unibe.scg.autoca.mode.ScanMode;

public final class AuToCa {

	private static final Logger logger = LoggerFactory.getLogger(AuToCa.class);

	private DataSet dataset;

	public AuToCa(String[] args) {
		dataset = new DataSet();
	}

	public void run() {
		dataset.loadStandardDataSet();

		ScanMode scanmode = new ScanMode();
		scanmode.execute(dataset);
	}

	public static void main(String[] args) {
		AuToCa autoca = new AuToCa(args);
		autoca.run();

	}
}
