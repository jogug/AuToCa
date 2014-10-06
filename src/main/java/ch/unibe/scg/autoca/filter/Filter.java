package ch.unibe.scg.autoca.filter;

import ch.unibe.scg.autoca.db.DB;

public abstract class Filter {
	private Filter next;
	
	public Filter(Filter next){
		this.next = next;
	}
	
	abstract void execute(DB db);
}
