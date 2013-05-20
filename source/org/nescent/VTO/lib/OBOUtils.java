package org.nescent.VTO.lib;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.log4j.Logger;
import org.bbop.dataadapter.DataAdapterException;
import org.obo.dataadapter.DefaultOBOParser;
import org.obo.dataadapter.OBOParseEngine;
import org.obo.dataadapter.OBOParseException;
import org.obo.dataadapter.OBOSerializationEngine;
import org.obo.dataadapter.OBO_1_2_Serializer;
import org.obo.datamodel.Dbxref;
import org.obo.datamodel.IdentifiedObject;
import org.obo.datamodel.Namespace;
import org.obo.datamodel.OBOClass;
import org.obo.datamodel.OBOProperty;
import org.obo.datamodel.OBORestriction;
import org.obo.datamodel.OBOSession;
import org.obo.datamodel.ObjectFactory;
import org.obo.datamodel.ObsoletableObject;
import org.obo.datamodel.PropertyValue;
import org.obo.datamodel.Synonym;
import org.obo.datamodel.SynonymType;
import org.obo.datamodel.impl.DefaultObjectFactory;
import org.obo.util.TermUtil;

/**
 * 
 * @author peter
 *
 */


class OBOUtils {

	static final public String PROPERTYVALUE_TAG = "property_value";
	static final public String ISA_PROPERTY = "OBO_REL:is_a"; 
	static final public String RANK_PROPERTY = "has_rank";
	static final public String EXTINCT_PROPERTY = "is_extinct";
	
	static final public String TRUE_VALUE = "\"true\" xsd:boolean";   //this should probably be some sort of XST:boolean expression


	final private OBOSession theSession;
	final private OBOProperty isaProperty;
	private OBOProperty hasRankProperty;
	final private SynonymType commonNameType;
	final private SynonymType misspelledType;
	final private SynonymType nameUsageType;
	final private Set<SynonymType> knownSynonymTypes;
	static final Logger logger = Logger.getLogger(OBOUtils.class.getName());
	private ObjectFactory oboFactory = null; 

	private Collection<OBOClass> terms;
	private Map<String,OBOClass> termNames;
	private Map<String,IdentifiedObject> termIDs;

	private boolean dirtyTermSets = false;


	private final Map<String,String> rankIDs = new HashMap<String,String>(60);   //rank name -> rankID (e.g., species->TAXRANK:0000006


	//this ought to be changed to the appropriate OBO purl
	static final String TAXON_RANK_URL = "https://phenoscape.svn.sourceforge.net/svnroot/phenoscape/trunk/vocab/taxonomic_rank.obo";
	//static final String TAXON_RANK_URL = "file:///Users/peter/Projects/VTO/Data/taxonomic_rank.obo";

	/**
	 * Constructor for use by OBOStore where the session doesn't come from parsing a file
	 */
	OBOUtils(){
		theSession = DefaultObjectFactory.getFactory().createSession();
		oboFactory = theSession.getObjectFactory();
		isaProperty = lookupProperty(ISA_PROPERTY);		
		hasRankProperty = (OBOProperty)theSession.getObjectFactory().createObject(RANK_PROPERTY, OBOClass.OBO_PROPERTY, false);
		hasRankProperty.setName("has taxonomic rank");
		commonNameType = theSession.getObjectFactory().createSynonymType("COMMONNAME", "common name", Synonym.RELATED_SYNONYM);
		misspelledType = theSession.getObjectFactory().createSynonymType("MISSPELLING", "misspelling", Synonym.EXACT_SYNONYM);
		nameUsageType = theSession.getObjectFactory().createSynonymType("TAXONNAMEUSAGE", "name with (author year)", Synonym.NARROW_SYNONYM);
		knownSynonymTypes = new HashSet<SynonymType>(3);
		knownSynonymTypes.add(commonNameType);
		knownSynonymTypes.add(misspelledType);
		knownSynonymTypes.add(nameUsageType);
		theSession.addSynonymType(commonNameType);
		theSession.addSynonymType(misspelledType);
		theSession.addSynonymType(nameUsageType);
		terms = TermUtil.getTerms(theSession);
		termNames = getAllTermNamesHash(terms);
		termIDs = getAllTermIDsHash(terms);
		dirtyTermSets = false;
		fillRankNames();
	}

