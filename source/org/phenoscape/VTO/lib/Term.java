package org.phenoscape.VTO.lib;

import org.obo.datamodel.OBOClass;

public interface Term {
	
	public OBOClass asOBOClass();

	public SynonymI getOldSynonym(String dbID);
	
	public void addSynonym(SynonymI s);
	
	public String getLabel();

	
}
