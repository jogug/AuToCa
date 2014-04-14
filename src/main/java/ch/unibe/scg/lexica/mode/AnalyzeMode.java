/*
** Copyright 2013 Software Composition Group, University of Bern. All rights reserved.
*/
package ch.unibe.scg.lexica.mode;

import java.nio.file.Path;
import java.util.ArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ch.unibe.scg.lexica.Language;
import ch.unibe.scg.lexica.Weight;

/**
 * Analyzes the tokens extracted from the code according to the actual tokesn of a language
 * @author Joel
 *
 */
public final class AnalyzeMode implements IOperationMode {

    private static final Logger logger = LoggerFactory.getLogger(AnalyzeMode.class);



    public AnalyzeMode(Path path, ArrayList<Weight> weights, Language language){  	     	

    }
    
    public void loadStandardData() {

    }

    @Override
    public void execute() {

    }
}
