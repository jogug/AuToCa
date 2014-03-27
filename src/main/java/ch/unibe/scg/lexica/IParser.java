package ch.unibe.scg.lexica;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;

public interface IParser {
	public void parse(BufferedReader reader,ArrayList<Integer> cflags, String table)throws IOException;
}
