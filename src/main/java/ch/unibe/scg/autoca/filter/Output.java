package ch.unibe.scg.autoca.filter;

import java.sql.SQLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.unibe.scg.autoca.db.DB;

public class Output extends AbstractFilter {
	
    private static final Logger logger = LoggerFactory.getLogger(Output.class);	
	
	private boolean save;
    private String PREFIXSTAT;
    
	public Output(boolean save, String PREFIXSTAT) {
		this.PREFIXSTAT = PREFIXSTAT;
		this.save = save;
	}
	
	@Override
	void execute(DB db, String languageName, String resultTable) {
		try {
			logger.info("Outputting Stats " + resultTable);
			db.newFilterTable();
			db.dropTableIfExists(PREFIXSTAT + resultTable);
			db.calculateStatisticsPerLanguage(languageName, resultTable, PREFIXSTAT + resultTable);
			db.filterTableFinished();
		} catch (SQLException | ClassNotFoundException e) {
			e.printStackTrace();
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