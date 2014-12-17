package ch.unibe.scg.autoca.filter;

import java.sql.SQLException;

import ch.unibe.scg.autoca.db.DB;

public class SubStringFilter extends AbstractFilter {
	private String subString;

	public SubStringFilter(String subString) {
		this.subString = subString;
	}

	@Override
	void execute(DB db, String languageName, String resultTable) {
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
