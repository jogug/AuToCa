/*
** Copyright 2013 Software Composition Group, University of Bern. All rights reserved.
*/
package ch.unibe.scg.lexica;

import java.io.BufferedReader;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Objects;

public class FoLParser implements IParser {
	private Graph graph;

	public FoLParser(Graph graph){
		Objects.requireNonNull(graph);
		this.graph = graph;
	}
	
	/**
	 * Finds the first word of a line and saves it in the db
	 * @throws  
	 */
	public void parse(BufferedReader reader, ArrayList<Integer> cflags, String table, int minWL, int maxWL) throws IOException{ 
		int counter = 0,i = 0, j = 0;	
        String name = "";
        boolean notNewLine = true, whitespace = true;
	
        while ((i = reader.read()) != -1) {           	
            char c = (char) i;              
            counter++;
            //Skip Comment                
            if(j<cflags.size()&&counter == cflags.get(j)){            	
            	reader.skip(cflags.get(j+1)-cflags.get(j));
            	counter = cflags.get(j+1);
            	j+=2;
                add(name, table, minWL, maxWL);
                name = "";
            }else{
	            if(notNewLine){
	            	if(c=='\n'){
	            		notNewLine = false;
	            	}
	           	}else if(whitespace){
	           		if(!(Character.isWhitespace(c)||Character.toString(c).matches("[\\\"\\'\\`\\^\\|\\~\\\\\\&\\$\\%\\#\\@\\.\\,\\;\\:\\!\\?\\+\\-\\*\\/\\=\\<\\>\\(\\)\\{\\}\\[\\]]"))){
	           			whitespace = false;
		                // Add character to token
		                 name += c;	                    		                
	           		}
	           		           
	            }else{
	                if (Character.toString(c).matches("[ \\r\\n\\t]")) {
	                    // Ignore white spaces
	                    add(name,table, minWL, maxWL);
	                    notNewLine = true;
	                    whitespace = true;
	                    name = "";
	                } else if (Character.toString(c).matches("[\\\"\\'\\`\\^\\|\\~\\\\\\&\\$\\%\\#\\@\\.\\,\\;\\:\\!\\?\\+\\-\\*\\/\\=\\<\\>\\(\\)\\{\\}\\[\\]]")) {
	                    // Ignore delimiters
	                    add(name,table, minWL, maxWL);
	                    notNewLine = true;
	                    whitespace = true;
	                    name = "";
	                } else {
	                    // Add character to token
	                    name += c;	                    
	                }
	            } 
            }
        }
	}
    
    public void add(String name, String table, int minWL, int maxWL){
        if (!name.isEmpty()&&name.length()<maxWL&&name.length()>minWL) {
            try {
				graph.put(name, table);
			} catch (SQLException e) {
				e.printStackTrace();
			}
        }
    }
}
