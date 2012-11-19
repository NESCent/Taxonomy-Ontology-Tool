package org.nescent.VTO.lib;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.obo.datamodel.OBOClass;
import org.semanticweb.owlapi.model.OWLIndividual;

public class OWLTerm implements Term {

	private OWLIndividual clade;

	public OWLTerm(OWLIndividual ind) {
		clade = ind;
	}

	@Override
	public OBOClass asOBOClass() {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public OWLIndividual asOWLIndividualTerm(){
		return clade;
	}

	@Override
	public SynonymI getOldSynonym(String dbID) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void addSynonym(SynonymI s) {
		// TODO Auto-generated method stub

	}

	@Override
	public String getLabel() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Set<SynonymI> getSynonyms() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getID() {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public Set<Term> getChildren() {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public Set<String> getCrossReferences() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean hasAncestor(String string) {
		// TODO Auto-generated method stub
		return false;
	}

	
	@Override
	public boolean isExtinct() {
		// TODO Auto-generated method stub
		return false;
	}
	

	@Override
	public void removeProperties() {
		// TODO Auto-generated method stub
		
	}


	@Override
	public void createAltID(String id) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public List<Term> getAncestors() {
		return new ArrayList<Term>();
		// TODO stub
	}

	@Override
	public boolean isObsolete() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void removeParent(Term parent){
	}

}
