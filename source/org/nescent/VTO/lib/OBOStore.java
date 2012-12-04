package org.nescent.VTO.lib;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.obo.datamodel.Dbxref;
import org.obo.datamodel.Link;
import org.obo.datamodel.OBOClass;
import org.obo.datamodel.OBOProperty;
import org.obo.datamodel.Synonym;
import org.obo.datamodel.SynonymType;

public class OBOStore implements TaxonStore {


	static final public String CLASSSTRING = "class";
	static final public String ORDERSTRING = "order";
	static final public String FAMILYSTRING = "family";
	static final public String GENUSSTRING = "genus";
	static final public String SPECIESSTRING = "species";
	static final public String SUBSPECIESSTRING = "subspecies";
	static final public String SYNONYMSOURCESTRING = "source";
	static final public String IDSTRING = "id";
	static final public String SYNONYMSTRING = "synonym";    //publication_name?
	static final public String BINOMIALSTRING = "species name";  //genus_species ??
	static final public String[] COLUMNRANKS = {CLASSSTRING,ORDERSTRING,FAMILYSTRING,GENUSSTRING,SPECIESSTRING,SUBSPECIESSTRING};
	static final public String[] SYNONYMRANKS = {FAMILYSTRING,ORDERSTRING,CLASSSTRING};
	static final public String[] SYNONYMCOLUMNS = {SYNONYMSTRING,GENUSSTRING,SPECIESSTRING,FAMILYSTRING,ORDERSTRING,CLASSSTRING,IDSTRING};
	final static String lineSeparator = System.getProperty("line.separator");
	static final String fileSep = System.getProperty("file.separator");


	static Logger logger = Logger.getLogger(OBOStore.class.getName());

	private final String defaultPrefix;

	String targetFile;

	/**
	 * The Utils object holds the obo session and takes care of most of the ugliness
	 */
	private final OBOUtils u;

	private int idCounter = 0;
	private String idSuffix = ":%07d";
	final private String defaultFormat;
	
	//When taxa are trimmed, we want to preserve the id in case the name is reintroduced with a subsequent attach
	private final Map<String,OBOClass> trimmedNames = new HashMap<String,OBOClass>();    //taxon name -> id
	
	//If the name of a trimmed taxon is submitted as a synonym, go head and record the synonym, but save it here
	//A final pass over these may generate obsoleted terms with consider links to the bearer of the synonym
	private final Set<String> trimmedTaxonNameAsSynonym = new HashSet<String>();

	/**
	 * 
	 * @param fileSpec - where this store will eventually write its contents
	 * @param prefix - default prefix for adding terms (may be overridden in some cases)
	 * @param oboNameSpace - default namespace for adding terms
	 */
	public OBOStore(final String fileSpec, final String prefix, final String oboNameSpace) {
		u = new OBOUtils();
		u.setNameSpace(oboNameSpace, fileSpec);
		u.setOntologyTag(oboNameSpace.substring(0, oboNameSpace.indexOf('-')));
		defaultPrefix = prefix;
		defaultFormat = defaultPrefix + idSuffix;		
		targetFile = fileSpec;
	}

	public String getDefaultPrefix(){
		return defaultPrefix;
	}
	
	/**
	 * Initially this will build a fresh set of translated terms everytime - need to fix
	 * @return a copy of the set of terms in the session underlying this OBOStore
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
		final OBOClass cl = u.lookupTermByID(termID);
		if (cl == null){
			return null;
		}
		else {	
			return new OBOTerm(u.lookupTermByID(termID));
		}
	}

	/**
	 * Unwraps the terms and passes them through to the session wrapper
	 * @param child
	 * @param parent
	 */
	public void attachParent(final Term child,final Term parent){
		final OBOClass childClass = child.asOBOClass();
		final OBOClass parentClass = parent.asOBOClass();
		u.attachParent(childClass, parentClass);
	}


