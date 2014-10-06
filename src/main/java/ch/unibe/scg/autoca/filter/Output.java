package ch.unibe.scg.autoca.filter;

import ch.unibe.scg.autoca.db.DB;

public class Output extends Filter {
	private String output;
	private boolean save;

	public Output(Filter next, boolean save, String output) {
		super(next);
		this.output = output;
		this.save = save;
	}

	@Override
	void execute(DB db) {
		if(save){
			// TODO Save result in database
		}else{
			
		}		
	}
}