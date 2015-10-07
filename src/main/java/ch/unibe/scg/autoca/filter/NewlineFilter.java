package ch.unibe.scg.autoca.filter;

import java.sql.SQLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.unibe.scg.autoca.database.Database;

public class NewlineFilter extends AbstractFilter {

    private static final Logger logger = LoggerFactory.getLogger(NewlineFilter.class);	
	
	@Override
	void execute(Database db, String languageName, String resultTable) {
		logger.info("Newline Method");
		try {
			db.newFilterTable();
			db.dropTableIfExists(resultTable);
			db.newlineMethodSaveRam(languageName, resultTable);
			db.filterTableFinished();
		} catch (SQLException | ClassNotFoundException e) {
			e.printStackTrace();
		}
		getNext().execute(db, languageName, resultTable);
	}

}
