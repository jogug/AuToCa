package ch.unibe.scg.autoca.filter;

import java.sql.SQLException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.unibe.scg.autoca.db.DB;

public class FilterChain {
	
    private static final Logger logger = LoggerFactory.getLogger(FilterChain.class);	
	
	private String resultName;
	private Filter start;
	private List<String> languageNames;
	
	public FilterChain(String resultName, List<String> languageNames, Filter start){
		this.resultName = resultName;
		this.start = start;
		this.languageNames = languageNames;
	}
	
	public void execute(DB db){
		logger.info("FilterChain: " + resultName);
		for(String languageName: languageNames){
			logger.info("Start on " + languageName+resultName);
			try {
				db.dropTableIfExists(languageName+resultName);
			} catch (SQLException e) {
				e.printStackTrace();
			}
			start.execute(db, languageName, languageName+resultName);
			logger.info("Finish");
		}
	}
	
	public String getResultName(){
		return resultName;
	}
	
	public List<String> getLanguageNames(){
		return languageNames;
	}
	
}
