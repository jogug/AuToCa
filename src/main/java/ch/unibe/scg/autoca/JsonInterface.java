/*
** Copyright 2013 Software Composition Group, University of Bern. All rights reserved.
*/
package ch.unibe.scg.autoca;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.json.JSONArray;
import org.json.JSONObject;

import ch.unibe.scg.autoca.db.DB;
import ch.unibe.scg.autoca.mode.AnalyzeMode;
import ch.unibe.scg.autoca.mode.IOperationMode;
import ch.unibe.scg.autoca.mode.ScanMode;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;

/**
 * Gets the execution settings from the cmd arguments
 * @author Joel
 *
 */
public final class JsonInterface {
	
	private enum OperationsMode {SCAN, ANALYZE, BOTH};
	private enum ConfigMode {DEF, PATH};
	
	public ConfigMode cfMode;
	public OperationsMode opMode;

    public void parseArguments(String[] args) throws IOException, ClassNotFoundException, SQLException {
        Objects.requireNonNull(args);
        
        OptionParser parser = new OptionParser();
        // Parse the arguments
        OptionSet options = parser.parse(args);
        List<String> nonOptionArgs = options.nonOptionArguments();
        
        // Get the configurations mode
        if (nonOptionArgs.get(1).equalsIgnoreCase("default")) {
            cfMode = ConfigMode.DEF;
         } else if (nonOptionArgs.get(1).equalsIgnoreCase("path")) {
            cfMode = ConfigMode.PATH;
         }else {
             throw new OptionException("Unknown configurations mode: " + nonOptionArgs.get(1));
         }
        
        // Get the operation mode
        if (nonOptionArgs.get(0).equalsIgnoreCase("scan")) {
        	opMode = OperationsMode.SCAN;
        } else if (nonOptionArgs.get(0).equalsIgnoreCase("analyze")) {
            opMode = OperationsMode.ANALYZE;
        }else {
            throw new OptionException("Unknown operation mode: " + nonOptionArgs.get(0));
        }
        
        //TODO load config file from path arg
		switch(cfMode){
	    case PATH:
	    	
	    	 break;        	     
		}
        execute();
    }
    
    private void execute(){
    	JSONObject plainData = loadConfiguration("resources/default.cfg");
    	
		switch(cfMode){
		case DEF:
	    	List<Language> languages = processLanguages(plainData);
	    	DataSet dataset = new DataSet(plainData, languages);
		        switch(opMode){
	        	case SCAN:
	        		ScanMode scanMode = new ScanMode(dataset);
	        		scanMode.execute();
	        		break;
	        	case ANALYZE:  
	        		AnalyzeMode analyzeMode = new AnalyzeMode(dataset, true, true, true);
	        		break;
	        	case BOTH:
	        		
	        		break;
		        }
	     	break;
	    case PATH:
	    	 break;        	     
		}
    }

