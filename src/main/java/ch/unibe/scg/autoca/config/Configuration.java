/*
** Copyright 2013 Software Composition Group, University of Bern. All rights reserved.
*/
package ch.unibe.scg.autoca.config;

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

import ch.unibe.scg.autoca.filter.*;
import ch.unibe.scg.autoca.mode.AnalyzeMode;
import ch.unibe.scg.autoca.mode.ScanMode;
import ch.unibe.scg.autoca.srcUtilities.SourceExtractor;
import ch.unibe.scg.autoca.structure.Language;
import joptsimple.OptionParser;
import joptsimple.OptionSet;

/**
 * Gets the execution settings from the cmd arguments
 * @author Joel
 *
 */
public final class Configuration {
    private static final Logger logger = LoggerFactory.getLogger(Configuration.class);	
	
    private final String defaultConfig = "resources/configuration/default.cfg";
    
	private enum OperationMode {SCAN, ANALYZE, BOTH, EXTRACT};
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
        	opMode = OperationMode.SCAN;
        } else if (nonOptionArgs.get(0).equalsIgnoreCase("analyze")) {
            opMode = OperationMode.ANALYZE;
        } else if (nonOptionArgs.get(0).equalsIgnoreCase("both")) {
            opMode = OperationMode.BOTH;
        } else if (nonOptionArgs.get(0).equalsIgnoreCase("prep")) {
            opMode = OperationMode.EXTRACT;
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
			plainData = loadJSON(defaultConfig);
	     	break;
	    case PATH:
	    	plainData = loadJSON(path);
	    	 break;   
	    default: 
			plainData = loadJSON(defaultConfig);
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
    		scanMode = new ScanMode(dataset);
    		scanMode.execute();
    		analyzeMode = new AnalyzeMode(dataset);
    		analyzeMode.execute();
    		break;
    	case EXTRACT:
    		dataset = new JSONInterface(plainData, processLanguages(plainData), processFilterChain(plainData));
    		SourceExtractor se = new SourceExtractor();
    		se.extractSourceFiles(dataset);
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
		if (!plainData.has("FilterChains")) return null;
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
			AbstractFilter start = processFilters(plainFilters);
			
			filterChains.add(new FilterChain(resultName, languageNames, start));
		}
		return filterChains;
	}
	
	private AbstractFilter processFilters(JSONArray plainFilters){
		List<AbstractFilter> active = new ArrayList<>();
		for(int i=0; i<plainFilters.length();i++){
			switch(plainFilters.getJSONObject(i).getString("name")){
				case "Output":			active.add(new Output(true, 
													plainFilters.getJSONObject(i).getString("langPreFix"),
													plainFilters.getJSONObject(i).getString("projPreFix")));
										break;
				case "UpCaseFilter": 	active.add(new UpCaseFilter());
										break;
				case "IntersectFilter": active.add(new IntersectFilter());
										break;
				case "GlobalFilter": 	active.add(new GlobalFilter());
										break;
				case "CoverageFilter":	active.add(new CoverageFilter());
										break;
				case "IndentFilter": 	active.add(new IndentFilter());
										break;
				case "RealIndentFilter":active.add(new RealIndentFilter());
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
	
	public JSONInterface testDataSet(String path){
    	JSONObject plainData = loadJSON(path);
    	return new JSONInterface(plainData, processLanguages(plainData), processFilterChain(plainData));
	}
	
	
}
