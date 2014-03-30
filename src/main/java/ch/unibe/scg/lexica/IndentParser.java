/*
** Copyright 2013 Software Composition Group, University of Bern. All rights reserved.
*/
package ch.unibe.scg.lexica;

import java.io.BufferedReader;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Objects;

public class IndentParser implements IParser {
	private Graph graph;

	public IndentParser(Graph graph){
		Objects.requireNonNull(graph);
		this.graph = graph;
	}
	
	/**
	 * 
	 */
	public void parse(BufferedReader reader, ArrayList<Integer> cflags, String table) throws IOException{ 
		int counter = 0,i = 0, j = 0;	
        String name = "";
        char prevC = 'i';
        boolean notNewLine = true, notIndent = true;
	
        while ((i = reader.read()) != -1) {           	
            char c = (char) i;              
            counter++;
            //Skip Comment                
            if(j<cflags.size()&&counter == cflags.get(j)){            	
            	reader.skip(cflags.get(j+1)-cflags.get(j));
            	counter = cflags.get(j+1);
            	j+=2;
                add(name, table);
                name = "";
            }else{
	            if(notNewLine){
	            	if(c=='\n'){
	            		notNewLine = false;
	            	}
	           	}else if(notIndent){
	           		if(!(Character.isWhitespace(c)||Character.toString(c).matches("[\\\"\\'\\`\\^\\|\\~\\\\\\&\\$\\%\\#\\@\\.\\,\\;\\:\\!\\?\\+\\-\\*\\/\\=\\<\\>\\(\\)\\{\\}\\[\\]]"))){
	           		  if(counter>1&&Character.toString(prevC).matches("[ \\t]")){
	           			notIndent = false;
	                    // Add character to token
	                    name += c;	  
	           		  }else{
	           			  notNewLine = true;
	           		  }
	           		}
	           		           
	            }else{
	                if (Character.toString(c).matches("[ \\r\\n\\t]")) {
	                    // Ignore white spaces
	                	//TODO
	                    add(name,table);
	                    notNewLine = true;
	                    notIndent = true;
	                    name = "";
	                } else if (Character.toString(c).matches("[\\\"\\'\\`\\^\\|\\~\\\\\\&\\$\\%\\#\\@\\.\\,\\;\\:\\!\\?\\+\\-\\*\\/\\=\\<\\>\\(\\)\\{\\}\\[\\]]")) {
	                    // Ignore delimiters
	                    add(name,table);
	                    notNewLine = true;
	                    notIndent = true;
	                    name = "";
	                } else {
	                    // Add character to token
	                    name += c;	                    
	                }
	            } 
            }
            prevC = c;
        }
	}
    
    public void add(String name, String table){
        if (!name.isEmpty()) {
            try {
				graph.put(name, table);
			} catch (SQLException e) {
				e.printStackTrace();
			}
        }
    }
}


