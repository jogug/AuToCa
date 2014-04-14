/*
 ** Copyright 2013 Software Composition Group, University of Bern. All rights reserved.
 */
package ch.unibe.scg.lexica;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ch.unibe.scg.lexica.tokenizer.Tokenizer;

/**
 * Created for each file to be visited, handles parsing to be done
 * @author Joel
 *
 */
public class SourceFileVisitor extends SimpleFileVisitor<Path> {

    private static final Logger logger = LoggerFactory.getLogger(SourceFileVisitor.class);

    private final DB db;
    private final PathMatcher pathMatcher;

    public SourceFileVisitor(DB db) throws SQLException, IOException {
        Objects.requireNonNull(db);
        this.db = db;     
        pathMatcher = FileSystems.getDefault().getPathMatcher("glob:" + Configuration.getInstance().getFilePattern());
    }

    @Override
    public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) throws IOException {
        Objects.requireNonNull(path);
        Objects.requireNonNull(attrs);

        Path name = path.getFileName();
        if (name != null && pathMatcher.matches(name)) {
            logger.debug("Parsing " + path.toString());

            //Empty Buffer
            try {
				db.deleteRecords("token_buffer");
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
            
            //Tokenize File
            TokenHandler th = new TokenHandler(db, name.toString());
            Tokenizer tk = new Tokenizer(th);
            tk.loadDefaults();
            tk.tokenize(path.toFile());
            th.mergeTokens();
            th.insertIDs();
        }
        return FileVisitResult.CONTINUE;
    }
}
