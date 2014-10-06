/*
 ** Copyright 2013 Software Composition Group, University of Bern. All rights reserved.
 */
package ch.unibe.scg.autoca;
import java.io.IOException;
import java.sql.SQLException;

import ch.unibe.scg.autoca.mode.AnalyzeMode;
import ch.unibe.scg.autoca.mode.ScanMode;

public final class AuToCa {

	private DataSet dataset;

	public AuToCa(String[] args) {
		JsonInterface config = new JsonInterface();
		try {
			config.parseArguments(args);
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}


	public static void main(String[] args) {
		String[] x = {"scan", "default"};
		AuToCa autoca = new AuToCa(x);
		//autoca.runAnalyze();
	}

}