	/**
	 * @param prefix specifies the prefix this store will be generating from, so only terms with that prefix are checked
	 * side-effect: the class field idCounter is set to one more than the largest count found in any term id with the 
	 * specified prefix.  Note that this needs to skip over the ids that are currently used and the ids that have been trimmed
	 * but might be reused.
	 */
	@Override
	public void updateIDGenerator(final String prefix){
		if (prefix == null)
			throw new RuntimeException("ID generator received a null prefix");
		int maxCounter = -1;
		maxCounter = classSweep(u.getTerms(), prefix,maxCounter);
		maxCounter = classSweep(trimmedNames.values(),prefix,maxCounter);
		idCounter = maxCounter+1;
	}
	
	
	private int classSweep(final Collection<OBOClass> classCl, final String prefix, final int counter){
		int result = counter;
		for (OBOClass c : classCl){
			final String id = c.getID();
			if (id.startsWith(prefix)){
				int colonIndex = id.indexOf(':');
				String suffix = id.substring(colonIndex+1);
				try{
					int index = Integer.parseInt(suffix);
					if (index > result)
						result = index;
				}
				catch (NumberFormatException e){
					//this really is safe to silently ignore...
				}
			}
		}
		return result;
	}
	
	
	/**
	 * This will either reuse an id (from a trimmed tree) that matches a name or generate a fresh it using the default id prefix
	 */
	@Override
	public Term addTerm(final String name, String prefix) {
		OBOClass addedClass;
		if (prefix == null){
			throw new RuntimeException("Prefix supplied to addTerm was null");
		}
		if (trimmedNames.containsKey(name)){
			final OBOClass oldClass = trimmedNames.get(name);
			String[] oldComponents = oldClass.getID().split(":");
			if ((oldComponents.length > 1) && prefix.equals(oldComponents[0])){
				addedClass = u.makeTerm(oldClass.getID(), name);
			}
			else { //wrong prefix, need new id
				updateIDGenerator(prefix);
				String newID = String.format(prefix+idSuffix,idCounter++);
				addedClass = u.makeTerm(newID, name);
			}
			if (u.isExtinct(oldClass)){
				u.setExtinct(addedClass);
			}
			for (Synonym s : oldClass.getSynonyms()){
				addedClass.addSynonym(s);
			}
		}
		else { 		// need to generate the ID
			updateIDGenerator(prefix);
			String newID = String.format(prefix+idSuffix,idCounter++);
			addedClass = u.makeTerm(newID, name);
		}
		//logger.info("add Term: " + addedClass.getName() +" " + addedClass.getID());
		return new OBOTerm(addedClass);
	}


	/**
	 * 
	 * @param ID the new term will have this id
	 * @param name the new term will be assigned this name
	 * @return a OBOTerm wrapping an OBOClass with the specified id and name
	 */
	@Override
	public Term addTermbyID(final String ID, final String name) {
		String[] idComponents = ID.split(":");
		if (idComponents.length < 2){
			throw new IllegalArgumentException("Provided ID: " + ID + " is not valid OBO syntax");
		}
		if (idComponents[0].equalsIgnoreCase(defaultPrefix)){
			int idIndex = Integer.parseInt(idComponents[1]);
			if (idIndex > idCounter){
				idCounter = idIndex+1;
			}
		}
		return new OBOTerm(u.makeTerm(ID, name));
	}

	public void obsoleteTerm(Term term){
		u.obsoleteTerm(term);
	}

	/**
	 * wrapper to call save the OBOSession to the Store's targetFile
	 */
	@Override
	public void saveStore() {
		u.saveOBOSession(targetFile);
	}


	/**
	 * @param termName
	 * @return an OBOTerm wrapping the OBOClass with the specified name or null if none found
	 */
	@Override
	public Term getTermbyName(final String termName) {
		final OBOClass term = u.lookupTermByName(termName);
		if (term != null)
			return new OBOTerm(term);
		else
			return null;
	}


	/**
	 * @param termName name to search the taxonomy for.
	 * @return true if a term exists in the taxonomy with the specified name
	 */
	public boolean hasTermbyName(final String termName){
		return (u.lookupTermByName(termName) != null);
	}

	
	public SynonymI makeSynonym(final String synString){
		trimmedNameCheck(synString);
		Synonym s = u.makeSynonym(synString);
		return new OBOSynonym(s);
	}

	public SynonymI makeSynonymWithXref(String synString, String dbxprefix, String entryID ){
		trimmedNameCheck(synString);
		final Synonym s = u.makeSynonymWithXref(synString, dbxprefix, entryID);
		return new OBOSynonym(s);
	}
	
