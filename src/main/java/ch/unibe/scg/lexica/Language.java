/*
** Copyright 2013 Software Composition Group, University of Bern. All rights reserved.
*/
package ch.unibe.scg.lexica;

import java.nio.file.Path;

/**
 * Holds name and the location of the actual Tokens of a Language
 * @author Joel
 *
 */
public class Language {
	private String name;
	private Path tokenPath;
	
	public Language(String name, Path tokenPath){
		this.name = name;
		this.tokenPath = tokenPath;
	}
	
	public String getName(){
		return name;
	}
	
	public Path getTokenPath(){
		return tokenPath;
	}
}