	/**
	 * Constructor for use by OBOMerger - will load an OBO file into a fresh session within this Utils instance
	 * @param path specifies the OBO file to load into the session
	 */
	OBOUtils(String path){
		DefaultOBOParser parser = new DefaultOBOParser();
		OBOParseEngine engine = new OBOParseEngine(parser);
		engine.setPath(path);
		try {
			engine.parse();
		} catch (IOException e) {
			logger.error("A error occurred while opening or reading from: " + path);
			e.printStackTrace();
		} catch (OBOParseException e) {
			logger.error("A error occurred while parsing the ontology in " + path);
			e.printStackTrace();
		}
		theSession = parser.getSession();
		oboFactory = theSession.getObjectFactory();
		isaProperty = lookupProperty(ISA_PROPERTY);
		hasRankProperty = lookupProperty(RANK_PROPERTY);
		if (hasRankProperty == null){
			logger.warn("Ontology loaded from " + path + " has no has_rank property");
			hasRankProperty = (OBOProperty)theSession.getObjectFactory().createObject(RANK_PROPERTY, OBOClass.OBO_PROPERTY, false);
			hasRankProperty.setName("has taxonomic rank");
			//theSession.addObject(hasRankProperty);
		}
		commonNameType = theSession.getObjectFactory().createSynonymType("COMMONNAME", "common name", Synonym.RELATED_SYNONYM);
		misspelledType = theSession.getObjectFactory().createSynonymType("MISSPELLING", "misspelling", Synonym.EXACT_SYNONYM);
		nameUsageType = theSession.getObjectFactory().createSynonymType("TAXONNAMEUSAGE", "name with (author year)", Synonym.NARROW_SYNONYM);
		theSession.addSynonymType(commonNameType);
		theSession.addSynonymType(misspelledType);
		theSession.addSynonymType(nameUsageType);
		knownSynonymTypes = new HashSet<SynonymType>(3);
		knownSynonymTypes.add(commonNameType);
		knownSynonymTypes.add(misspelledType);
		knownSynonymTypes.add(nameUsageType);
		terms = TermUtil.getTerms(theSession);
		termNames = getAllTermNamesHash(terms);
		termIDs = getAllTermIDsHash(terms);
		dirtyTermSets = false;
		fillRankNames();
	}


	/**
	 * 
	 * @param session
	 * @param name
	 * @return
	 */
	protected OBOProperty lookupProperty(String name){
		Collection<OBOProperty> relations = TermUtil.getRelationshipTypes(theSession);   
		for (OBOProperty p : relations){
			if (p.getID().equalsIgnoreCase(name))
				return p;
		}
		return null;
	}

	/**
	 * 
	 */
	public boolean isEmpty(){
		int termCount = TermUtil.getTerms(theSession).size();
		return (termCount < 10);  
	}

	/**
	 * Simply wraps an OBOclass in a new term wrapper
	 * @param term
	 * @return wrapped class
	 */
	public OBOClass makeTerm(String id, String name){
		OBOClass c = (OBOClass)oboFactory.createObject(id,OBOClass.OBO_CLASS, false);
		c.setName(name);
		theSession.addObject(c);
		dirtyTermSets = true;
		return c;
	}

	/**
	 * Simply wraps an existing OBO synomym in a synonym wrapper
	 * @param s
	 * @return wrapped synonym
	 */
	public SynonymI makeSynonym(Synonym s){
		return new OBOSynonym(s);
	}

	public Synonym makeSynonym(String synString){
		Synonym s = oboFactory.createSynonym(synString, Synonym.RELATED_SYNONYM);
		return s;
	}
	
	public Synonym makeSynonymWithType(String synString,SynonymType st){
		Synonym s = oboFactory.createSynonym(synString, Synonym.RELATED_SYNONYM);
		s.setSynonymType(st);
		return s;
	}

	public Synonym makeSynonymWithXref(String synString, String dbxprefix, String entryID){
		Synonym s = oboFactory.createSynonym(synString, Synonym.RELATED_SYNONYM);
		Dbxref d = (Dbxref)oboFactory.createDbxref(dbxprefix, entryID, "", Dbxref.RELATED_SYNONYM, null);           
		s.addXref(d);
		return s;
	}

