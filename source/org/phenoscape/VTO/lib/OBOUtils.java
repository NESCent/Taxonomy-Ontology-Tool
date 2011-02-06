package org.phenoscape.VTO.lib;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

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
import org.obo.datamodel.PropertyValue;
import org.obo.datamodel.Synonym;
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

	
	final private OBOSession theSession;
	final private OBOProperty isaProperty;
	private OBOProperty hasRankProperty;
	static final Logger logger = Logger.getLogger(OBOUtils.class.getName());
	private ObjectFactory oboFactory = null; 
	
	private Collection<OBOClass> terms;
	private Map<String,OBOClass> termNames;
	private Map<String,IdentifiedObject> termIDs;
	
	private boolean dirtyTermSets = false;
	
	/**
	 * Constructor for use by OBOStore where the session doesn't come from parsing a file
	 */
	OBOUtils(){
		theSession = DefaultObjectFactory.getFactory().createSession();
		oboFactory = theSession.getObjectFactory();
		isaProperty = lookupProperty(ISA_PROPERTY);		
		hasRankProperty = (OBOProperty)theSession.getObjectFactory().createObject(RANK_PROPERTY, OBOClass.OBO_PROPERTY, false);
		hasRankProperty.setName("has taxonomic rank");
		theSession.addObject(hasRankProperty);
		terms = TermUtil.getTerms(theSession);
		termNames = getAllTermNamesHash(terms);
		termIDs = getAllTermIDsHash(terms);
		dirtyTermSets = false;
	}

	/**
	 * Constructor for use by OBOMerger where the session
	 * @param path
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
			theSession.addObject(hasRankProperty);
		}
		terms = TermUtil.getTerms(theSession);
		termNames = getAllTermNamesHash(terms);
		termIDs = getAllTermIDsHash(terms);
		dirtyTermSets = false;
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
	
	public Synonym makeSynonym(String synString, String dbxprefix, String entryID){
		String cleanSyn = cleanIllegalCharacters(synString);
		Synonym s = oboFactory.createSynonym(cleanSyn, Synonym.RELATED_SYNONYM);
		Dbxref d = (Dbxref)oboFactory.createDbxref(dbxprefix, entryID, "", Dbxref.RELATED_SYNONYM, null);           
		s.addXref(d);
		return s;
	}

	  /**
	   * This cleans up Latin-1 (and a few UTF-8) characters - apparently it's what Excel-2003 format uses, at least the mac version.
	   * This may have to be re-addressed at some point.  Using String.replace() is inefficient in the case of Latin-1, but 
	   * this really ought to be receiving UTF-8, which the builder for the collection vocabulary uses, so I'll leave it this way. 
	   * @param syn the (synonym) string to clean of extended characters
	   * @return the cleaned up (synonym) string
	   */
	  private static String cleanIllegalCharacters(final String syn){
		  StringBuffer synB = new StringBuffer(syn);
		  for(int k=0;k<synB.length();k++){
			  final int c = synB.codePointAt(k);
			  switch (c) {
			  case 252: {
				  synB.replace(k,k+1,"u");
				  break;
			  }
			  case 225: {
				  synB.replace(k,k+1,"a");
				  break;
			  }
			  case 228: {
				  synB.replace(k,k+1,"a");
				  break;
			  }
			  case 232:
			  case 233: {
				  synB.replace(k,k+1,"e");
				  break;
			  }
			  case 237: {
				  synB.replace(k,k+1,"i");
				  break;
			  }
			  case 241: {
				  synB.replace(k,k+1,"n");
				  break;
			  }
			  case 243:
			  case 244:
			  case 246:
			  case 248: {
				  synB.replace(k,k+1,"o");
				  break;
			  }
			  case 8217: {
				  synB.replace(k, k+1, "'");
				  break;
			  }
			  default:{
				  if (c > 127){
					  System.out.println("** Hist " + c);
				  }
			  }
			  }
		  }	
		  return synB.toString();
	  }

	  private static String cleanIllegalCharactersAndDelimiters(final String syn){
		  final String cleanString = cleanIllegalCharacters(syn);
		  if ((cleanString.startsWith("\"") && cleanString.endsWith("\"")) ||
				  (cleanString.startsWith("'") && cleanString.endsWith("'"))){
			  return cleanString.substring(1,cleanString.length()-1);
		  }
		  else return cleanString;
	  }
	  
	  public Collection<OBOClass> getTerms(){
		  return TermUtil.getTerms(theSession);
	  }
	  
	  public Dbxref createDbxref(String name1, String name2, String name3, int synonymtype){
		  return theSession.getObjectFactory().createDbxref(name1, name2, name3, synonymtype,null);
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
	  }

	  private void addRankProperty(IdentifiedObject c, String rank){
		  PropertyValue rankProperty = oboFactory.createPropertyValue(PROPERTYVALUE_TAG,RANK_PROPERTY + " " + rank);
		  c.addPropertyValue(rankProperty);
	  }

	  public void setNameSpace(String namespace, String filepath){
		  Namespace n = oboFactory.createNamespace(namespace, filepath);
		  theSession.setDefaultNamespace(n);
	  }
	  
	  public OBOProperty getISAproperty() {
		  return isaProperty;
	  }

	  public static Map<String,OBOClass> getAllTermNamesHash(Collection <OBOClass> terms){
		  final HashMap<String,OBOClass> result = new HashMap<String,OBOClass>(terms.size());
		  for (OBOClass item : terms){
			  if (item.getName() == null)
				  logger.error("Term " + item.getID() + " has null for name");
			  else {
				  if (result.get(item.getName()) != null)
					  logger.error("Hash collision in building names hash; Name = " + item.getName() + " old ID = " + ((OBOClass)result.get(item.getName())).getID() + " new ID = " + item.getID());
				  else
					  result.put(item.getName(), item);
			  }
		  }
		  return result;
	  }


	  private Map<String, IdentifiedObject> getAllTermIDsHash(Collection <OBOClass> terms){
		  return TermUtil.createIDMap(terms);
	  }

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

	public OBOClass lookupTermByName(String termName) {
		terms = TermUtil.getTerms(theSession);
		for (OBOClass item : terms){
			if (termName.equals(item.getName()))
				return item;
		}
		return null;
	}


}
