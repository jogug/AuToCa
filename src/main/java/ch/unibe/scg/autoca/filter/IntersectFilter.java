package ch.unibe.scg.autoca.filter;

import ch.unibe.scg.autoca.db.DB;

public class IntersectFilter extends Filter{
	private String[] tables;

	public IntersectFilter(Filter next, String[] tables) {
		super(next);
		this.tables = tables;
	}

	@Override
	void execute(DB db) {
		// TODO Intersect tables in 
		
	}

}