	/**
	 * 
	 * @param synString
	 * @param st
	 * @param dbxprefix
	 * @param entryID
	 * @return
	 */
	public Synonym makeSynonymWithTypeAndXref(String synString, SynonymType st, String dbxprefix, String entryID){
		Synonym s = oboFactory.createSynonym(synString, Synonym.RELATED_SYNONYM);
		Dbxref d = (Dbxref)oboFactory.createDbxref(dbxprefix, entryID, "", Dbxref.RELATED_SYNONYM, null);           
		s.addXref(d);
		s.setSynonymType(st);
		return s;
	}

	public Collection<OBOClass> getTerms(){
		return TermUtil.getTerms(theSession);
	}


	public Dbxref createDbxref(String db, String id, String desc, int synonymtype){
		return theSession.getObjectFactory().createDbxref(db, id, desc, synonymtype,null);
	}
	

	public OBORestriction attachParent(OBOClass child, OBOClass parent){
		final OBORestriction res = theSession.getObjectFactory().createOBORestriction(child,getISAproperty(),parent,false);
		child.addParent(res);
		parent.addChild(res);                            
		return res; 
	}

	public void installTerm(IdentifiedObject newTerm,Map<String, IdentifiedObject> termIDs){
		theSession.addObject(newTerm);
		termIDs.put(newTerm.getID(), newTerm);
		dirtyTermSets = true;
	}



	public PropertyValue createRankProperty(String id) {
		if (!id.contains(":")){  //can't be an id with no column - so throw something nasty
			throw new IllegalArgumentException("createRankProperty received " + id + " which is not an OBO ID");
		}
		return oboFactory.createPropertyValue(PROPERTYVALUE_TAG,RANK_PROPERTY + " " + id);
	}

	public void setExtinct(IdentifiedObject c){
		if (c == null){
			throw new RuntimeException("Null obo class passed to setExtinct");
		}
		final PropertyValue extinctProperty = 
			      oboFactory.createPropertyValue(PROPERTYVALUE_TAG,
			    		                         EXTINCT_PROPERTY + 
			    		                         " " + 
			    		                         TRUE_VALUE);
		c.addPropertyValue(extinctProperty);
	}
	
	public boolean isExtinct(IdentifiedObject c){
		for (PropertyValue v : c.getPropertyValues()){
			String[] pair = v.getValue().split(" ");
			if (EXTINCT_PROPERTY.equalsIgnoreCase(pair[0]))
				return true;
		}
		return false;
	}
	
	public void resetExtinct(OBOClass c) {
		final PropertyValue extinctProperty = 
		      oboFactory.createPropertyValue(PROPERTYVALUE_TAG,
		    		                         EXTINCT_PROPERTY + 
		    		                         " " + 
		    		                         TRUE_VALUE); 
		c.removePropertyValue(extinctProperty);
		
	}

	public void setNameSpace(String namespace, String filepath){
		Namespace n = oboFactory.createNamespace(namespace, filepath);
		theSession.setDefaultNamespace(n);
	}
	
	public void setOntologyTag(String ontologyStr){
	}

	public OBOProperty getISAproperty() {
		return isaProperty;
	}

	public OBOProperty getHasRank(){
		return hasRankProperty; 
	}
	
	

	public static Map<String,OBOClass> getAllTermNamesHash(Collection <OBOClass> terms){
		final HashMap<String,OBOClass> result = new HashMap<String,OBOClass>(terms.size());
		for (OBOClass item : terms){
			if (item.getName() == null)
				logger.error("Term " + item.getID() + " has null for name");
			else {
				if (result.get(item.getName()) != null){
					//logger.error("Hash collision in building names hash; Name = " + item.getName() + " old ID = " + ((OBOClass)result.get(item.getName())).getID() + " new ID = " + item.getID());
				}
				else
					result.put(item.getName(), item);
			}
		}
		return result;
	}


	private Map<String, IdentifiedObject> getAllTermIDsHash(Collection <OBOClass> terms){
		return TermUtil.createIDMap(terms);
	}

