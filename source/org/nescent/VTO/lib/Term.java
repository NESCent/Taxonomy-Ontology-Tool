package org.nescent.VTO.lib;

import java.util.Set;

import org.obo.datamodel.OBOClass;
import org.semanticweb.owlapi.model.OWLIndividual;

public interface Term {
	
	public OBOClass asOBOClass();
	
	public OWLIndividual asOWLIndividualTerm();

	public SynonymI getOldSynonym(String dbID);
	
	public void addSynonym(SynonymI s);
	
	public String getLabel();

	public Set<SynonymI> getSynonyms();
	
	public String getID();
	
	public void addRank(String rank);

	public Set<Term> getChildren();
	
	public Set<String> getCrossReferences();

	public boolean hasAncestor(String string);
	
	public boolean isExtinct();

	void createAltID(String id);

}
