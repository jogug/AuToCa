package ch.unibe.scg.autoca.filter;

import java.sql.SQLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.unibe.scg.autoca.db.DB;

public class UpCaseFilter extends AbstractFilter {
	
    private static final Logger logger = LoggerFactory.getLogger(UpCaseFilter.class);	

	@Override
	void execute(DB db, String languageName, String resultTable) {
		logger.info("Upcase filter");
		try {
			db.newFilterTable();
			db.upperCaseRemoval(languageName, resultTable);
			db.filterTableFinished();
		} catch (SQLException | ClassNotFoundException e) {
			e.printStackTrace();
		}
		getNext().execute(db, languageName, resultTable);
	}
}