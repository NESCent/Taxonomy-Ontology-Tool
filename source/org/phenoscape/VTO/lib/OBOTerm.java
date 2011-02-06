package org.phenoscape.VTO.lib;

import org.obo.datamodel.Dbxref;
import org.obo.datamodel.OBOClass;
import org.obo.datamodel.Synonym;

public class OBOTerm implements Term {
	
	private OBOClass term;

	public OBOTerm(OBOClass cl) {
		term = cl;
	}

	public SynonymI getOldSynonym(String dbID){
		Synonym old = null; 
		for (Dbxref dx : term.getDbxrefs()){
			if (dbID.equals(dx.getDatabase())){
				old = dx.getSynonym();
				return new OBOSynonym(old);
			}
		}
		return null;
	}

	
	public void addSynonym(SynonymI s){
		term.addSynonym(s.asOBOSynonym());
	}

	@Override
	public OBOClass asOBOClass() {
		return term;
	}

	@Override
	public String getLabel() {
		return term.getName();
	}
	
	
	
	
}
