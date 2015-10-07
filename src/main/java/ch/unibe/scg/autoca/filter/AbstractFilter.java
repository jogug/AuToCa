package ch.unibe.scg.autoca.filter;

import ch.unibe.scg.autoca.database.Database;

/**
 * Filter, register in J
 * @author Joel
 *
 */
public abstract class AbstractFilter {
	private AbstractFilter next;
	
	public void setNext(AbstractFilter next){
		this.next = next;
	}
	
	public AbstractFilter getNext(){
		return next;
	}
	
	abstract void execute(Database db, String languageName, String resultTable);
}