	public OBOClass lookupTermByName(String termName) {
		if (termName == null){
			throw new RuntimeException("termName is null");
		}
		if (dirtyTermSets){
			terms = TermUtil.getTerms(theSession);
			termNames = getAllTermNamesHash(terms);
			termIDs = getAllTermIDsHash(terms);
			dirtyTermSets = false;
		}
		OBOClass result;
		result = termNames.get(termName);
		if (result == null){
			for (IdentifiedObject io : theSession.getLinkDatabase().getObjects()){
				if (io.getName().equals(termName)){
					logger.info("Term names (" + termName +") failed; Found matching object in sessions link database: " + io);
					logger.info("Matching object is " + io.getType() + " and obsolete: " + ((ObsoletableObject) io).isObsolete());
					termNames.put(termName, ((OBOClass)io));
					logger.info("check put " + termNames.get(termName));
					return ((OBOClass)io);
				}
					
			}
		return termNames.get(termName);
		}
		else {
			return result;
		}
	}
	
	public OBOClass lookupTermByXRef(String dbName, String dbID) {
		if (dirtyTermSets){
			terms = TermUtil.getTerms(theSession);
			termNames = getAllTermNamesHash(terms);
			termIDs = getAllTermIDsHash(terms);
			dirtyTermSets = false;
		}
		terms = TermUtil.getTerms(theSession);
		if (dbName == null){
			throw new RuntimeException("lookupTermByXref received null for database");
		}
		if (dbID == null){
			throw new RuntimeException("lookupTermByXref received null for dbID");
		}
		for (OBOClass item : terms){
			for (Dbxref ref : item.getDbxrefs()){
				if (dbName.equals(ref.getDatabase()) && dbID.equals(ref.getDatabaseID())){
					return item;
				}
			}
		}
		return null;
	}
	

	public Map<String,String> getSynonyms(OBOClass term){
		Map<String,String> result = new HashMap<String,String>();
		for (Synonym s : term.getSynonyms()){
			for (Dbxref ref : s.getXrefs()){
				result.put(s.getText(),ref.getDatabase()+":"+ref.getDatabaseID());
			}
		}
		if (term.getDbxrefs() != null && !term.getDbxrefs().isEmpty()){
			for (Dbxref d : term.getDbxrefs()){
				result.put(term.getName(),d.getDatabase()+ ":" + d.getDatabaseID());
			}
		}
		else{
			result.put(term.getName(), term.getID());
		}
		return result;
	}

