package org.phenoscape.VTO.lib;

import java.util.Collection;

import org.apache.log4j.Logger;
import org.obo.datamodel.Dbxref;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLIndividual;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;

public class OWLStore implements TaxonStore {
	
	
	private final OWLUtils u;
	private final String prefixStr;
	private int idCounter = 0;
	private String idSuffix = "_%07d";
	private String defaultFormat;
	
	static final Logger logger = Logger.getLogger(OWLUtils.class.getName());



	/**
	 * 
	 * @param fileSpec
	 * @param prefix
	 * @param nameSpace
	 * @throws OWLOntologyCreationException 
	 */
	public OWLStore(String fileSpec, String prefix, String nameSpace) throws OWLOntologyCreationException{
		u = new OWLUtils("",fileSpec);
		prefixStr = prefix;
		defaultFormat = prefixStr + idSuffix;
	}

	@Override
	public void saveStore() {
		try {
			u.saveOntology();
			logger.info("Done");
		} catch (OWLOntologyStorageException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public Term getTerm(String termID) {
		throw new RuntimeException("Not Implemented");		
	}

	@Override
	public Collection<Term> getTerms() {
		throw new RuntimeException("Not Implemented");
		// TODO Auto-generated method stub
	}

	@Override
	public Term addTerm(String name) {
		String newID = String.format(defaultFormat,idCounter++);
		OWLIndividual owlTaxon = u.makeTerm(newID, name);
		return new OWLTerm(owlTaxon);
	}

	@Override
	public void attachParent(Term child, Term parent) {
		OWLIndividual parentTerm = parent.asOWLIndividualTerm();
		OWLIndividual childTerm = child.asOWLIndividualTerm();
		u.attachParent(childTerm,parentTerm);
		return;
	}

	@Override
	public void setRankFromName(Term term, String rank) {
		return;
		//throw new RuntimeException("Not Implemented");
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
		OWLIndividual oi = u.lookupTermByName(taxonName);
		if (oi != null)
			return new OWLTerm(oi);
		return null;
	}

	@Override
	public SynonymI makeSynonym(String syn) {
		//throw new RuntimeException("Not Implemented");
		return null;
		// TODO Auto-generated method stub
	}

	@Override
	public SynonymI makeSynonymWithXref(String syn, String prefix, String xref) {
		//throw new RuntimeException("Not Implemented");
		return null;
		// TODO Auto-generated method stub
	}

	@Override
	public boolean isEmpty() {
		return u.isEmpty();
	}

	@Override
	public Term addTermbyID(String id, String name) {
		OWLIndividual owlTaxon;
		owlTaxon = u.makeTerm(id, name);
		return new OWLTerm(owlTaxon);
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
		
		//Dbxref newRef = u.createDbxref(dbName, dbID, null, Dbxref.ANALOG);   //not sure this is exactly right, but the short-form constructors in DbxrefImpl suggest it works 
		//t.asOBOClass().addDbxref(newRef);

		//throw new RuntimeException("Not Implemented");
		return;
	}

	@Override
	public Term getTermByXRef(String dbName, String dbID) {
		throw new RuntimeException("Not Implemented");
	}

	
}
