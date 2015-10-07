package ch.unibe.scg.autoca.filter;

import java.sql.SQLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.unibe.scg.autoca.database.Database;

public class CoverageMethodFilter extends AbstractFilter {

    private static final Logger logger = LoggerFactory.getLogger(CoverageMethodFilter.class);	
	
	@Override
	void execute(Database db, String languageName, String resultTable) {
		logger.info("Coverage method");
		try {
			db.newFilterTable();
			db.dropTableIfExists(resultTable);
			db.coverageMethod(languageName, resultTable);
			db.filterTableFinished();
		} catch (SQLException | ClassNotFoundException e) {
			e.printStackTrace();
		}
		getNext().execute(db, languageName, resultTable);
	}

}
