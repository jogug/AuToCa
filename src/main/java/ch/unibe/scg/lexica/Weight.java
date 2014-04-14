/*
** Copyright 2013 Software Composition Group, University of Bern. All rights reserved.
*/
package ch.unibe.scg.lexica;


/**
 * Holds Information on parsing and table name
 * @author Joel
 *
 */

public class Weight {
	private String tableName, name;

	
	public Weight(String name, String tableName){
		this.name = name;	//TODO will be used when Results extracted
		this.tableName = tableName;

	}
	
	public String getName(){
		return name;
	}
	
	public String getTableName(){
		return tableName;
	}	

}
