package org.nescent.VTO.lib;

import java.util.Collection;
import java.util.List;

public interface TaxonStore {
	
	
	public void saveStore();
	
	/**
	 * 
	 * @param termID name of requested term
	 * @return wrapped term named by termID
	 */
	public Term getTerm(String termID);

	/**
	 * 
	 * @return all terms in the store
	 */
	public Collection<Term> getTerms();
	
	/**
	 * 
	 * @param name the taxonomic name of the term
	 * @return the ID of the new term
	 */
	public Term addTerm(String name);
	
	/**
	 * Attaches subsumption relation (is_a) between a child term and a parent term
	 * @param child term subsumed by parent
	 * @param parent term subsuming child
	 */
	public void attachParent(Term child, Term parent);
	
	/**
	 * 
	 * @param term
	 * @param rank
	 */
	public void setRankFromName(Term term, String rank);
	
	public void setRankFromID(Term term, String rankID);
	
	public String getRankID(String rankName);
	
	public Term getTermbyName(String taxonName);
	
	/**
	 * This just checks that a term exists, doesn't need to build anything
	 * @param taxonName
	 * @return true if store has a term with taxonName
	 */
	public boolean hasTermbyName(String taxonName);

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
	
	public void saveAllColumnFormat(String targetFilterPrefixStr);

	public void trim(String nodeStr);

	public Term getTermByXRef(String dbName, String dbID);

	public String getRankString(Term term);
	
	public void setExtinct(Term term);
	
	/**
	 * This generates a report of counts of terms, synonyms, breakdown by rank, etc.
	 * @return
	 */
	public List<String> countTerms();

	/**
	 * This provides the store a chance to (re)initialize the id generator before a merge
	 * @param prefix 
	 */
	public void updateIDGenerator(String prefix);

	
	// these should be accept from Merger
}
