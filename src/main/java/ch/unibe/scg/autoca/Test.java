/*
 ** Copyright 2013 Software Composition Group, University of Bern. All rights reserved.
 */

package ch.unibe.scg.autoca;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.ArrayList;

import ch.unibe.scg.autoca.mode.ScanMode;

/**
 * Holds information on a test done on one code project to be scanned
 * @author Joel
 *
 */
public class Test {
	private ArrayList<Language> languages;
	private Path outputLocation;
	
	public Test(ArrayList<Language> language){
		this.languages = language;
	}
	
	public Test(){
		this.languages = new ArrayList<Language>();
	}
	
	public void loadStandardTest() {		
		outputLocation = Paths.get("C:\\Users\\Joel\\Desktop\\Testprojekte\\");
		/*
		Language java = new Language("Java","*.java", Paths.get("../lexica/resources/java_tokens.txt"));
		java.addMultipleProjects(Paths.get("C:\\Users\\Joel\\Desktop\\Testprojekte\\Java\\"), java);
		languages.add(java);
		
		Language c = new Language("C","*.c", Paths.get("../lexica/resources/c_tokens.txt"));
		c.addMultipleProjects(Paths.get("C:\\Users\\Joel\\Desktop\\Testprojekte\\C\\"), c);
		languages.add(c);	
		*/
		Language python = new Language("Python", "*.py", Paths.get("../lexica/resources/python_tokens.txt"));
		python.addMultipleProjects(Paths.get("C:\\Users\\Joel\\Desktop\\Testprojekte\\Python\\"), python);
		languages.add(python);
		
		Language cpp = new Language("Cpp", "*.cpp", Paths.get("../lexica/resources/cpp_tokens.txt"));
		cpp.addMultipleProjects(Paths.get("C:\\Users\\Joel\\Desktop\\Testprojekte\\Cpp\\"), cpp);
		languages.add(cpp);			
		
	}
	
	public void loadTest(){
		outputLocation = Paths.get("C:\\Users\\Joel\\Desktop\\Testprojekte\\");
		
		Language java = new Language("Test","*.java", Paths.get("../lexica/resources/java_tokens.txt"));
		java.addMultipleProjects(Paths.get("C:\\Users\\Joel\\Desktop\\Testprojekte\\Test\\"), java);
		languages.add(java);
	}

	public void scan(){
		try {
			//Create DB
			DB db = new DB(outputLocation);
	        db.initialize();
	        
			//Fill DB
			if(!languages.isEmpty()){
				for(int j= 0;j<languages.size();j++){
					for(int i = 0;i<languages.get(j).getProjects().size();i++){
						ScanMode scanMode = new ScanMode(languages.get(j).getProjects().get(i));
						scanMode.execute(db);	
					}
				}
			}
		} catch (ClassNotFoundException | SQLException e) {
			e.printStackTrace();
		}  

		System.out.println("Finish");
	
	}
	
	public void analyze(){

	}
}
