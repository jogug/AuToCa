/*
** Copyright 2013 Software Composition Group, University of Bern. All rights reserved.
*/
package ch.unibe.scg.lexica.parser;

import java.io.BufferedReader;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import ch.unibe.scg.lexica.Graph;

/**
 * Finds words after indents  on new lines and saves them in the db
 * @author Joel
 *
 */
public class IndentParser implements IParser {

	public IndentParser(){
		//TODO incase delimiter signs may change
	}
	
	public void parse(Graph graph, BufferedReader reader, ArrayList<Integer> cflags, String table, int minWL, int maxWL) throws IOException{ 
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
                add(graph, name,table, minWL, maxWL);
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
	                    add(graph, name,table, minWL, maxWL);
	                    notNewLine = true;
	                    notIndent = true;
	                    name = "";
	                } else if (Character.toString(c).matches("[\\\"\\'\\`\\^\\|\\~\\\\\\&\\$\\%\\#\\@\\.\\,\\;\\:\\!\\?\\+\\-\\*\\/\\=\\<\\>\\(\\)\\{\\}\\[\\]]")) {
	                    // Ignore delimiters
	                    add(graph, name,table, minWL, maxWL);
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
    
    public void add(Graph graph, String name, String table, int minWL,int maxWL){
        if (!name.isEmpty()&&name.length()<maxWL&&name.length()>minWL) {
            try {
				graph.put(name, table);
			} catch (SQLException e) {
				e.printStackTrace();
			}
        }
    }
}