	private void trimmedNameCheck(String synString){
		if (trimmedNames.containsKey(synString)){
			trimmedTaxonNameAsSynonym.add(synString);
		}
	}

	public SynonymI makeCommonName(final String commonName){
		final Synonym cn = u.makeSynonymWithType(commonName,u.getCommonNameType());
		return new OBOSynonym(cn);
	}

	public SynonymI makeCommonNameWithXref(final String commonName, String dbxprefix, String entryID ){
		final Synonym cn = u.makeSynonymWithTypeAndXref(commonName,u.getCommonNameType(), dbxprefix, entryID);
		return new OBOSynonym(cn);
	}

	@Override
	public void trim(String targetNode){
		final OBOClass cladeRoot = u.lookupTermByName(targetNode);
		int initialsize = trimmedNames.size();
		if (cladeRoot == null){
			logger.error("Clade root to trim " + targetNode + " not found");
		}
		else{
			removeClade(cladeRoot);
			logger.info("Removed " + (trimmedNames.size()-initialsize) + " net nodes");
		}
	}
	
	
	private void removeClade(OBOClass node){
		if (!isTip(node)){
			final Set<Link> links = new HashSet<Link>();
			links.addAll(node.getChildren());
			for (Link l : links){
				final OBOClass childNode = (OBOClass) l.getChild();
				node.removeChild(l);
				childNode.removeParent(l);
				removeClade(childNode);
			}
		}
		trimmedNames.put(node.getName(), node);
		u.removeNode(node);
	}
	
	public Term getTrimmed(String nodeName){
		return new OBOTerm(trimmedNames.get(nodeName));
	}


	@Override
	public void saveXref(String targetFilterPrefixStr) {
		File originalTargetFile = new File(targetFile);
		String targetName = originalTargetFile.getName();
		File targetXrefFile;
		if (targetName.contains(".")){
			int dotpos = targetName.indexOf('.');
			String dstName = originalTargetFile.getParent() + fileSep + targetName.substring(0,dotpos) + ".txt";
			targetXrefFile = new File(dstName);
		}
		else {
			targetXrefFile = new File(targetName + ".txt");
		}
		List<OBOClass> speciesList = getSpecies();
		PrintWriter targetWriter = null;
		try {
			targetWriter = new PrintWriter(new BufferedWriter(new FileWriter(targetXrefFile)));
		} catch (IOException e) {
			logger.error("An error occurred when opening " + targetXrefFile.getName() + " for output");
			throw new RuntimeException("");
		}
		logger.info("Writing to " + targetXrefFile.getAbsolutePath());
		targetWriter.write("ID\tSpecies\tSynonym");
		targetWriter.write(lineSeparator);
		for(OBOClass term : speciesList){
			//System.out.println("Term is " + term.getName());
			for(Synonym s : term.getSynonyms()){
				boolean saveSynonym = false;
				String dbID = null;  //TODO why aren't we using this?
				for (Dbxref d : s.getXrefs()){
					if (d.getDatabase() != null && d.getDatabase().equalsIgnoreCase(targetFilterPrefixStr)){
						saveSynonym = true;
						dbID = d.getDatabaseID();
					}
				}
				if (saveSynonym){
					String targetXRef = getNamedXref(term);
					if (targetXRef != null){
						targetWriter.write(targetXRef);
						targetWriter.write("\t");
						targetWriter.write(term.getName());
						targetWriter.write("\t");
						targetWriter.write(s.getText());
						targetWriter.write(lineSeparator);
					}
					else{
						throw new RuntimeException("ID with no source XRref " + term.getID());
					}
				}
			}
		}
		targetWriter.close();
		logger.info("Done");
	}

	// Assumes singleton list of xrefs - safer assumption than hardcoding a prefix; needs more thought
	// Perhaps this should be altID?
	String getNamedXref(OBOClass term){
		for (Dbxref d: term.getDbxrefs()){
				return d.getDatabaseID();
		}
		return null;
	}
	
