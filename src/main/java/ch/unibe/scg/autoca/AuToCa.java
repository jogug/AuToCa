/*
 ** Copyright 2013 Software Composition Group, University of Bern. All rights reserved.
 */
package ch.unibe.scg.autoca;
import java.io.IOException;
import java.sql.SQLException;

import ch.unibe.scg.autoca.configuration.Configuration;
import ch.unibe.scg.autoca.utilities.AuToCaUtils;

public class AuToCa {

	private void execute(String[] args){
		Configuration config = new Configuration();
		try {
			config.parseArguments(args);
		} catch (ClassNotFoundException | IOException | SQLException e) {
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args) {	
		//		String[] x = {"both", "path", "resources/configuration/thesisUnfit.cfg"};
//		AuToCa autoca = new AuToCa();
//		autoca.execute(x);
	}
}
