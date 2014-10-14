package ch.unibe.scg.autoca.filter;

import java.sql.SQLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.unibe.scg.autoca.db.DB;

public class CoverageFilter extends Filter {

    private static final Logger logger = LoggerFactory.getLogger(CoverageFilter.class);	
	
	@Override
	void execute(DB db, String languageName, String resultTable) {
		logger.info("Coverage method");
		try {
			db.newFilterTable();
			db.coverageKeywordMethod(languageName, resultTable);
			db.filterTableFinished();
		} catch (SQLException | ClassNotFoundException e) {
			e.printStackTrace();
		}
		getNext().execute(db, languageName, resultTable);
	}

}