	boolean filterFromSynonymPrefix(OBOClass term, String filterStr){
		for(Synonym s : term.getSynonyms()){
			for (Dbxref d : s.getXrefs()){
				if (d.getDatabase() != null && d.getDatabase().equalsIgnoreCase(filterStr)){
					return true;			
				}
			}
		}
		return false;
	}
	
	
	

	@Override
	public void saveColumnsFormat(String targetFilterPrefixStr){
		File originalTargetFile = new File(targetFile);
		String targetName = originalTargetFile.getName();
		File targetColumnsFile;
		if (targetName.contains(".")){
			int dotpos = targetName.indexOf('.');
			String dstName = originalTargetFile.getParent() + fileSep + targetName.substring(0,dotpos) + ".txt";
			targetColumnsFile = new File(dstName);
		}
		else {
			targetColumnsFile = new File(targetName + ".txt");
		}
		List<OBOClass> tipList = getTips();
		PrintWriter targetWriter = null;
		try {
			targetWriter = new PrintWriter(new BufferedWriter(new FileWriter(targetColumnsFile)));
		} catch (IOException e) {
			logger.error("An error occurred when opening " + targetColumnsFile.getName() + " for output");
			throw new RuntimeException("");
		}
		logger.info("Writing to " + targetColumnsFile.getAbsolutePath());

		for(String parentRank : COLUMNRANKS){
			targetWriter.write(parentRank);
			targetWriter.write("\t");
		}
		targetWriter.write(lineSeparator);

		for(OBOClass term : tipList){
			Map<String,OBOClass> parentTable = getParents(term); 
			if (!parentTable.isEmpty()){
				for(String parentRank : COLUMNRANKS){
					OBOClass parentTerm = parentTable.get(parentRank);
					if (parentTerm != null){
						targetWriter.write(parentTerm.getName());
					}
					targetWriter.write("\t");
				}
				targetWriter.write(lineSeparator);
			}
		}
		targetWriter.close();
		logger.info("Done");
	}

	private List<OBOClass> getSpecies(){
		final List<OBOClass> result = new ArrayList<OBOClass>();
		Collection<OBOClass> source = u.getTerms();
		logger.info("Terms count is " + source.size());
		for (OBOClass term : source){
			if (!term.getPropertyValues().isEmpty()){
				if ("species".equals(u.getRankString(term))){				
						result.add(term);
				}
			}
		}
		return result;
	}

	
	private List<OBOClass> getTips(){
		final List<OBOClass> result = new ArrayList<OBOClass>();
		Collection<OBOClass> source = u.getTerms();
		logger.info("Terms count is " + source.size());
		for (OBOClass term : source){
			if (isTip(term)){
				if (!term.getPropertyValues().isEmpty()){
					String rankName = u.getRankString(term);
					if (!"species".equals(rankName)){
						logger.info("Tip is " + term.getName() + "; rank is " + rankName);
					}
				}
					
				result.add(term);
			}
		}
		return result;
	}
	
	private boolean isTip(OBOClass term){
		if (term.getChildren()!= null && !term.getChildren().isEmpty()){
			final OBOProperty isaProperty = u.getISAproperty();
			for (Link l : term.getChildren()){
				if (l.getType().equals(isaProperty)){
					return false;
				}
			}
			return false;
		}
		else
			return true;
	}


	private Map<String,OBOClass> getParents(OBOClass cl){
		final Map<String,OBOClass> result = new HashMap<String,OBOClass>();
		final OBOProperty isaProperty = u.getISAproperty();
		final List<String> columnNames = Arrays.asList(COLUMNRANKS);
		OBOClass curTerm = cl;
		if (!curTerm.getPropertyValues().isEmpty()){
			String rankName = u.getRankString(curTerm);
			if (rankName != null){
				if (columnNames.contains(rankName)){
					result.put(rankName, curTerm);
				}
			}
		}
		while (curTerm != null){
			if (curTerm.getParents() != null && !curTerm.getParents().isEmpty()){
				for (Link l : curTerm.getParents()){
					if (l.getType().equals(isaProperty)){
						OBOClass parent = (OBOClass)l.getParent();
						String rankName = u.getRankString(parent);
						if (rankName != null){
							if (columnNames.contains(rankName)){
								result.put(rankName, parent);
							}
						}
						curTerm = parent;
					}
				}
			}
			else{
				curTerm = null;
			}
		}
		return result;
	}
	
