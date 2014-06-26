/*
** Copyright 2013 Software Composition Group, University of Bern. All rights reserved.
*/
package ch.unibe.scg.autoca;
import ch.unibe.scg.autoca.mode.ScanMode;

public final class AuToCa {

    private DataSet dataset;

    public AuToCa(DataSet dataset) {
    	this.dataset = dataset;    	   	
    }
    
    public void executeScan() {
    	ScanMode scanmode = new ScanMode();
    	scanmode.execute(dataset);
	}
    
    public void executeAnalysis(){
    	
    }

    public static void main(String[] args) {
    	DataSet dataset = new DataSet();
    	dataset.loadStandardDataSet();;
    	dataset.extractSourceFiles();
		//AuToCa autoca = new AuToCa(dataset);		
		//autoca.executeScan();
    }
}
