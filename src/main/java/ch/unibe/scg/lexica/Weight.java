/*
** Copyright 2013 Software Composition Group, University of Bern. All rights reserved.
*/
package ch.unibe.scg.lexica;

import ch.unibe.scg.lexica.parser.IParser;

/**
 * Holds Information on parsing and table name
 * @author Joel
 *
 */

public class Weight {
	private String tableName, name;
	private IParser parser;
	
	public Weight(String name, String tableName, IParser parser){
		this.name = name;	//TODO will be used when Results extracted
		this.tableName = tableName;
		this.parser = parser;
	}
	
	public String getName(){
		return name;
	}
	
	public String getTableName(){
		return tableName;
	}	
	
	public IParser getParser(){
		return this.parser;
	}
}
