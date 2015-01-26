package ch.unibe.scg.autoca.filter;

import java.sql.SQLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.unibe.scg.autoca.db.DB;

public class SubStringFilter extends AbstractFilter {
	private String subString;
    private static final Logger logger = LoggerFactory.getLogger(SubStringFilter.class);	
	
	public SubStringFilter(String subString) {
		this.subString = subString;
	}

	@Override
	void execute(DB db, String languageName, String resultTable) {
		logger.info("SubString Filter");
		try {
			db.newFilterTable();
			db.specialSubRemoval(languageName, resultTable, subString);
			db.filterTableFinished();
		} catch (SQLException | ClassNotFoundException e) {
			e.printStackTrace();
		}
		getNext().execute(db, languageName, resultTable);
	}

}
