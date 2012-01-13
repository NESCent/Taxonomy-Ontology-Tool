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

	private final OBOUtils u;

	private int idCounter = 0;
	private String idSuffix = ":%07d";
	private String defaultFormat;

	/**
	 * 
	 * @param fileSpec
	 * @param prefix
	 * @param oboNameSpace
	 */
	public OBOStore(String fileSpec, String prefix, String oboNameSpace) {
		u = new OBOUtils();
		u.setNameSpace(oboNameSpace, fileSpec);
		defaultPrefix = prefix;
		defaultFormat = defaultPrefix + idSuffix;		
		targetFile = fileSpec;
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
	public void updateIDGenerator(String prefix){
		int maxCounter = 0;
		for (OBOClass c : u.getTerms()){
			final String id = c.getID();
			if (prefix != null && id.startsWith(prefix)){
				int colonIndex = id.indexOf(':');
				String suffix = id.substring(colonIndex);
				try{
					int index = Integer.parseInt(suffix);
					if (index > maxCounter)
						maxCounter = index;
				}
				catch (NumberFormatException e){
				//this really is safe to silently ignore...
				}
			}
		}
		idCounter = maxCounter+1;
	}
	
	@Override
	public Term addTerm(String name) {
		// need to generate the ID
		String newID = String.format(defaultFormat,idCounter++);
		return new OBOTerm(u.makeTerm(newID, name));
	}


	@Override
	public Term addTermbyID(String ID, String name) {
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


	@Override
	public void saveStore() {
		u.saveOBOSession(targetFile);
	}



	@Override
	public Term getTermbyName(String termName) {
		final OBOClass term = u.lookupTermByName(termName);
		if (term != null)
			return new OBOTerm(term);
		else
			return null;
	}

	public boolean hasTermbyName(String termName){
		return (u.lookupTermByName(termName) != null);
	}

	
	public SynonymI makeSynonym(String synString){
		Synonym s = u.makeSynonym(synString);
		return new OBOSynonym(s);
	}

	public SynonymI makeSynonymWithXref(String synString, String dbxprefix, String entryID ){
		final Synonym s = u.makeSynonymWithXref(synString, dbxprefix, entryID);
		return new OBOSynonym(s);
	}

	
	@Override
	public void trim(String targetNode){
		final OBOClass cladeRoot = u.lookupTermByName(targetNode);
		if (cladeRoot == null){
			logger.error("Clade root to trim " + targetNode + " not found");
		}
		else{
			removeClade(cladeRoot);
		}
	}
	
	
	private void removeClade(OBOClass clRoot){
		if (isTip(clRoot)){
			u.removeNode(clRoot);
		}
		else {
			Set<Link> links = new HashSet<Link>();
			links.addAll(clRoot.getChildren());
			for (Link l : links){
				OBOClass childNode = (OBOClass) l.getChild();
				clRoot.removeChild(l);
				childNode.removeParent(l);
				removeClade(childNode);
			}
			u.removeNode(clRoot);
		}
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
			for (Link l : term.getChildren()){
				if (l.getType().equals(u.getISAproperty())){
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
		OBOClass curTerm = cl;
		if (!curTerm.getPropertyValues().isEmpty()){
			String rankName = u.getRankString(curTerm);
			if (rankName != null){
				if (Arrays.asList(COLUMNRANKS).contains(rankName)){
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
							if (Arrays.asList(COLUMNRANKS).contains(rankName)){
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
    		//the following is included to support matching applications that want ranks filled in
    		//Ideally these should be specified in the XML configuration 
    		//TODO add syntax to the XML configuration to do this
    		addIntermediateRanks(term,parentTable);
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
							String[] components = speciesStr.split(" ");
							if (components.length == 1){
								targetWriter.write(speciesStr);
								targetWriter.write("\t");
							}
							else if (components.length>1){
								targetWriter.write(components[0]);
								targetWriter.write("\t");
								targetWriter.write(components[1]);
							}
							else {
								throw new RuntimeException("String '" + speciesStr + "' could not be split, something's wrong");
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

	

	//the following is included to support matching applications that want ranks filled in
	//Ideally these should be specified in the XML configuration 
	//TODO add syntax to the XML configuration to do this

	private OBOClass reptiliaFiller = null;
	private OBOClass crocodiliaFiller = null; 
	private OBOClass artiodactylaFiller = null;
	private OBOClass afrosoricidaFiller = null;
	private OBOClass chondrichthyesFiller = null;
	private OBOClass sarcopterygiiFiller = null;
	private OBOClass cephalaspidomorphiFiller = null;
	
	private void addIntermediateRanks(OBOClass term, Map<String,OBOClass> parentTable){
		if (reptiliaFiller == null){
			reptiliaFiller = u.makeTerm("TEMP:0000001","Reptilia");
		}
		if (crocodiliaFiller == null){
			crocodiliaFiller = u.makeTerm("TEMP:0000002","Crocodilia");
		}
		if (artiodactylaFiller == null){
			artiodactylaFiller = u.makeTerm("TEMP:0000003","Artiodactyla");
		}
		if (afrosoricidaFiller == null){
			afrosoricidaFiller = u.makeTerm("TEMP:0000004", "Afrosoricida");
		}
		if (chondrichthyesFiller == null){
			chondrichthyesFiller = u.lookupTermByName("Chondrichthyes");
		}
		if (sarcopterygiiFiller == null){
			sarcopterygiiFiller = u.lookupTermByName("Sarcopterygii");
		}
		if (cephalaspidomorphiFiller == null){
			cephalaspidomorphiFiller = u.makeTerm("TEMP:0000005", "Cephalaspidomorphi");
		}
		
		final OBOClass orderClass = parentTable.get("order");
		final OBOClass familyClass = parentTable.get("family");
		if (orderClass != null && "Sphenodontia".equals(orderClass.getName())){
			parentTable.put("class", reptiliaFiller);
		}
		if (orderClass != null && "Squamata".equals(orderClass.getName())){
			parentTable.put("class", reptiliaFiller);
		}
		if (orderClass != null && "Testudines".equals(orderClass.getName())){
			parentTable.put("class", reptiliaFiller);
		}
		if (familyClass != null && "Crocodylidae".equals(familyClass.getName())){
			parentTable.put("class", reptiliaFiller);
			parentTable.put("order", crocodiliaFiller);
		}
		if (familyClass != null && "Bovidae".equals(familyClass.getName())){
			parentTable.put("order", artiodactylaFiller);
		}
		if (familyClass != null && "Camelidae".equals(familyClass.getName())){
			parentTable.put("order", artiodactylaFiller);
		}
		if (familyClass != null && "Giraffidae".equals(familyClass.getName())){
			parentTable.put("order", artiodactylaFiller);
		}
		if (familyClass != null && "Cervidae".equals(familyClass.getName())){
			parentTable.put("order", artiodactylaFiller);
		}
		if (familyClass != null && "Antilocapridae".equals(familyClass.getName())){
			parentTable.put("order", artiodactylaFiller);
		}
		if (familyClass != null && "Moschidae".equals(familyClass.getName())){
			parentTable.put("order", artiodactylaFiller);
		}
		if (familyClass != null && "Tragulidae".equals(familyClass.getName())){
			parentTable.put("order", artiodactylaFiller);
		}
		if (familyClass != null && "Suidae".equals(familyClass.getName())){
			parentTable.put("order", artiodactylaFiller);
		}
		if (familyClass != null && "Tayassuidae".equals(familyClass.getName())){
			parentTable.put("order", artiodactylaFiller);
		}
		if (familyClass != null && "Hippopotamidae".equals(familyClass.getName())){
			parentTable.put("order", artiodactylaFiller);
		}
		if (familyClass != null && "Tenrecidae".equals(familyClass.getName())){
			parentTable.put("order", afrosoricidaFiller);
		}
		if (familyClass != null && "Chrysochloridae".equals(familyClass.getName())){
			parentTable.put("order", afrosoricidaFiller);
		}
		if (orderClass != null && "Carcharhiniformes".equals(orderClass.getName())){
			parentTable.put("class", chondrichthyesFiller);
		}
		if (orderClass != null && "Chimaeriformes".equals(orderClass.getName())){
			parentTable.put("class", chondrichthyesFiller);
		}
		if (orderClass != null && "Heterodontiformes".equals(orderClass.getName())){
			parentTable.put("class", chondrichthyesFiller);
		}
		if (orderClass != null && "Hexanchiformes".equals(orderClass.getName())){
			parentTable.put("class", chondrichthyesFiller);
		}
		if (orderClass != null && "Lamniformes".equals(orderClass.getName())){
			parentTable.put("class", chondrichthyesFiller);
		}
		if (orderClass != null && "Pristiformes".equals(orderClass.getName())){
			parentTable.put("class", chondrichthyesFiller);
		}
		if (orderClass != null && "Pristiophoriformes".equals(orderClass.getName())){
			parentTable.put("class", chondrichthyesFiller);
		}
		if (orderClass != null && "Rajiformes".equals(orderClass.getName())){
			parentTable.put("class", chondrichthyesFiller);
		}
		if (orderClass != null && "Orectolobiformes".equals(orderClass.getName())){
			parentTable.put("class", chondrichthyesFiller);
		}
		if (orderClass != null && "Squaliformes".equals(orderClass.getName())){
			parentTable.put("class", chondrichthyesFiller);
		}
		if (orderClass != null && "Squatiniformes".equals(orderClass.getName())){
			parentTable.put("class", chondrichthyesFiller);
		}
		if (orderClass != null && "Torpediniformes".equals(orderClass.getName())){
			parentTable.put("class", chondrichthyesFiller);
		}

		if (orderClass != null && "Ceratodontiformes".equals(orderClass.getName())){
			parentTable.put("class", sarcopterygiiFiller);
		}
		if (orderClass != null && "Coelacanthiformes".equals(orderClass.getName())){
			parentTable.put("class", sarcopterygiiFiller);
		}
		if (orderClass != null && "Lepidosireniformes".equals(orderClass.getName())){
			parentTable.put("class", sarcopterygiiFiller);
		}
		if (orderClass != null && "Petromyzontiformes".equals(orderClass.getName())){
			parentTable.put("class", cephalaspidomorphiFiller);
		}
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


}