	private List<OBOClass> getAllParents(OBOClass cl){
		final List<OBOClass>result = new ArrayList<OBOClass>();
		final OBOProperty isaProperty = u.getISAproperty();
		OBOClass curTerm = cl;
		result.add(curTerm);
		while (curTerm != null){
			if (curTerm.getParents() != null && !curTerm.getParents().isEmpty()){
				for (Link l : curTerm.getParents()){
					if (l.getType().equals(isaProperty)){
						OBOClass parent = (OBOClass)l.getParent();
						result.add(parent);
						curTerm = parent;
					}
				}
			}
			else{
				curTerm = null;
			}
		}
		return result;
	}
	
	
	final private HashMap<String,String> allSynonyms = new HashMap<String,String>();    //Synonym -> taxonID
	final private Collection<String> homonymTable = new HashSet<String>();

	@Override
	public void saveSynonymFormat(String targetFilterPrefixStr) {
		File originalTargetFile = new File(targetFile);
		String targetName = originalTargetFile.getName();
		File targetSynonymsFile;
		if (targetName.contains(".")){
			int dotpos = targetName.indexOf('.');
			String dstName = originalTargetFile.getParent() + fileSep + targetName.substring(0,dotpos) + ".txt";
			targetSynonymsFile = new File(dstName);
		}
		else {
			targetSynonymsFile = new File(targetName + ".txt");
		}
		PrintWriter targetWriter = null;
		try {
			targetWriter = new PrintWriter(new BufferedWriter(new FileWriter(targetSynonymsFile)));
		} catch (IOException e) {
			logger.error("An error occurred when opening " + targetSynonymsFile.getName() + " for output");
			throw new RuntimeException("");
		}
		logger.info("Writing to " + targetSynonymsFile.getAbsolutePath());		

		for(String parentRank : SYNONYMCOLUMNS){
			targetWriter.write(parentRank);
			targetWriter.write("\t");
		}
		targetWriter.write(lineSeparator);
		
		List<OBOClass> tipList = getTips();
		logger.info("TipList contains " + tipList.size() + " terminal taxa");
		int tipCount = 0;
		for(OBOClass term : tipList){
			tipCount++;
        	Map<String,OBOClass> parentTable = getParents(term);        		
			Set<String> mySynonyms = new HashSet<String>();
			Map<String,String> tipSynonyms = u.getSynonyms(term);
			if (!term.getName().contains(":") && "species".equals(u.getRankString(term))){   //avoid writing tips that are not species
				for (String tipSyn : tipSynonyms.keySet()){
					if (!mySynonyms.contains(tipSyn)){
						if (allSynonyms.keySet().contains(tipSyn)){
							if (!term.getID().equals(allSynonyms.get(tipSyn))){
								homonymTable.add(tipSyn);
								mySynonyms.add(tipSyn);
							}
						}
						else{
							allSynonyms.put(tipSyn,term.getID());
							mySynonyms.add(tipSyn);
						}
					}
					targetWriter.write(tipSyn);
					targetWriter.write("\t");
					if (!parentTable.isEmpty()){
						if (parentTable.containsKey(SPECIESSTRING)) {
							String speciesStr = parentTable.get(SPECIESSTRING).getName();
							if (!speciesStr.contains(" ")){
								targetWriter.write(speciesStr);
								targetWriter.write("\t");
							}
							else {  //trinomials should not be here
								String[] components = speciesStr.split(" ");
								targetWriter.write(components[0]);
								targetWriter.write("\t");
								targetWriter.write(components[1]);
							}
							targetWriter.write("\t");
						}
						for(String parentRank : SYNONYMRANKS){
							OBOClass parentTerm = parentTable.get(parentRank);
							if (parentTerm != null){
								targetWriter.write(parentTerm.getName());
							}
						targetWriter.write("\t");
						}
					}
					else{
						targetWriter.write(term.getName());
						targetWriter.write("\t\t\t");
					}
					targetWriter.write(tipSynonyms.get(tipSyn));   //value is idstring
					targetWriter.write(lineSeparator);
				}
			}
		}
		targetWriter.close();
		logger.info("Detected " + homonymTable.size() + " homonyms");
//		for (String homonym : homonymTable){
//			System.out.println(homonym);
//		}
	}


