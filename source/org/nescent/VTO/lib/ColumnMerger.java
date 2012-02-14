package org.nescent.VTO.lib;

import java.io.File;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

public class ColumnMerger implements Merger,ColumnFormat {

	private final static String INCERTAESEDIS = "Incertae sedis";
	private final String columnSeparator; 
	private final ColumnReader reader;

	private Map<Integer,String> synPrefixMap;

	private File source;
	private TaxonStore target;
	
	private SynonymSource preserveSynonyms;

	static Logger logger = Logger.getLogger(ColumnMerger.class.getName());

	/* Metadata methods */
	@Override
	public boolean canAttach() {
		return true;
	}

	// returning false, though some cases might work
	@Override
	public boolean canPreserveID() {
		return false;
	}

	@Override
	public void setPreserveID(boolean v){
		if (v)
			throw new IllegalArgumentException("This merger can't preserve IDs because column format does not support ids");
	}
	
	@Override
	public void setPreserveSynonyms(SynonymSource s){
		preserveSynonyms = s;
	}


	public ColumnMerger(String separator){
		columnSeparator = separator;
		reader = new ColumnReader(columnSeparator);
	}


	@Override
	public void setSource(File sourceFile){
		source = sourceFile;
	}

	@Override
	public void setTarget(TaxonStore targetStore){
		target = targetStore;
	}


	@Override
	public void setColumns(List<String> columns, Map<Integer,String> synPrefixes) {
		reader.setColumns(columns,synPrefixes);  // and what else?
		synPrefixMap = synPrefixes;
	}

	@Override
	public void merge(String prefix) {
		ItemList items = reader.processCatalog(source, true);
		for(Item item : items.getContents()){

		}
		// TODO finish

	}

	@Override
	public void attach(String attachment, String cladeRoot, String prefix) {
		ItemList items = reader.processCatalog(source, true);
		Term attachTerm = null;
		if (!"".equals(attachment)){
			attachTerm = target.getTermbyName(attachment);
			if (attachTerm == null){   //parent is unknown
				if (!target.isEmpty()){
					System.err.println("Can not attach " + source.getAbsolutePath() + " specified parent: " + attachment + " is unknown to " + target);
					return;
				}
				else { // attachment will be added first to provide a root for an otherwise empty target
					attachTerm = target.addTerm(attachment);
					logger.info("Assigning " + attachment + " as root");
				}
			}
		}
		if (items.hasColumn(KnownField.CLASS)){
			processClassColumn(items, attachTerm);
		}
		if (items.hasColumn(KnownField.ORDER)){
			processOrderColumn(items, attachTerm);
		}
		if (items.hasColumn(KnownField.FAMILY)){
			processFamilyColumn(items, attachTerm);
		}
		if (items.hasColumn(KnownField.SUBFAMILY)){
			processSubFamilyColumn(items, attachTerm);
		}	
		if (items.hasColumn(KnownField.GENUS)){
			processGenusColumn(items,attachTerm);
		}	
		if (items.hasColumn(KnownField.SPECIES)){
			processSpeciesColumn(items,attachTerm);
		}
		logger.info("Checkpoint 5: " + target.getTermbyName("Allophrynidae").getID());
	}

	private void processClassColumn(ItemList items, Term attachTerm){
		for (final Item it : items.getContents()){
			final String className = it.getName(KnownField.CLASS);
			if (className.length()>0 && !INCERTAESEDIS.equalsIgnoreCase(className)){
				Term classTerm = target.getTermbyName(className);
				if (classTerm == null){
					classTerm = target.addTerm(className);
					target.setRankFromName(classTerm,KnownField.CLASS.getCannonicalName());   //string from knownColumn?
					if (attachTerm != null)  // this is weak, but allows construction of an ontology with multiple roots (so obviously wrong)
						target.attachParent(classTerm, attachTerm);
				}
			}
		}
	}

	private void processOrderColumn(ItemList items, Term attachTerm){
		for (final Item it : items.getContents()){
			final String orderName = stripDagger(it.getName(KnownField.ORDER));
			final boolean isExtinct = daggerPrefix(it.getName(KnownField.FAMILY));
			if (orderName.length()>0 && !INCERTAESEDIS.equalsIgnoreCase(orderName)){
				Term orderTerm = target.getTermbyName(orderName);
				if (orderTerm == null){
					orderTerm = target.addTerm(orderName);
					target.setRankFromName(orderTerm,KnownField.ORDER.getCannonicalName());
					if (it.hasColumn(KnownField.CLASS) && target.getTermbyName(it.getName(KnownField.CLASS)) != null){
						final String parentName = it.getName(KnownField.CLASS);
						target.attachParent(orderTerm,target.getTermbyName(parentName));
					}
					else if (attachTerm != null)
						target.attachParent(orderTerm, attachTerm);
					if (isExtinct){
						target.setExtinct(orderTerm);
					}
				}
			}
		}		
	}


	private void processFamilyColumn(ItemList items, Term attachTerm){
		for (final Item it : items.getContents()){
			final String familyName = stripDagger(it.getName(KnownField.FAMILY));
			final boolean isExtinct = daggerPrefix(it.getName(KnownField.FAMILY));
			if (familyName.length()>0 && !INCERTAESEDIS.equalsIgnoreCase(familyName)){
				if (familyName != null){
					if (target.getTermbyName(familyName) == null){
						final Term familyTerm = target.addTerm(familyName);
						logger.info("adding family " + familyName + " id is " + familyTerm.getID());
						target.setRankFromName(familyTerm,KnownField.FAMILY.getCannonicalName());
						if (it.hasColumn(KnownField.ORDER) && target.getTermbyName(it.getName(KnownField.ORDER)) != null){
							final String parentName = it.getName(KnownField.ORDER);
							target.attachParent(familyTerm,target.getTermbyName(parentName));
						}
						else if (attachTerm != null)
							target.attachParent(familyTerm, attachTerm);
						if (isExtinct){
							target.setExtinct(familyTerm);
						}
					}
				}
			}
		}		
	}


