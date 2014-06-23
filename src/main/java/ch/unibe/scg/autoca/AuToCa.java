/*
** Copyright 2013 Software Composition Group, University of Bern. All rights reserved.
*/
package ch.unibe.scg.autoca;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.unibe.scg.autoca.mode.ScanMode;
public final class AuToCa {

    private static final Logger logger = LoggerFactory.getLogger(AuToCa.class);

    /** TODO JK: bad practice, to keep this in a constructor... */
    public AuToCa(String[] args) {
    	DataSet dataset = new DataSet();
    	dataset.loadStandardDataSet();;
    	
    	ScanMode scanmode = new ScanMode();
    	scanmode.execute(dataset);
    }

    public static void main(String[] args) {
		@SuppressWarnings("unused")
		AuToCa autoca = new AuToCa(args);
    }
}
