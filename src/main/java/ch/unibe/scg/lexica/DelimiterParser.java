/*
** Copyright 2013 Software Composition Group, University of Bern. All rights reserved.
*/
package ch.unibe.scg.lexica;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.UnmappableCharacterException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DelimiterParser implements IParser {

    private static final Logger logger = LoggerFactory.getLogger(DelimiterParser.class);
    private final Graph graph;

    public DelimiterParser(Graph graph) {
        Objects.requireNonNull(graph);
        this.graph = graph;
    }
    
    public void parse(BufferedReader reader,ArrayList<Integer> cflags, String table, int minWL, int maxWL) throws IOException {
      	int counter = 0;
        String prevName = null;
        String name = "";
                        	            
        int i = 0;
        int j = 0;
        try {
            while ((i = reader.read()) != -1) {           	
                char c = (char) i;              
                counter++;
                //Skip Comment                
                if(j<cflags.size()&&counter == cflags.get(j)){            	
                	reader.skip(cflags.get(j+1)-cflags.get(j));
                	counter = cflags.get(j+1);
                	j+=2;
                    prevName = add(name, prevName, table, minWL, maxWL);
                    name = "";
                }else{
	                if (Character.toString(c).matches("[ \\r\\n\\t]")) {
	                    // Ignore white spaces
	                    prevName = add(name, prevName, table, minWL, maxWL);
	                    name = "";
	                } else if (Character.toString(c).matches("[\\\"\\'\\`\\^\\|\\~\\\\\\&\\$\\%\\#\\@\\.\\,\\;\\:\\!\\?\\+\\-\\*\\/\\=\\<\\>\\(\\)\\{\\}\\[\\]]")) {
	                    // Ignore delimiters
	                    prevName = add(name, prevName, table, minWL, maxWL);
	                    name = "";
	                } else {
	                    // Add character to token
	                    name += c;
	                }
                }
            }

            prevName = add(name, prevName, table, minWL, maxWL);
        } catch (UnmappableCharacterException | SQLException e) {
            logger.warn("An error occured", e);
        }
    }

    private String add(String name,String prev, String table, int minWL, int maxWL) throws SQLException {
        if (!name.isEmpty()&&name.length()<maxWL&&name.length()>minWL) {
            graph.put(name, prev, table);
        }
        return prev;
    }

}
