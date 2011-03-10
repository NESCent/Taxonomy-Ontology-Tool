package org.phenoscape.VTO.lib;

import java.util.Collection;

public class OWLStore implements TaxonStore {

	@Override
	public void saveStore() {
		throw new RuntimeException("Not Implemented");
		// TODO Auto-generated method stub
		
	}

	@Override
	public Term getTerm(String termID) {
		throw new RuntimeException("Not Implemented");
		// TODO Auto-generated method stub
	}

	@Override
	public Collection<Term> getTerms() {
		throw new RuntimeException("Not Implemented");
		// TODO Auto-generated method stub
	}

	@Override
	public Term addTerm(String name) {
		throw new RuntimeException("Not Implemented");
		// TODO Auto-generated method stub
	}

	@Override
	public void attachParent(Term child, Term parent) {
		throw new RuntimeException("Not Implemented");
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setRankFromName(Term term, String rank) {
		throw new RuntimeException("Not Implemented");
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setRankFromID(Term term, String rankID) {
		throw new RuntimeException("Not Implemented");
		// TODO Auto-generated method stub
		
	}

	@Override
	public String getRankID(String rankName) {
		throw new RuntimeException("Not Implemented");
		// TODO Auto-generated method stub
	}

	@Override
	public Term getTermbyName(String taxonName) {
		throw new RuntimeException("Not Implemented");
		// TODO Auto-generated method stub
	}

	@Override
	public SynonymI makeSynonym(String syn) {
		throw new RuntimeException("Not Implemented");
		// TODO Auto-generated method stub
	}

	@Override
	public SynonymI makeSynonymWithXref(String syn, String prefix, String xref) {
		throw new RuntimeException("Not Implemented");
		// TODO Auto-generated method stub
	}

	@Override
	public boolean isEmpty() {
		throw new RuntimeException("Not Implemented");
		// TODO Auto-generated method stub
	}

	@Override
	public Term addTermbyID(String ID, String name) {
		throw new RuntimeException("Not Implemented");
		// TODO Auto-generated method stub
	}

	@Override
	public void saveXref(String targetFilterPrefixStr) {
		throw new RuntimeException("Not Implemented");
		// TODO Auto-generated method stub
		
	}

	@Override
	public void saveColumnsFormat(String targetFilterPrefixStr) {
		throw new RuntimeException("Not Implemented");
		// TODO Auto-generated method stub
		
	}

	@Override
	public void saveSynonymFormat(String targetFilterPrefixStr) {
		throw new RuntimeException("Not Implemented");
		// TODO Auto-generated method stub
		
	}

	@Override
	public void trim(String nodeStr) {
		throw new RuntimeException("Not Implemented");
		// TODO Auto-generated method stub
		
	}

	
	//likely needs a factory to implement this, so not an OWLTerm method
	@Override
	public void addXRefToTerm(Term t, String dbName, String dbID) {
		throw new RuntimeException("Not Implemented");
		
	}

	@Override
	public Term getTermByXRef(String dbName, String dbID) {
		throw new RuntimeException("Not Implemented");
	}

	
}
