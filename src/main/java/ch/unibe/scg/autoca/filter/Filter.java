package ch.unibe.scg.autoca.filter;

import ch.unibe.scg.autoca.db.DB;

/**
 * Filter, register in J
 * @author Joel
 *
 */
public abstract class Filter {
	private Filter next;
	
	public void setNext(Filter next){
		this.next = next;
	}
	
	public Filter getNext(){
		return next;
	}
	
	abstract void execute(DB db, String languageName, String resultTable);
}
