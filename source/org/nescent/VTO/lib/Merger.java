package org.nescent.VTO.lib;

import java.io.File;

/**
 * 
 * @author pmidford
 *
 */
public interface Merger {
		
	/* Metadata methods report what the Merger can and cannot do */
	boolean canAttach();
	
	boolean canPreserveID();
	
	/* This specifies whether to preserve an ID (only works in an attach) 
	 * This can throw an exception if canPreserveID is false. */
	void setPreserveID(boolean v);
	
	void setPreserveSynonyms(SynonymSource s);
	
	void setSource(File source);
	
	void setTarget(TaxonStore target);
	
	void merge(String prefix);
	
	// If the prefix of a term's ID equals the passed prefix, don't generate a new id for the term
	void attach(String parent, String cladeRoot, String prefix);

}
