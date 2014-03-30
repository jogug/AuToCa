/*
** Copyright 2013 Software Composition Group, University of Bern. All rights reserved.
*/
package ch.unibe.scg.lexica;

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
