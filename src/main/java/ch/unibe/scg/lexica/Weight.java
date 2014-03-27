package ch.unibe.scg.lexica;

import java.util.HashSet;

/**
 * Saves the influence and name of a weight 
 * @author Joel
 *
 */

public class Weight {
	private String tableName, name;
	
	public Weight(String name, String tableName){
		this.name = name;
		this.tableName = tableName;
	}
	
	public String getName(){
		return name;
	}
	
	public String getTableName(){
		return tableName;
	}	
}
