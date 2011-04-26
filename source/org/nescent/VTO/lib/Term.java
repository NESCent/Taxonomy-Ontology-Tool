package org.nescent.VTO.lib;

import java.util.Set;

import org.obo.datamodel.OBOClass;
import org.semanticweb.owlapi.model.OWLClass;
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
	
	//these correspond to OBO Xrefs and their OWL equivalent
	//public void AddCrossReference(String crossRefID);
	
	public Set<String> getCrossReferences();

	public boolean hasAncestor(String string);

}
