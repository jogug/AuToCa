package ch.unibe.scg.autoca.filter;

import java.sql.SQLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.unibe.scg.autoca.database.Database;

public class GlobalMethodFilter extends AbstractFilter {

    private static final Logger logger = LoggerFactory.getLogger(GlobalMethodFilter.class);	
	
	@Override
	void execute(Database db, String languageName, String resultTable) {
		logger.info("Global Method");
		try {
			db.newFilterTable();
			db.dropTableIfExists(resultTable);
			db.globalMethod(languageName, resultTable);
			db.filterTableFinished();
		} catch (SQLException | ClassNotFoundException e) {
			e.printStackTrace();
		}
		getNext().execute(db, languageName, resultTable);
	}
}