	/**
	 * This fills the table that maps rank ids to names
	 */
	void fillRankNames(){
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


	public String getRankID(String name){
		return rankIDs.get(name);
	}


	public String getRankName(String ID){
		if (rankIDs.containsValue(ID)){
			for (Entry<String,String> ent : rankIDs.entrySet()){
				if (ent.getValue().equals(ID))
					return ent.getKey();
			}
			return null;
		}
		return null;
	}

	public void setRankFromID(OBOClass cl, String rankID){
		PropertyValue rankProperty = createRankProperty(rankID);
		cl.addPropertyValue(rankProperty);
	}

	public void setRankFromName(OBOClass cl, String rankName){
		String rankID = rankIDs.get(rankName);
		if (rankID != null)
			setRankFromID(cl,rankID);
	}



	public String getRankString(OBOClass term){
		for (PropertyValue pv : term.getPropertyValues()){
			if (PROPERTYVALUE_TAG.equals(pv.getProperty())){  // more checking
				String propVal = pv.getValue();
				int spacePos = propVal.indexOf(' ');
				String propertyName = propVal.substring(0, spacePos);
				if (RANK_PROPERTY.equals(propertyName)){
					String rankName = getRankName(propVal.substring(spacePos+1));
					return rankName;
				}
			}
		}
		return null;
	}

	
	public void removeRank(OBOClass cl){
		PropertyValue rankPropertyValue = null;
		for (PropertyValue pv : cl.getPropertyValues()){
			if (PROPERTYVALUE_TAG.equals(pv.getProperty())){  // more checking
				String propVal = pv.getValue();
				int spacePos = propVal.indexOf(' ');
				String propertyName = propVal.substring(0, spacePos);
				if (RANK_PROPERTY.equals(propertyName)){
					rankPropertyValue = pv;
					break;
				}
			}
		}
		if (rankPropertyValue != null){
			cl.removePropertyValue(rankPropertyValue);
		}
	}	

	public String makeUnderScoreJoinedName(String rawString){
		String[] components = rawString.split(" ");
		if (components.length == 1)
			return components[0];
		else if (components.length>0){
			StringBuilder b = new StringBuilder();
			for(int i=0;i<components.length-1;i++){
				b.append(components[i]);
				b.append('_');
			}
			b.append(components[components.length-1]);
			return b.toString();
		}
		else return "";
	}

	protected void removeNode(OBOClass target){
		theSession.removeObject(target);	
		dirtyTermSets=true;
	}

	public List<String> countTerms(){
		List<String> result = new ArrayList<String>();
		List<String> rankUsages = new ArrayList<String>();
		int[] rankCounts = new int[100];
		int termCount = 0;
		int synonymCount = 0;
		int commonNameCount = 0;
		int misspellingCount = 0;
		int NCBIxrefCount = 0;
		int extinctCount = 0;
		SynonymType commonNameType = null;
		SynonymType misspellingType = null;
		for(SynonymType st : theSession.getSynonymTypes()){
			if ("COMMONNAME".equals(st.getID()))
				commonNameType = st;
			if ("MISSPELLING".equals(st.getID()))
				misspellingType = st;
		}
		Collection<OBOClass> terms = TermUtil.getTerms(theSession);
		for (OBOClass term : terms){
			String termID = term.getID();
			String[] idSplit = termID.split(":");
			if (!term.isObsolete()){
				termCount++;
				for (Synonym syn : term.getSynonyms()){
					synonymCount++;
					if (commonNameType != null && commonNameType.equals(syn.getSynonymType())){
						commonNameCount++;
					}
					if (misspellingType != null && misspellingType.equals(syn.getSynonymType())){
						misspellingCount++;
					}
				}
				for (Dbxref xref : term.getDbxrefs()){
					if ("NCBITaxon".equals(xref.getDatabase()))
						NCBIxrefCount++;
				}
				for (PropertyValue v : term.getPropertyValues()){
					String[] pair = v.getValue().split(" ");
					if (RANK_PROPERTY.equals(pair[0])){
						String rankStr = pair[1];
						int rankPos = rankUsages.indexOf(rankStr);
						if (rankPos == -1){
							rankUsages.add(rankStr);
							rankPos = rankUsages.indexOf(rankStr);
							rankCounts[rankPos] = 1;
						}
						else{
							rankCounts[rankPos]++;
						}
					}
					if (EXTINCT_PROPERTY.equals(pair[0])){
						extinctCount++;
					}
				}
			}
		}
		result.add("Taxon Terms " + termCount);
		result.add("Synonyms " + synonymCount);
		result.add("Common Names " + commonNameCount);
		result.add("Synonyms resulting from misspelling " + misspellingCount);
		result.add("Extinct taxa " + extinctCount);
		result.add("NCBI Taxa cross referenced " + NCBIxrefCount);
		for(int i = 0; i< rankUsages.size(); i++){
			result.add("Rank: " + getRankName(rankUsages.get(i)) + " count: " + rankCounts[i]);
		}
		return result;
	}



	/* Saving Methods */

	/**
	 * Saves the contents of the session as an obo format text file
	 * @param OBODst specifies a local path for the obo file
	 */
	protected void saveOBOSession(String OBODst){
		OBO_1_2_Serializer serializer = new OBO_1_2_Serializer();
		OBOSerializationEngine se = new OBOSerializationEngine();
		try {
			se.serialize(theSession, serializer, OBODst);
		} catch (DataAdapterException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void obsoleteTerm(Term term) {
		OBOClass c = term.asOBOClass();
		c.setObsolete(true);
	}

	public OBOClass lookupTermByID(String termID) {
		if (dirtyTermSets){
			terms = TermUtil.getTerms(theSession);
			termNames = getAllTermNamesHash(terms);
			termIDs = getAllTermIDsHash(terms);
			dirtyTermSets = false;
		}
		return (OBOClass)termIDs.get(termID);
	}


	public SynonymType getCommonNameType(){
		return commonNameType;
	}
	
	public boolean isKnownSynonymType(SynonymType t){
		return knownSynonymTypes.contains(t);
	}

	public SynonymType lookupSynonymType(String name){
		for (SynonymType st : knownSynonymTypes){
			if (st.getName().equals(name)){
				return st;
			}
		}
		return null;
	}

}
