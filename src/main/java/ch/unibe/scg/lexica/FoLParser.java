package ch.unibe.scg.lexica;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Objects;

public class FoLParser implements IParser {
	private Graph graph;

	public FoLParser(Graph graph){
		Objects.requireNonNull(graph);
		this.graph = graph;
	}
	
	public void parse(BufferedReader reader, ArrayList<Integer> cflags, String table) throws IOException{ 
		int counter = 0,i = 0, j = 0;	
         String name = "";
	
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
                if (Character.toString(c).matches("[ \\r\\n\\t]")){
                	//TODO
                }
            }
        }
	}
    
    public void add(String name, String table){
    	//TODO
    }
}
