package org.nescent.VTO.lib;

import java.util.List;
import java.util.Set;

import org.obo.datamodel.OBOClass;
import org.semanticweb.owlapi.model.OWLIndividual;

public interface Term {
	
	public OBOClass asOBOClass();
	
	public OWLIndividual asOWLIndividualTerm();

	public SynonymI getOldSynonym(String dbID);
	
	public void addSynonym(SynonymI s);
	
	public void setComment(String comment);
	
	public String getLabel();

	public Set<SynonymI> getSynonyms();
	
	public String getID();
	
	public Set<Term> getChildren();
	
	public Set<String> getCrossReferences();

	public boolean hasAncestor(String string);
	
	public boolean isExtinct();

	public void removeProperties();
	
	void createAltID(String id);

	public List<Term> getAncestors();
	
	public boolean isObsolete();
	
	public void removeParent(Term parent);


}
