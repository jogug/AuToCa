/*
** Copyright 2013 Software Composition Group, University of Bern. All rights reserved.
*/
package ch.unibe.scg.lexica;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;

import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;

/**
 * Get the Configuration from the cmd
 * @author Joel
 *
 */
public final class Configuration {

    private static final Configuration instance = new Configuration();

    private IOperationMode mode = null;
    private String filePattern = null;
    private String[] commentPattern = null;
    private String[] ignorePattern = null;

    private Configuration() {
    }

    /**
     * Parse the Command Line arguments.
     *
     * <scan|analyze> [path] [-f <file pattern>] [-p <start pattern>,<end pattern>,<start...] [-i <ignore pattern>, ..]
     *
     * @param args the arguments
     * @throws IOException if an I/O error occurs
     * @throws SQLException 
     * @throws ClassNotFoundException 
     */
    public void parseArguments(String[] args) throws IOException, ClassNotFoundException, SQLException {
        Objects.requireNonNull(args);
        
        // Parse Options
        // minimizes search on code files, f
        OptionParser parser = new OptionParser();
        OptionSpec<String> filePatternArg = parser.accepts("f").withRequiredArg().defaultsTo("");
        OptionSpec<String> commentPatternArg = parser.accepts("p").withRequiredArg().defaultsTo("");       
        OptionSpec<String> ignorePatternArg = parser.accepts("i").withRequiredArg().defaultsTo("");
                
        // Parse the arguments
        OptionSet options = parser.parse(args);

        // Check the non-option arguments
        List<String> nonOptionArgs = options.nonOptionArguments();
        if (nonOptionArgs.size() < 1) {
            throw new OptionException("Please specify an operation mode");
        } else if (nonOptionArgs.size() > 2) {
            throw new OptionException("Unknown option: " + nonOptionArgs.get(2));
        }

        // Get the path
        Path path = null;
        if (nonOptionArgs.size() == 2) {
            path = Paths.get(nonOptionArgs.get(1)).toRealPath();
            if (!Files.exists(path)) {
                throw new OptionException("Directory does not exist: " + path.toString());
            } else if (!Files.isDirectory(path)) {
                throw new OptionException("Path is not a directory: " + path.toString());
            }
        } else {
            path = Paths.get(".").toRealPath();
            assert Files.exists(path);
            assert Files.isDirectory(path);
        }

        // Get the operation mode
        if (nonOptionArgs.get(0).equalsIgnoreCase("scan")) {
            mode = new ScanMode(path);
        } else if (nonOptionArgs.get(0).equalsIgnoreCase("analyze")) {
        	//TODO Analyze mode not integrated from console yet
            mode = new AnalyzeMode(path,path,50);
        } else {
            throw new OptionException("Unknown operation mode: " + nonOptionArgs.get(0));
        }

        // Get the file pattern
        filePattern = filePatternArg.value(options).replaceAll("[\\\"]", "");
               
        // Gets the start and end-patterns !Attention produces an ERROR if comments start with [ or ]
        // When new Line is passed as argument in the cmd 
        String temp = commentPatternArg.values(options).toString().replaceAll("\\[|\\]", "");
        commentPattern = temp.split(",");
        for(int i = 0; i<commentPattern.length;i++){
        	commentPattern[i] = commentPattern[i].replaceAll("\\\\n", "\n");
        }
        
        temp = ignorePatternArg.values(options).toString().replaceAll("\\[|\\]", "");
        ignorePattern = temp.split(",");
        for(int i = 0; i<ignorePattern.length;i++){
        	ignorePattern[i] = ignorePattern[i].replaceAll("\\\\n", "\n");
        }
    }

	public String[] getCommentPattern() {
		return commentPattern;
	}
	
	public String[] getIgnorePattern(){
		return ignorePattern;
	}

    public static Configuration getInstance() {
        return instance;
    }

    public IOperationMode getMode() {
        return mode;
    }

    public String getFilePattern() {
        return filePattern;
    }
}
