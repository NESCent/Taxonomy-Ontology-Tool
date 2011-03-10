package org.phenoscape.VTO.lib;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.obo.datamodel.Dbxref;
import org.obo.datamodel.Link;
import org.obo.datamodel.OBOClass;
import org.obo.datamodel.OBOProperty;
import org.obo.datamodel.PropertyValue;
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
	
	public Set<SynonymI> getSynonyms(){
		Set<SynonymI> result = new HashSet<SynonymI>();
		for (Synonym s : term.getSynonyms()){
			result.add(new OBOSynonym(s));
		}
		return result;
	}



	@Override
	public OBOClass asOBOClass() {
		return term;
	}

	@Override
	public String getLabel() {
		return term.getName();
	}
	
	@Override
	public String getID(){
		return term.getID();
	}

	@Override
	public void addRank(String rank) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Set<Term> getChildren() {
		Set<Term> results = new HashSet<Term>();
		Collection<Link> childLinks = term.getChildren();
		for(Link l : childLinks){
			OBOProperty lType = l.getType();
			if (OBOUtils.ISA_PROPERTY.equals(lType.getID())){
				OBOClass child = (OBOClass)l.getChild();
				results.add(new OBOTerm(child));
			}
		}
		return results;
	}


	@Override
	public Set<String> getCrossReferences() {
		// TODO Auto-generated method stub
		return null;
	}

	
	
}
