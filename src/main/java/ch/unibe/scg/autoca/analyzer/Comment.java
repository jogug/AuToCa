/*
** Copyright 2013 Software Composition Group, University of Bern. All rights reserved.
*/
package ch.unibe.scg.autoca.analyzer;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Sets skip flags for comments, saves them in cflags
 */
public class Comment {
	private  String[] commentPat;
	private String[] ignorePat;
	
	public Comment(String[] commentPat,String[] ignorePat){
     	this.commentPat = commentPat;
     	this.ignorePat = ignorePat;
	}

    final public ArrayList<Integer> parse(BufferedReader reader) throws IOException{ 
     	ArrayList<Integer> cflags = new ArrayList<Integer>();
     	
        int i = 0, counter = 0, z = 0;
        //Find Start Pattern => true or find END Pattern => false
        boolean findS = true,findP = false;      

        while ((i = reader.read()) != -1) {
        	counter++;
            char c = (char) i;  
            
            //ignore patterns
           if(ignorePat.length>=1){
	            for(int j=0;j<ignorePat.length;j++){      
	            	reader.mark(ignorePat[j].length());
	            	char resetC = c;
	            	for(int k=0;k<ignorePat[j].length();k++)                      			
	            		if(c == ignorePat[j].charAt(k)){
	            			//Matching the rest of the characters
	                		if(k == ignorePat[j].length()-1){
	                			findP = true;
	                			//FOUND ignore pattern
	                			i = reader.read();
	                			c = (char) i;
	                			counter++;
	                			//Step out of loop
	                			k = ignorePat[j].length()+1;
	                		}else{
	                			i = reader.read();
	                			c = (char) i;
	                			counter++;
	                		}
	            		}else{
	            			//Missmatch jump out of inner loop and reset already controlled chars
	            			if(k>0&&j<ignorePat.length){
	            				counter = counter - k;
	            				reader.reset();
	                			c = resetC;
	            			}
	            			k = ignorePat[j].length()+1;                			
	            		}
	            	if(findP) j = ignorePat.length; findP = false;
	            } 	
           }
        
            
            //ignore comments
           if(commentPat.length>=1){
	            if(findS){
	                //Match chars with chars of starting pattern
	                for(int j=0;j<commentPat.length;j+=2){      
	                	reader.mark(commentPat[j].length());
	                	for(int k=0;k<commentPat[j].length();k++)                      			
	                		if(c == commentPat[j].charAt(k)){
	                			//Matching the rest of the characters
	                    		if(k == commentPat[j].length()-1){
	                    			//Match -> set flag
	                    			cflags.add(counter-(commentPat[j].length()-1));
	                    			z = j+1;
	                    			findS = false;
	                    			//Step out of loop
	                    			k = commentPat[j].length()+1;
	                    		}else{
	                    			i = reader.read();
	                    			c = (char) i;
	                    			counter++;
	                    		}
	                		}else{
	                			//Missmatch jump out of inner loop and reset already controlled chars
	                			if(k>0&&j<commentPat.length){
	                				counter = counter - k;
	                				reader.reset();
	                			}
	                			k = commentPat[j].length()+1;                			
	                		}
	                	if(!findS) j = commentPat.length;
	                } 
	            }else{
	            	//Match chars with chars of end pattern
	            	assert(z!=0);
	            	reader.mark(commentPat[z].length());
	            	for(int k=0;k<commentPat[z].length();k++){
	            		if(c == commentPat[z].charAt(k)){        
	                		if(k == commentPat[z].length()-1){
	                			//Match -> set flag
	                			cflags.add(counter);
	                			findS = true;
	                			z = 0;
	                			//Step out of loop
	                			k = commentPat[z].length()+1;
	                		}else{
		            			//Matching the rest of the characters
		            			i = reader.read();
		            			c = (char) i;
		            			counter++;
	                		}
	            		}else{
	            			//Missmatch jump out of inner loop and reset already controlled chars
	            			if(k>0){ 
	            				counter = counter - k;
	            				reader.reset();
	            			}
	            			k = commentPat[z].length()+1;                			
	            		}
	            	}
	            }                    
	        }
        }
        //logger.debug("COMMENTS:" + cflags.toString());
        return cflags;
    }
}
