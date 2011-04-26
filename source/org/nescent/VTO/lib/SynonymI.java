package org.nescent.VTO.lib;

import org.obo.datamodel.Synonym;

public interface SynonymI {

	Synonym asOBOSynonym();
	
	String getID();

	public String getText();

}