	private JSONObject loadConfiguration(String path) {
	    InputStream is = null;;
		try {
			is = new FileInputStream(path);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	    String jsonTxt = convertStreamToString(is);
	        
		return new JSONObject(jsonTxt);
	}
	
	private List<Language> processLanguages(JSONObject plainData){
		List<Language> languages = new ArrayList<>();
		
		JSONArray plainLang = plainData.getJSONArray("DefaultLanguages");
		for(int i=0; i<plainLang.length();i++){
			Language language = new Language(	plainLang.getJSONObject(i).getString("name"),
											plainLang.getJSONObject(i).getString("filePattern"), 
											Paths.get(plainLang.getJSONObject(i).getString("tokenPath")));
			language.addMultipleProjects(Paths.get(plainLang.getJSONObject(i).getString("projectsPath")));												
			languages.add(language);
		}
		return languages;
	}
    
	private String convertStreamToString(java.io.InputStream is) {
		    java.util.Scanner s = new java.util.Scanner(is).useDelimiter("\\A");
		    return s.hasNext() ? s.next() : "";
	}
	
	public DataSet  testDataSet(){
    	JSONObject plainData = loadConfiguration("resources/default.cfg");
    	List<Language> languages = processLanguages(plainData);
    	return new DataSet(plainData, languages);
	}
    
    /**
     * Parse the Command Line arguments.
     *
     * <scan|analyze> [path] [-f <constants mode>] [-p <start pattern>,<end pattern>,<start...] [-i <ignore pattern>, ..] [-m <int> min, max]
     *
     * @param args the arguments
     * @throws IOException if an I/O error occurs
     * @throws SQLException 
     * @throws ClassNotFoundException 
     */
    /**
    private static final Configuration instance = new Configuration();

    private IOperationMode mode = null;
    private String constants = null;
    private String[] commentPattern = null;
    private String[] ignorePattern = null;
    private int minWL,maxWL;
    
    public void parseArguments(String[] args) throws IOException, ClassNotFoundException, SQLException {
        Objects.requireNonNull(args);
        
        // Parse Options
        // load standards -s
        OptionParser parser = new OptionParser();
        OptionSpec<String> filePatternArg = parser.accepts("f").withRequiredArg().defaultsTo("");
        OptionSpec<String> commentPatternArg = parser.accepts("p").withRequiredArg().defaultsTo("");       
        OptionSpec<String> ignorePatternArg = parser.accepts("i").withRequiredArg().defaultsTo("");
        OptionSpec<Integer> minMaxFilterArg = parser.accepts("m").withRequiredArg().withValuesSeparatedBy(',').ofType(Integer.class).ofType(Integer.class).defaultsTo(new Integer[]{0,30});
                
        // Parse the arguments
        OptionSet options = parser.parse(args);

        // Check the non-option arguments
        List<String> nonOptionArgs = options.nonOptionArguments();
        if (nonOptionArgs.size() < 1) {
            throw new OptionException("Please specify an operation mode");
        } else if (nonOptionArgs.size() > 2) {
            throw new OptionException("Unknown option: " + nonOptionArgs.get(2));
        }

        // Get the path
        Path path = null;
        if (nonOptionArgs.size() == 2) {
            path = Paths.get(nonOptionArgs.get(1)).toRealPath();
            if (!Files.exists(path)) {
                throw new OptionException("Directory does not exist: " + path.toString());
            } else if (!Files.isDirectory(path)) {
                throw new OptionException("Path is not a directory: " + path.toString());
            }
        } else {
            path = Paths.get(".").toRealPath();
            assert Files.exists(path);
            assert Files.isDirectory(path);
        }
        
        //Get the constants mode
        constants = filePatternArg.value(options).replaceAll("[\\\"]", "");
        if(constants.equalsIgnoreCase("standard")){
        	
        }else{
            throw new OptionException("Please provide a config file for the constants");        
        }

        // Get the operation mode
        if (nonOptionArgs.get(0).equalsIgnoreCase("scan")) {
        	//TODO Standard weights list
           // mode = new ScanMode(path);
        } else if (nonOptionArgs.get(0).equalsIgnoreCase("analyze")) {
        	//TODO Analyze mode not integrated from console yet
            //mode = new AnalyzeMode(path,initStandardLanguage());
        }else {
            throw new OptionException("Unknown operation mode: " + nonOptionArgs.get(0));
        }

   
        // Gets the start and end-patterns !Attention produces an ERROR if comments start with [ or ]
        // When new Line is passed as argument in the cmd 
        commentPattern = commentPatternArg.values(options).toString().replaceAll("\\[|\\]", "").split(",");
        resolveNewLineProblem(commentPattern);
        
        ignorePattern = ignorePatternArg.values(options).toString().replaceAll("\\[|\\]", "").split(",");
        resolveNewLineProblem(ignorePattern);
        
        minWL = minMaxFilterArg.values(options).get(0);
        maxWL = minMaxFilterArg.values(options).get(1);
    }
    
    
    private Language initStandardLanguage(){
    	return new Language("Java",".java", Paths.get("../lexica/resources/java_tokens.txt"));
    }
    
    private void resolveNewLineProblem(String arg[]){
        for(int i = 0; i<arg.length;i++){
        	arg[i] = arg[i].replaceAll("\\\\n", "\n");
        }
    }

	public String[] getCommentPattern() {
		return commentPattern;
	}
	
	public String[] getIgnorePattern(){
		return ignorePattern;
	}

    public static Configuration getInstance() {
        return instance;
    }

    public IOperationMode getMode() {
        return mode;
    }

    public int getMin(){
    	return minWL;
    }
    
    public int getMax(){
    	return maxWL;
    }
    **/
}
