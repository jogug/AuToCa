package ch.unibe.scg.autoca.filter;

import java.sql.SQLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.unibe.scg.autoca.database.Database;

public class IntersectFilter extends AbstractFilter{
    private static final Logger logger = LoggerFactory.getLogger(IntersectFilter.class);	
    private int minOccInProject;
    
    public IntersectFilter(int minOccInProject) {
		this.minOccInProject = minOccInProject;
	}

	@Override
	void execute(Database db, String languageName, String resultTable) {
		logger.info("Intersect filter");
		try {
			db.newFilterTable();
			db.intersectProjectsOfLanguage(languageName, resultTable, minOccInProject);
			db.filterTableFinished();
		} catch (SQLException | ClassNotFoundException e) {
			e.printStackTrace();
		}	
		getNext().execute(db, languageName, resultTable);
	}
}
