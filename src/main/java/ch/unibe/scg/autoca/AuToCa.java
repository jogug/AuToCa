/*
** Copyright 2013 Software Composition Group, University of Bern. All rights reserved.
*/
package ch.unibe.scg.autoca;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
public final class AuToCa {

    private static final Logger logger = LoggerFactory.getLogger(AuToCa.class);

    /** TODO JK: bad practice, to keep this in a constructor... */
    public AuToCa(String[] args) {
    	Test test = new Test();
    	test.scan();
    	/*
            try {
                Configuration.getInstance().parseArguments(args);
                IOperationMode mode = Configuration.getInstance().getMode();
                mode.execute();
            } catch (IOException | ClassNotFoundException | SQLException e) {
                logger.error("An error occured", e);
            }
        */
    }

    public static void main(String[] args) {
		@SuppressWarnings("unused")
		AuToCa autoca = new AuToCa(args);
    }
}