	private void processSubFamilyColumn(ItemList items, Term attachTerm){
		for (Item it : items.getContents()){
			final String subFamilyName = stripDagger(it.getName(KnownField.SUBFAMILY));
			final boolean isExtinct = daggerPrefix(it.getName(KnownField.SUBFAMILY));
			if (subFamilyName.length()>0 && !INCERTAESEDIS.equalsIgnoreCase(subFamilyName)){
				if (target.getTermbyName(subFamilyName) == null){
					final Term subFamilyTerm = target.addTerm(subFamilyName);
					target.setRankFromName(subFamilyTerm, KnownField.SUBFAMILY.getCannonicalName());
					if (it.hasColumn(KnownField.FAMILY) && target.getTermbyName(it.getName(KnownField.FAMILY)) != null){
						final String parentName = it.getName(KnownField.FAMILY);
						target.attachParent(subFamilyTerm,target.getTermbyName(parentName));
					}
					else if (it.hasColumn(KnownField.ORDER) && target.getTermbyName(it.getName(KnownField.ORDER)) != null){  //this is weird, but not inconceivable
						final String parentName = it.getName(KnownField.ORDER);
						target.attachParent(subFamilyTerm,target.getTermbyName(parentName));
					}
					else if (attachTerm != null)
						target.attachParent(subFamilyTerm, attachTerm);
					if (isExtinct){
						target.setExtinct(subFamilyTerm);
					}
				}
			}
		}
	}

	private void processGenusColumn(ItemList items, Term attachTerm){
		for (final Item it : items.getContents()){
			final String genusName = stripDagger(it.getName(KnownField.GENUS));
			final boolean isExtinct = daggerPrefix(it.getName(KnownField.GENUS));
			if (genusName.length()>0 && !INCERTAESEDIS.equalsIgnoreCase(genusName)){
				if (genusName != null && target.getTermbyName(genusName) == null){
					final Term genusTerm = target.addTerm(genusName);
					target.setRankFromName(genusTerm, KnownField.GENUS.getCannonicalName());
					if (it.hasColumn(KnownField.SUBFAMILY) && target.getTermbyName(it.getName(KnownField.SUBFAMILY)) != null){
						final String parentName = it.getName(KnownField.SUBFAMILY);
						target.attachParent(genusTerm,target.getTermbyName(parentName));
					}
					else if (it.hasColumn(KnownField.FAMILY) && target.getTermbyName(it.getName(KnownField.FAMILY)) != null){
						final String parentName = it.getName(KnownField.FAMILY);
						target.attachParent(genusTerm,target.getTermbyName(parentName));						
					}
					else if (it.hasColumn(KnownField.ORDER) && target.getTermbyName(it.getName(KnownField.ORDER)) != null){ 
						final String parentName = it.getName(KnownField.ORDER);
						target.attachParent(genusTerm,target.getTermbyName(parentName));
					}
					else if (attachTerm != null)
						target.attachParent(genusTerm, attachTerm);
					if (isExtinct){
						target.setExtinct(genusTerm);
					}
				}
			}
		}
	}


	private void processSpeciesColumn(ItemList items, Term attachTerm){
		for (final Item it : items.getContents()){
			if (it.getName(KnownField.SPECIES) != null){
				if (it.hasColumn(KnownField.GENUS) && target.getTermbyName(it.getName(KnownField.GENUS)) != null){
					final String parentName = stripDagger(it.getName(KnownField.GENUS));
					final boolean isExtinct = daggerPrefix(it.getName(KnownField.SPECIES));
					final String childName = stripDagger(it.getName(KnownField.SPECIES));
					final String speciesName = parentName + " " + childName;
					Term speciesTerm;
					if (target.getTermbyName(speciesName) == null){
						speciesTerm = target.addTerm(speciesName);
						target.setRankFromName(speciesTerm,KnownField.SPECIES.getCannonicalName());
						target.attachParent(speciesTerm,target.getTermbyName(parentName));
					}
					else{
						speciesTerm = target.getTermbyName(speciesName);  //already exists  - so just update xrefs and synonyms (? not sure about this - might be a homonymy)
					}
					if (isExtinct){
						target.setExtinct(speciesTerm);
					}
					if (items.hasColumn(KnownField.XREF)){
						//TODO make sure xrefs are handled
					}
					addSpeciesSynonyms(it,speciesTerm);
				}
			}
		}
	}


	private void addSpeciesSynonyms(Item it, Term speciesTerm){
		for (final String synSource : it.getSynonymSources()){
			final String[] sourceComps = synSource.split(":",2);
			for(String syn : it.getSynonymsForSource(synSource))
				if (true) { //!syn.equals(speciesName)){
					SynonymI s = target.makeSynonymWithXref(syn, sourceComps[0], sourceComps[1]);
					speciesTerm.addSynonym(s);
				}
		}

	}
	
	
	private boolean daggerPrefix(String name){
		return (name.length()>0 && name.charAt(0) == ' ');
	}
	
	private String stripDagger(String name){
		if (daggerPrefix(name))
			return name.substring(1);
		else
			return name;
	}
}
