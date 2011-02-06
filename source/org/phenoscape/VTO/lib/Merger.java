package org.phenoscape.VTO.lib;

import java.io.File;

public interface Merger {
	
	boolean canAttach();
	
	void merge(File source, TaxonStore target, String prefix);
	
	void attach(File source, TaxonStore target, String parent, String prefix);

}
