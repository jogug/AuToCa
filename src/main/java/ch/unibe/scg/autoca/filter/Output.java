package ch.unibe.scg.autoca.filter;

import java.sql.SQLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.unibe.scg.autoca.db.DB;

public class Output extends Filter {
	
    private static final Logger logger = LoggerFactory.getLogger(Output.class);	
	
	private boolean save;
    String langPreFix;
    String projPreFix;
    
	public Output(boolean save, String langPreFix, String projPreFix) {
		this.langPreFix = langPreFix;
		this.projPreFix = projPreFix;
		this.save = save;
	}
	
	//TODO ADD CSV SUPPORT// PER PROJECT PER LANGUAGE
	@Override
	void execute(DB db, String languageName, String resultTable) {
		if(!langPreFix.contains("Null")){
			try {
				logger.info("Outputting Stats " + resultTable);
				db.newFilterTable();
				db.dropTableIfExists(langPreFix + resultTable);
				db.calculateStatisticsPerLanguage(languageName,resultTable, langPreFix + resultTable);
				db.filterTableFinished();
			} catch (SQLException | ClassNotFoundException e) {
				e.printStackTrace();
			}
			
		}
		if(!projPreFix.contains("Null")){
			
		}
		if(save){
			try {
				logger.info("Outputting " + resultTable);
				db.newFilterTable();
				db.nameOrderToken(languageName, resultTable);
				db.filterTableFinished();
			} catch (SQLException | ClassNotFoundException e) {
				e.printStackTrace();
			}
		}else{
			try {
				logger.info("Dropping " + resultTable);
				db.newFilterTable();
				db.dropTableIfExists(resultTable);
				db.filterTableFinished();
			} catch (SQLException | ClassNotFoundException e) {
				e.printStackTrace();
			}
		}
	}
}