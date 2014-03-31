/*
** Copyright 2013 Software Composition Group, University of Bern. All rights reserved.
*/
package ch.unibe.scg.lexica;

import org.junit.Test;


public class LexicaTest {
	

    @Test
    public void test() {   	
    	@SuppressWarnings("unused")
		Lexica lexica = new Lexica(loadStrings(4));
    }
    
    public String[] loadStrings(int i){
    	if (i == 1){
    		String[] stringArgs = {	"scan",
									"C:\\javaapachetest\\",
									"-f",
									"*.java",
									"-p",
									"/*,*/,//,\n",
									"-m",
									"1,11"
									}; 
    		return stringArgs;
    	}else if(i == 2){
    		String[] stringArgs =  { "scan",
    								//"C:\\inkscape0.48.4\\",
    								"C:\\filezilla3.7.4.1\\",
    								//"C:\\inkscape0.48.4\\src\\ui\\",
									//"C:\\win32diskmgr\\",
									"-f",
									"*.cpp",
									"-p",
									"/*,*/",
									"-m",
									"1,11"
									}; 
    		return stringArgs;
    	}else if(i == 3){
			String[] stringArgs = { "scan",
									"C:\\wireshark1.10.6\\",
									"-f",
									"*.c",
									"-p",
									"/*,*/,//,\n",
									"-m",
									"1,11"
									}; 
			return stringArgs;
    	}else if(i == 4){
			String[] stringArgs = { "scan",
									"C:\\numpy1.8.0\\",
									"-f",
									"*.py",
									"-p",
									"\"\"\",\n,#,\n",
									"-m",
									"1,11"
									}; 
			return stringArgs;
    	}else if(i == 5){
        	String[] stringArgs = {	"scan",
									"E:\\Benutzer\\Joel\\Bachelorarbeit\\lexica\\src\\test\\java\\ch\\unibe\\scg\\lexica\\TestClasses",
									"-f",
									"*.java",
									//"*.txt",
									"-p",
									"/*,*/,//,\n",
									"-m",
									"1,11"
									//"-i",
									//"://,\"/*,/*\""
									}; 
    		return stringArgs;
    	}else{
    		String[] stringArgs = {	"scan",
									"../lexica/resources/TestClasses",
									"-f",
									"*.java",
									"-p",
									"/*,*/,//,\n",
									"-m",
									"1,11"
									}; 
    		return stringArgs;
    	}
    }
}
