package org.nescent.VTO.lib;

import java.io.File;

public interface Merger {
	
	/* Metadata methods report what the Merger can and cannot do */
	boolean canAttach();
	
	boolean canPreserveID();

	void setSource(File source);
	
	void setTarget(TaxonStore target);
	
	void merge(String prefix);
	
	void attach(String parent, String cladeRoot, String prefix, boolean preserveIDs);

}
