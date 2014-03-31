/*
** Copyright 2013 Software Composition Group, University of Bern. All rights reserved.
*/
package ch.unibe.scg.lexica.parser;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;

import ch.unibe.scg.lexica.Graph;

public interface IParser {
	public void parse(Graph graph, BufferedReader reader,ArrayList<Integer> cflags, String table, int minWL, int maxWL)throws IOException;
}
