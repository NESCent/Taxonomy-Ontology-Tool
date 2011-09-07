package org.nescent.VTO.lib;

import java.io.File;

import org.apache.log4j.Logger;

public class OWLMerger implements Merger {

	
	private File source;
	private TaxonStore target;

	
	static final Logger logger = Logger.getLogger(OWLMerger.class.getName());

	/* MetaData methods */
	@Override
	public boolean canAttach() {
		return false;
	}

	@Override
	public boolean canPreserveID(){
		return false;   //TODO look into this
	}

	
	@Override 
	public void setSource(File sourceFile){
		source = sourceFile;
	}

	@Override
	public void setTarget(TaxonStore targetStore){
		target = targetStore;
	}
	


	@Override
	public void merge(String prefix) {
		throw new RuntimeException("OWLMerger not implemented yet");
		// TODO Auto-generated method stub

	}

	@Override
	public void attach(String parent, String cladeRoot, String prefix) {
		throw new RuntimeException("OWLMerger does not currently implement attach");
		// TODO Auto-generated method stub

	}

}
