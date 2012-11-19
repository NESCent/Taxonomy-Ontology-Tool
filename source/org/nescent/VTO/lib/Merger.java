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

	// This will allows specifying whether this merge adds synonyms or xrefs to existing terms
	void setSubAction(String subAction);
	
	//These next three allow the construction of uri's based on taxonomy from the source (mostly useful for columns)
	void setURITemplate(String template);
	

}
