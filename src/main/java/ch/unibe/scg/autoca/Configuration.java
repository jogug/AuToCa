/*
** Copyright 2013 Software Composition Group, University of Bern. All rights reserved.
*/
package ch.unibe.scg.autoca;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.unibe.scg.autoca.filter.CoverageFilter;
import ch.unibe.scg.autoca.filter.Filter;
import ch.unibe.scg.autoca.filter.FilterChain;
import ch.unibe.scg.autoca.filter.GlobalFilter;
import ch.unibe.scg.autoca.filter.IndentFilter;
import ch.unibe.scg.autoca.filter.IntersectFilter;
import ch.unibe.scg.autoca.filter.Output;
import ch.unibe.scg.autoca.filter.SubStringFilter;
import ch.unibe.scg.autoca.filter.UpCaseFilter;
import ch.unibe.scg.autoca.mode.AnalyzeMode;
import ch.unibe.scg.autoca.mode.ScanMode;
import joptsimple.OptionParser;
import joptsimple.OptionSet;

/**
 * Gets the execution settings from the cmd arguments
 * @author Joel
 *
 */
public final class Configuration {
    private static final Logger logger = LoggerFactory.getLogger(Configuration.class);	
	
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
        
        execute();
    }
    
    private void execute(){
    	JSONObject plainData;
    	JSONInterface dataset;
    	ScanMode scanMode;
    	AnalyzeMode analyzeMode;
    	
    	//Load config file from default path or arg
		switch(cfMode){
		case DEF:
			plainData = loadJSON("resources/default.cfg");
	     	break;
	    case PATH:
	    	//TODO PATH from arg
	    	plainData = loadJSON("resources/default.cfg");
	    	 break;   
	    default: 
			plainData = loadJSON("resources/default.cfg");
		}
		
		//Switch between different operations modes
        switch(opMode){
    	case SCAN:   		
    		dataset = new JSONInterface(plainData, processLanguages(plainData), null);	        		
    		scanMode = new ScanMode(dataset);
    		scanMode.execute();
    		break;
    	case ANALYZE:  
    		dataset = new JSONInterface(plainData, processLanguages(plainData), processFilterChain(plainData));	        		
    		analyzeMode = new AnalyzeMode(dataset);
    		analyzeMode.execute();
    		break;
    	case BOTH:
    		dataset = new JSONInterface(plainData, processLanguages(plainData), processFilterChain(plainData));
    		analyzeMode = new AnalyzeMode(dataset);
    		scanMode = new ScanMode(dataset);
    		scanMode.execute();
    		analyzeMode.execute();
    		break;
        }
    }

	private JSONObject loadJSON(String path) {
		logger.info("Loading configuration from JSON File");
	    InputStream is = null;;
		try {
			is = new FileInputStream(path);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	    String jsonTxt = convertStreamToString(is);
		logger.info("finished loading configuration"); 
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
	
	private List<FilterChain> processFilterChain(JSONObject plainData){
		List<FilterChain> filterChains = new ArrayList<>();
		
		JSONArray plainChains = plainData.getJSONArray("FilterChains");
		for(int i=0; i<plainChains.length();i++){
			String resultName = plainChains.getJSONObject(i).getString("resultName");
			
			JSONArray plainLangs = plainChains.getJSONObject(i).getJSONArray("languages");
			List<String> languageNames = new ArrayList<>();
			for(int j=0; j<plainLangs.length();j++){
				languageNames.add(plainLangs.getJSONObject(j).getString("name"));
			}
			
			JSONArray plainFilters = plainChains.getJSONObject(i).getJSONArray("filters");
			Filter start = processFilters(plainFilters);
			
			filterChains.add(new FilterChain(resultName, languageNames, start));
		}
		return filterChains;
	}
	
	private Filter processFilters(JSONArray plainFilters){
		List<Filter> active = new ArrayList<>();
		for(int i=0; i<plainFilters.length();i++){
			switch(plainFilters.getJSONObject(i).getString("name")){
				case "Output":			active.add(new Output(null, true));
										break;
				case "UpCaseFilter": 	active.add(new UpCaseFilter());
										break;
				case "IntersectFilter": active.add(new IntersectFilter(plainFilters.getJSONObject(i).getInt("#occInProj")));
										break;
				case "GlobalFilter": 	active.add(new GlobalFilter());
										break;
				case "CoverageFilter":	active.add(new CoverageFilter());
										break;
				case "IndentFilter": 	active.add(new IndentFilter());
										break;
				case "SubStringFilter": active.add(new SubStringFilter(plainFilters.getJSONObject(i).getString("subString")));
				break;
			}
		}
		for(int i = active.size()-1;i>0;i--){
			active.get(i).setNext(active.get(i-1));
		}
		return active.get(active.size()-1);
	}
    
	private String convertStreamToString(java.io.InputStream is) {
		    @SuppressWarnings("resource")
			java.util.Scanner s = new java.util.Scanner(is).useDelimiter("\\A");
		    return s.hasNext() ? s.next() : "";
	}
	
	public JSONInterface  testDataSet(){
    	JSONObject plainData = loadJSON("resources/default.cfg");
    	return new JSONInterface(plainData, processLanguages(plainData), null);
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
