/*
 ** Copyright 2013 Software Composition Group, University of Bern. All rights reserved.
 */
package ch.unibe.scg.autoca;
import java.io.IOException;
import java.sql.SQLException;

import ch.unibe.scg.autoca.config.Configuration;

public final class AuToCa {

	public AuToCa(String[] args) {
		Configuration config = new Configuration();
			try {
				config.parseArguments(args);
			} catch (ClassNotFoundException | IOException | SQLException e) {
				e.printStackTrace();
			}
	}

	public static void main(String[] args) {
		//String[] x = {"prep", "path", "resources/prep.cfg"};
		//String[] x = {"both", "path", "resources/config/testjava.cfg"};
		//String[] x = {"both", "path", "resources/configuration/cpp.cfg"};
		//String[] x = {"both", "path", "resources/configuration/python.cfg"};
		//String[] x = {"both", "path", "resources/configuration/c.cfg"};
		String[] x = {"analyze", "path", "resources/configuration/java.cfg"};
		AuToCa autoca = new AuToCa(x);
	}

}
