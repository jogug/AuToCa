/*
** Copyright 2013 Software Composition Group, University of Bern. All rights reserved.
*/
package ch.unibe.scg.autoca.configuration;

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

import ch.unibe.scg.autoca.datastructure.Dataset;
import ch.unibe.scg.autoca.datastructure.Language;
import ch.unibe.scg.autoca.executionmode.AnalyzeMode;
import ch.unibe.scg.autoca.executionmode.TokenizeMode;
import ch.unibe.scg.autoca.filter.*;
import ch.unibe.scg.autoca.utilities.SourceExtractor;
import joptsimple.OptionParser;
import joptsimple.OptionSet;

/**
 * Parses the execution settings from the cmd arguments
 * @author Joel
 *
 */
public final class Configuration {
    private static final Logger logger = LoggerFactory.getLogger(Configuration.class);	
	
    private final String defaultConfig = "resources/configuration/default.cfg";
    
	private enum OperationMode {TOKENIZE, ANALYZE, BOTH, EXTRACT};
	private enum ConfigurationMode {DEF, PATH};
	private String path;
	
	public ConfigurationMode cfMode;
	public OperationMode opMode;

    public void parseArguments(String[] args) throws IOException, ClassNotFoundException, SQLException {
        Objects.requireNonNull(args);
        
        OptionParser parser = new OptionParser();
        // Parse arguments
        OptionSet options = parser.parse(args);
        List<String> nonOptionArgs = options.nonOptionArguments();
        
        // Get the configuration mode
        if (nonOptionArgs.get(1).equalsIgnoreCase("default")) {
            cfMode = ConfigurationMode.DEF;
         } else if (nonOptionArgs.get(1).equalsIgnoreCase("path")) {
            cfMode = ConfigurationMode.PATH;
            path = nonOptionArgs.get(2).toString();
         }else {
             throw new OptionException("Unknown configuration mode: " + nonOptionArgs.get(1));
         }
        
        // Get the operation mode
        if (nonOptionArgs.get(0).equalsIgnoreCase("scan")) {
        	opMode = OperationMode.TOKENIZE;
        } else if (nonOptionArgs.get(0).equalsIgnoreCase("analyze")) {
            opMode = OperationMode.ANALYZE;
        } else if (nonOptionArgs.get(0).equalsIgnoreCase("both")) {
            opMode = OperationMode.BOTH;
        } else if (nonOptionArgs.get(0).equalsIgnoreCase("extract")) {
            opMode = OperationMode.EXTRACT;
        }else {
            throw new OptionException("Unknown operation mode: " + nonOptionArgs.get(0));
        }
        
        execute();
    }
    
    private void execute(){
    	JSONObject jsonObject;
    	Dataset ds;
    	TokenizeMode tokenizeMode;
    	AnalyzeMode analyzeMode;
    	
    	//Load config file from default path or arg
		switch(cfMode){
		case DEF:
			jsonObject = loadJSON(defaultConfig);
	     	break;
	    case PATH:
	    	jsonObject = loadJSON(path);
	    	 break;   
	    default: 
			jsonObject = loadJSON(defaultConfig);
		}
		
		//Switch between different operations modes
        switch(opMode){
    	case TOKENIZE:   		
    		ds = new Dataset(jsonObject, processLanguages(jsonObject), null);	        		
    		tokenizeMode = new TokenizeMode(ds);
    		tokenizeMode.execute();
    		break;
    	case ANALYZE:  
    		ds = new Dataset(jsonObject, processLanguages(jsonObject), processFilterChain(jsonObject));	        		
    		analyzeMode = new AnalyzeMode(ds);
    		analyzeMode.execute();
    		break;
    	case BOTH:
    		ds = new Dataset(jsonObject, processLanguages(jsonObject), processFilterChain(jsonObject));
    		tokenizeMode = new TokenizeMode(ds);
    		tokenizeMode.execute();
    		analyzeMode = new AnalyzeMode(ds);
    		analyzeMode.execute();
    		break;
    	case EXTRACT:
    		ds = new Dataset(jsonObject, processLanguages(jsonObject), processFilterChain(jsonObject));
    		SourceExtractor se = new SourceExtractor();
    		se.extractSourceFiles(ds);
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
		
		JSONArray plainLang = plainData.getJSONArray("language");
		for(int i=0; i<plainLang.length();i++){
			Language language = new Language(plainLang.getJSONObject(i).getString("name"),
											plainLang.getJSONObject(i).getString("filePattern"), 
											Paths.get(plainLang.getJSONObject(i).getString("tokenPath")),
											plainLang.getJSONObject(i).getLong("projectSizeLimit"),
											plainLang.getJSONObject(i).getInt("minAmountOfProjects"));
			language.addMultipleProjects(Paths.get(plainLang.getJSONObject(i).getString("projectsPath")));												
			languages.add(language);
		}
		return languages;
	}
	
	private List<FilterChain> processFilterChain(JSONObject plainData){
		if (!plainData.has("filterchains")) return null;
		List<FilterChain> filterChains = new ArrayList<>();
		
		JSONArray plainChains = plainData.getJSONArray("filterchains");
		for(int i=0; i<plainChains.length();i++){
			String resultName = plainChains.getJSONObject(i).getString("resultName");
			
			JSONArray plainLangs = plainChains.getJSONObject(i).getJSONArray("languages");
			List<String> languageNames = new ArrayList<>();
			for(int j=0; j<plainLangs.length();j++){
				languageNames.add(plainLangs.getJSONObject(j).getString("name"));
			}
			
			JSONArray plainFilters = plainChains.getJSONObject(i).getJSONArray("filters");
			AbstractFilter start = processFilters(plainFilters, plainData.getJSONObject("database").getString("PREFIXSTAT"));
			
			filterChains.add(new FilterChain(resultName, languageNames, start));
		}
		return filterChains;
	}
	
	private AbstractFilter processFilters(JSONArray plainFilters, String PREFIXSTAT){
		List<AbstractFilter> active = new ArrayList<>();
		for(int i=0; i<plainFilters.length();i++){
			switch(plainFilters.getJSONObject(i).getString("name")){
				case "Output":			boolean out = plainFilters.getJSONObject(i).getBoolean("save");
										active.add(new OutputFilter(out, PREFIXSTAT));
										break;
				case "UpCaseFilter": 	active.add(new UpCaseFilter());
										break;
				case "IntersectFilter": active.add(new IntersectFilter(plainFilters.getJSONObject(i).getInt("minOccInProject")));
										break;
				case "GlobalFilter": 	active.add(new GlobalMethodFilter());
										break;
				case "CoverageFilter":	active.add(new CoverageMethodFilter());
										break;
				case "IndentFilter": 	active.add(new IndentSMethodFilter());
										break;
				case "RealIndentFilter":active.add(new IndentMethodFilter());
										break;						
				case "NewlineFilter":	active.add(new NewlineFilter());
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
	
	public Dataset testDataSet(String path){
    	JSONObject plainData = loadJSON(path);
    	return new Dataset(plainData, processLanguages(plainData), processFilterChain(plainData));
	}	
}