	@Override
	public String getRankID (String rankName){
		return u.getRankID(rankName);
	}


	@Override
	public void setRankFromName(Term term, String rank) {
		u.setRankFromName(term.asOBOClass(), rank);
	}


	@Override
	public void setRankFromID(Term term, String rankID) {
		u.setRankFromID(term.asOBOClass(), rankID);
	}
	
	@Override
	public String getRankString(Term term){
		return u.getRankString(term.asOBOClass());
	}


	@Override
	public void addXRefToTerm(Term t, String dbName, String dbID) {
		Dbxref newRef = u.createDbxref(dbName, dbID, null, Dbxref.ANALOG);   //not sure this is exactly right, but the short-form constructors in DbxrefImpl suggest it works 
		t.asOBOClass().addDbxref(newRef);
	}


	@Override
	public Term getTermByXRef(String dbName, String dbID) {
		final OBOClass term = u.lookupTermByXRef(dbName,dbID);
		if (term != null)
			return new OBOTerm(term);
		else
			return null;
	}
	
	
	@Override
	public List<String> countTerms(){
		return u.countTerms();
	}

	public void processObsoletes(){
		for(String oldName : trimmedTaxonNameAsSynonym){
			OBOClass oldTerm = trimmedNames.get(oldName);
			if (u.lookupTermByID(oldTerm.getID()) == null){
				OBOClass addedClass = u.makeTerm(oldTerm.getID(), oldName);
				addedClass.setObsolete(true);
				List<OBOClass> considerTerms = findSynonymOccurances(oldName);
				for(OBOClass candidate : considerTerms){
					addedClass.addConsiderReplacement(candidate);					
				}
			}
		}
		trimmedTaxonNameAsSynonym.clear();
	}

	
	//sadly, brute force since the current interface doesn't capture the term with the matching synonym
	private List<OBOClass> findSynonymOccurances(String oldName){
		final List<OBOClass> result = new ArrayList<OBOClass>();
		if (oldName == null){ //should never happen, but harmless here
			return result;
		}
		for (OBOClass c : u.getTerms()){
			Set <Synonym> synonyms = c.getSynonyms();
			for (Synonym s : synonyms){
				if (oldName.equals(s.getText())){
					result.add(c);
					break;
				}
			}
		}
		return result;
	}

	
	@Override
	public void saveAllColumnFormat(String targetFilterPrefixStr) {
		File originalTargetFile = new File(targetFile);
		String targetName = originalTargetFile.getName();
		File targetAllColumnsFile;
		if (targetName.contains(".")){
			int dotpos = targetName.indexOf('.');
			String dstName = originalTargetFile.getParent() + fileSep + targetName.substring(0,dotpos) + ".txt";
			targetAllColumnsFile = new File(dstName);
		}
		else {
			targetAllColumnsFile = new File(targetName + ".txt");
		}
		PrintWriter targetWriter = null;
		try {
			targetWriter = new PrintWriter(new BufferedWriter(new FileWriter(targetAllColumnsFile)));
		} catch (IOException e) {
			logger.error("An error occurred when opening " + targetAllColumnsFile.getName() + " for output");
			throw new RuntimeException("");
		}
		logger.info("Writing to " + targetAllColumnsFile.getAbsolutePath());		
		
		List<OBOClass> tipList = getTips();
		logger.info("TipList contains " + tipList.size() + " terminal taxa");
		int tipCount = 0;
		for(OBOClass term : tipList){
			tipCount++;
        	List<OBOClass> parents = getAllParents(term);        		
			for (OBOClass parent: parents){  //parent list includes the term itself...
				targetWriter.write(parent.getName());
				targetWriter.write("\t");
			}
			targetWriter.write(lineSeparator);
		}
		targetWriter.close();
		logger.info("Detected " + homonymTable.size() + " homonyms");
//		for (String homonym : homonymTable){
//			System.out.println(homonym);
//		}
	}



	@Override
	public void setExtinct(Term term) {
		u.setExtinct(term.asOBOClass());		
	}

	@Override
	public void resetExtinct(Term term) {
		u.resetExtinct(term.asOBOClass());
	}


}
