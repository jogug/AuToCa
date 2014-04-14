package ch.unibe.scg.lexica;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.ArrayList;
import org.junit.Test;
import ch.unibe.scg.lexica.mode.AnalyzeMode;

public class AnalyzeTest {

    @Test
    public void test() throws ClassNotFoundException, SQLException {
    	//0 c,1 java, 2 cpp, 3 python
    	int i = 5;
    	
    	AnalyzeMode test = new AnalyzeMode(	loadPath(i)[0],
    										initStandardWeights(),
    										new Language("Python",loadPath(i)[1]));  	
    	test.execute();
    }
    
    public Path[] loadPath(int i){    	
    	if(i==1){
        	return new Path[]{	Paths.get("C:\\javaapachetest\\"),
        						Paths.get("../lexica/resources/java_tokens.txt")};   		
    	}else if(i==2){
           	return new Path[]{	Paths.get("C:\\filezilla3.7.4.1\\"),
           						Paths.get("../lexica/resources/cpp_tokens.txt")};   		
    	}else if(i==3){
           	return new Path[]{	Paths.get("C:\\numpy1.8.0\\"),
								Paths.get("../lexica/resources/python_tokens.txt")};   		
    	}else if(i==4){
           	return new Path[]{	Paths.get("C:\\wireshark1.10.6\\"),
           						Paths.get("../lexica/resources/c_tokens.txt")};   		
    	}else if(i==5){
        	return new Path[]{	Paths.get("C:\\box2d\\"),
								Paths.get("../lexica/resources/java_tokens.txt")};   
    	}else{
        	return new Path[]{	Paths.get("../lexica/resources/TestClasses"),
								Paths.get("../lexica/resources/java_tokens.txt")};   
    	}
    }
    
    private ArrayList<Weight> initStandardWeights(){
    	ArrayList<Weight> result = new ArrayList<>();
    	return result;
    }
}