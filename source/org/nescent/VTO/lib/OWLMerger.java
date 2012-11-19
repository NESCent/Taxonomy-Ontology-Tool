package org.nescent.VTO.lib;

import java.io.File;

import org.apache.log4j.Logger;
import org.nescent.VTO.Builder;

public class OWLMerger implements Merger {

	
	private File source;
	private TaxonStore target;

	private SynonymSource preserveSynonyms;
	private String subAction = Builder.SYNSUBACTION;  // default (currently only implemented) behavior is to merge synonyms

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
	public void setPreserveID(boolean v){
		throw new RuntimeException("This merger can't preserve IDs because TBD");
	}
	
	@Override
	public void setPreserveSynonyms(SynonymSource s){
		preserveSynonyms = s;
	}

	
	@Override 
	public void setSource(File sourceFile){
		source = sourceFile;
	}

	@Override
	public void setTarget(TaxonStore targetStore){
		target = targetStore;
	}
	
	/**
	 * @param sa specifies whether this merges synonyms or cross references
	 */
	@Override
	public void setSubAction(String sa){
		if (Builder.XREFSUBACTION.equals(sa)){
			throw new IllegalArgumentException("Xref merging not currently supported by OWLMerger");
		}
		subAction = sa;
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

	@Override
	public void setURITemplate(String template) {
		// TODO Auto-generated method stub
		
	}

}
