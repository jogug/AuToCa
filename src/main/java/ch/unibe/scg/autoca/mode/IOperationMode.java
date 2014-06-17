/*
** Copyright 2013 Software Composition Group, University of Bern. All rights reserved.
*/
package ch.unibe.scg.autoca.mode;

import ch.unibe.scg.autoca.DB;

public interface IOperationMode {

    void execute(DB db);

}
