package org.nescent.VTO.lib;

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
	
	public void setRankFromName(Term term, String rank);
	
	public void setRankFromID(Term term, String rankID);
	
	public String getRankID(String rankName);
	
	public Term getTermbyName(String taxonName);

	public SynonymI makeSynonym(String syn);
	
	public SynonymI makeSynonymWithXref(String syn, String prefix, String xref);
	
	public boolean isEmpty();

	public Term addTermbyID(String ID, String name);
	
	//needs to be here as terms don't carry around their factories
	public void addXRefToTerm(Term t, String dbName, String dbID);

	// This saves a file of cross references
	public void saveXref(String targetFilterPrefixStr);

	void saveColumnsFormat(String targetFilterPrefixStr);

	public void saveSynonymFormat(String targetFilterPrefixStr);

	public void trim(String nodeStr);

	public Term getTermByXRef(String dbName, String dbID);
	
	// these should be accept from Merger
}
