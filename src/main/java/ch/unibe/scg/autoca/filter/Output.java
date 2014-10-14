package ch.unibe.scg.autoca.filter;

import java.sql.SQLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.unibe.scg.autoca.db.DB;

public class Output extends Filter {
	
    private static final Logger logger = LoggerFactory.getLogger(Output.class);	
	
	private boolean save;

	public Output(Filter next, boolean save) {
		this.save = save;
	}

	@Override
	void execute(DB db, String languageName, String resultTable) {
		if(save){
			try {
				logger.info("Outputting " + resultTable);
				db.newFilterTable();
				db.nameOrderToken(languageName, resultTable);
			} catch (SQLException | ClassNotFoundException e) {
				e.printStackTrace();
			}
		}else{
			try {
				logger.info("Dropping " + resultTable);
				db.newFilterTable();
				db.dropTableIfExists(resultTable);
			} catch (SQLException | ClassNotFoundException e) {
				e.printStackTrace();
			}
		}
	}
}