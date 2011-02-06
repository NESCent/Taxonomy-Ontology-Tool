package org.phenoscape.VTO.lib;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.obo.dataadapter.DefaultOBOParser;
import org.obo.dataadapter.OBOParseEngine;
import org.obo.dataadapter.OBOParseException;
import org.obo.datamodel.IdentifiedObject;
import org.obo.datamodel.OBOClass;
import org.obo.datamodel.OBORestriction;
import org.obo.datamodel.OBOSession;
import org.obo.datamodel.ObjectFactory;
import org.obo.datamodel.PropertyValue;
import org.obo.datamodel.Synonym;

public class OBOStore implements TaxonStore {



	static final public String FAMILYSTRING = "family";
	static final public String GENUSSTRING = "genus";
	static final public String SPECIESSTRING = "species";



	private final static Map<String,String> rankIDs = new HashMap<String,String>(60);

	//this ought to be changed to the appropriate OBO purl
	static final String TAXON_RANK_URL = "https://phenoscape.svn.sourceforge.net/svnroot/phenoscape/trunk/vocab/taxonomic_rank.obo";


	static Logger logger = Logger.getLogger(OBOStore.class.getName());
	
	private final String defaultPrefix;

	String targetFile;

	private final OBOUtils u;
	
	private int idCounter = 0;
	private String idSuffix = ":%07d";
	private String defaultFormat;

	public OBOStore(String fileSpec, String prefix, String oboNameSpace) {
		u = new OBOUtils();
		u.setNameSpace(oboNameSpace, fileSpec);
		defaultPrefix = prefix;
		defaultFormat = defaultPrefix + idSuffix;
		
		targetFile = fileSpec;
		fillRankNames();

		// TODO Auto-generated constructor stub
	}


	/**
	 * Initially this will build a fresh set of translated terms everytime - need to fix
	 */
	public Collection<Term> getTerms(){
		Collection<OBOClass> source = u.getTerms();   //getting this first allows pre-setting the set size
		Collection<Term> result = new HashSet<Term>(source.size());
		for (OBOClass oc : source){
			result.add(new OBOTerm(oc));
		}
		return result;
	}
	
	@Override
	public boolean isEmpty(){
		return u.isEmpty();
	}
	
	@Override
	public Term getTerm(String termID) {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * Unwraps the terms and passes them through to the session wrapper
	 * @param child
	 * @param parent
	 */
	public void attachParent(Term child, Term parent){
		u.attachParent(child.asOBOClass(), parent.asOBOClass());
	}

	
	@Override
	public Term addTerm(String name) {
		// need to generate the ID
		String newID = String.format(defaultFormat,idCounter++);
		return new OBOTerm(u.makeTerm(newID, name));
	}


	@Override
	public Term addTermbyID(String ID, String name) {
		return new OBOTerm(u.makeTerm(ID, name));
	}




	@Override
	public String getRankID(String name){
		return rankIDs.get(name);
	}


	/**
	 * This fills the table that maps rank ids to names
	 */
	private static void fillRankNames(){
		DefaultOBOParser parser = new DefaultOBOParser();
		OBOParseEngine engine = new OBOParseEngine(parser);
		engine.setPath(TAXON_RANK_URL);
		try {
			engine.parse();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (OBOParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		OBOSession rankSession = parser.getSession();
		Collection<IdentifiedObject> rankObjects = rankSession.getObjects();
		for(IdentifiedObject io : rankObjects){
			rankIDs.put(io.getName(),io.getID());  // will pickup some extra terms (e.g., OBO:Term), but we can ignore those
		}
	}



	@Override
	public void saveStore() {
		u.saveOBOSession(targetFile);
	}


	@Override
	public void setRank(Term term, Term rank) {
		// TODO Auto-generated method stub
		
	}


	@Override
	public Term getRank(String rankName) {
		final OBOClass rank = u.lookupTermByName(rankName);
		if (rank != null)
			return new OBOTerm(rank);
		else
			return null;
	}


	@Override
	public Term getTermbyName(String termName) {
		final OBOClass term = u.lookupTermByName(termName);
		if (term != null)
			return new OBOTerm(term);
		else
			return null;
	}


	public SynonymI makeSynonym(String synString, String dbxprefix, String entryID ){
		final Synonym s = u.makeSynonym(synString, dbxprefix, entryID);
		return new OBOSynonym(s);
	}


}
