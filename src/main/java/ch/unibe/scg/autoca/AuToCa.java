/*
** Copyright 2013 Software Composition Group, University of Bern. All rights reserved.
*/
package ch.unibe.scg.autoca;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
public final class AuToCa {

    private static final Logger logger = LoggerFactory.getLogger(AuToCa.class);

    public AuToCa(String[] args) {
    	Test test = new Test();
    	test.loadStandardTest();
    	test.scan();
    }

    public static void main(String[] args) {
		@SuppressWarnings("unused")
		AuToCa autoca = new AuToCa(args);
    }
}
