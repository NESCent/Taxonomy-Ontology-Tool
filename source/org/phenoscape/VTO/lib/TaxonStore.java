package org.phenoscape.VTO.lib;

import java.util.Collection;

public interface TaxonStore {
	
	
	public void saveStore();
	
	public Term getTerm(String termID);

	public Collection<Term> getTerms();
	
	/**
	 * 
	 * @param name the taxonomic name of the term
	 * @return the ID of the new term
	 */
	public Term addTerm(String name);
	
	public void attachParent(Term child, Term parent);
	
	public void setRank(Term term, Term rank);
	
	public String getRankID(String rankName);
	
	public Term getRank(String rankName);

	public Term getTermbyName(String taxonName);

	public SynonymI makeSynonym(String syn, String prefix, String xref);
	
	public boolean isEmpty();

	public Term addTermbyID(String ID, String name);
	
	// these should be accept from Merger
}
