package org.nescent.VTO.lib;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.nescent.VTO.Builder;
import org.nescent.VTO.lib.ITISMerger.ITISElement;
import org.obo.datamodel.Synonym;

public class ColumnMerger implements Merger,ColumnFormat {

	private final static String INCERTAESEDIS = "Incertae sedis";
	private final String columnSeparator; 
	private final ColumnReader reader;

	private String subAction = Builder.SYNSUBACTION;  // default; currently only implemented behaviors are to attach or to merge synonyms;
	private File source;
	private TaxonStore target;
	private String uriTemplate;

	private boolean updateObsoletes = false;
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
	
	@Override
	public void setUpdateObsoletes(boolean v){
		updateObsoletes = v;
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
	public void setColumns(List<ColumnType> columns) {
		reader.setColumns(columns);  // and what else?
	}

	/**
	 * @param sa specifies whether this merges synonyms or cross references
	 */
	@Override
	public void setSubAction(String sa){
		subAction = sa;
	}


	@Override
	public void setURITemplate(String template) {
		uriTemplate = template;

	}

	@Override
	public void merge(String prefix) {
		ItemList items = reader.processCatalog(source, true);
		if (Builder.SYNSUBACTION.equals(subAction)){
			mergeSynonyms(items);
		}
		else {
			mergeXrefs(items);
		}
	}


	private void mergeSynonyms(ItemList items){
		for(Item item : items.getContents()){

		}
	}

	private void mergeXrefs(ItemList items){
		int termCount = 0;
		final ColumnType c = reader.getColumn(KnownField.SPECIES);
		if (c == null)
			return;
		for(Item item : items.getContents()){

			final String genus = item.getFieldValue(KnownField.GENUS);
			String species = item.getFieldValue(KnownField.SPECIES);
			if (species.indexOf(' ') != -1){
				species = species.substring(0,species.indexOf(' '));
			}
			final Term matchingTerm = target.getTermbyName(genus + " " + species);
			if (matchingTerm != null){
				termCount++;
				for(String xref : item.getTermXRefs()) {
					logger.info("xref = " + xref );
					int colonpos = xref.indexOf(':');  //TODO remove this - the split echoes the OBO library interface, but not necessary here
					target.addXRefToTerm(matchingTerm, xref.substring(0,colonpos), xref.substring(colonpos+1));
				}
			}
		}

	}


	/**
	 * @param targetParentName name of parent node for attached clade
	 * @param sourceRootName name of root node (child of parent) for attached clade
	 * @param prefix prefix for newly generated terms
	 */
	@Override
	public void attach(String targetParentName, String sourceRootName, String prefix) {
		ItemList items = reader.processCatalog(source, true);
		Term attachTerm = null;
		if (!"".equals(targetParentName)){
			attachTerm = target.getTermbyName(targetParentName);
			if (attachTerm == null){   //parent is unknown
				if (!target.isEmpty()){
					System.err.println("Can not attach " + source.getAbsolutePath() + " specified parent: " + targetParentName + " is unknown to " + target);
					return;
				}
				else { // attachment will be added first to provide a root for an otherwise empty target
					attachTerm = target.addTerm(targetParentName,prefix);
					logger.info("Assigning " + targetParentName + " as root");
				}
			}
		}
		//does a "breadth first" traversal of the columns so smaller id values are assigned to higher level taxa
		if (items.hasColumn(KnownField.CLASS)){
			processClassColumn(items, attachTerm, prefix);
		}
		if (items.hasColumn(KnownField.ORDER)){
			processOrderColumn(items, attachTerm, prefix);
		}
		if (items.hasColumn(KnownField.FAMILY)){
			processFamilyColumn(items, attachTerm, prefix);
		}
		if (items.hasColumn(KnownField.SUBFAMILY)){
			processSubFamilyColumn(items, attachTerm, prefix);
		}	
		if (items.hasColumn(KnownField.GENUS)){
			processGenusColumn(items,attachTerm, prefix);
		}	
		if (items.hasColumn(KnownField.SPECIES)){
			processSpeciesColumn(items,attachTerm, prefix);
		}
		else if (items.hasColumn(KnownField.DELIMITEDNAME)){
		}	
		if (updateObsoletes){
			target.processObsoletes();
		}
	}

	private void processClassColumn(ItemList items, Term attachTerm, String prefix){
		for (final Item it : items.getContents()){
			final String className = it.getName(KnownField.CLASS);
			if (className.length()>0 && !INCERTAESEDIS.equalsIgnoreCase(className)){
				Term classTerm = target.getTermbyName(className);
				if (classTerm == null){
					classTerm = target.addTerm(className, prefix);
					target.setRankFromName(classTerm,KnownField.CLASS.getCannonicalName());   //string from knownColumn?
					if (attachTerm != null)  // this is weak, but allows construction of an ontology with multiple roots (so obviously wrong)
						target.attachParent(classTerm, attachTerm);
				}
			}
		}
	}

	private void processOrderColumn(ItemList items, Term attachTerm, String prefix){
		for (final Item it : items.getContents()){
			final String orderField = it.getName(KnownField.ORDER);
			if (orderField == null)
				throw new RuntimeException("Empty order field in line: " + it);
			final String orderName = stripDagger(it.getName(KnownField.ORDER));
			final boolean isExtinct = daggerPrefix(it.getName(KnownField.ORDER));
			if (orderName.length()>0 && !INCERTAESEDIS.equalsIgnoreCase(orderName)){
				Term orderTerm = target.getTermbyName(orderName);
				if (orderTerm == null){
					orderTerm = target.addTerm(orderName, prefix);
					target.setRankFromName(orderTerm,KnownField.ORDER.getCannonicalName());
					if (it.hasColumn(KnownField.CLASS) && target.getTermbyName(stripDagger(it.getName(KnownField.CLASS))) != null){
						final String parentName = stripDagger(it.getName(KnownField.CLASS));
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


	private void processFamilyColumn(ItemList items, Term attachTerm, String prefix){
		for (final Item it : items.getContents()){
			final String familyName = stripDagger(it.getName(KnownField.FAMILY));
			final boolean isExtinct = daggerPrefix(it.getName(KnownField.FAMILY));
			if (familyName.length()>0 && !INCERTAESEDIS.equalsIgnoreCase(familyName)){
				if (familyName != null){
					if (target.getTermbyName(familyName) == null){
						final Term familyTerm = target.addTerm(familyName, prefix);
						target.setRankFromName(familyTerm,KnownField.FAMILY.getCannonicalName());
						if (it.hasColumn(KnownField.ORDER) && target.getTermbyName(stripDagger(it.getName(KnownField.ORDER))) != null){
							final String parentName = stripDagger(it.getName(KnownField.ORDER));
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


	private void processSubFamilyColumn(ItemList items, Term attachTerm, String prefix){
		for (Item it : items.getContents()){
			final String subFamilyName = stripDagger(it.getName(KnownField.SUBFAMILY));
			final boolean isExtinct = daggerPrefix(it.getName(KnownField.SUBFAMILY));
			if (subFamilyName.length()>0 && !INCERTAESEDIS.equalsIgnoreCase(subFamilyName)){
				if (target.getTermbyName(subFamilyName) == null){
					final Term subFamilyTerm = target.addTerm(subFamilyName, prefix);
					target.setRankFromName(subFamilyTerm, KnownField.SUBFAMILY.getCannonicalName());
					if (it.hasColumn(KnownField.FAMILY) && target.getTermbyName(stripDagger(it.getName(KnownField.FAMILY))) != null){
						final String parentName = stripDagger(it.getName(KnownField.FAMILY));
						target.attachParent(subFamilyTerm,target.getTermbyName(parentName));
					}
					else if (it.hasColumn(KnownField.ORDER) && target.getTermbyName(stripDagger(it.getName(KnownField.ORDER))) != null){  //this is weird, but not inconceivable
						final String parentName = stripDagger(it.getName(KnownField.ORDER));
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

	private void processGenusColumn(ItemList items, Term attachTerm, String prefix){
		for (final Item it : items.getContents()){
			final String genusName = stripDagger(it.getName(KnownField.GENUS));
			final boolean isExtinct = daggerPrefix(it.getName(KnownField.GENUS));
			if (genusName.length()>0 && !INCERTAESEDIS.equalsIgnoreCase(genusName)){
				if (genusName != null && target.getTermbyName(genusName) == null){
					final Term genusTerm = target.addTerm(genusName, prefix);
					target.setRankFromName(genusTerm, KnownField.GENUS.getCannonicalName());
					if (it.hasColumn(KnownField.SUBFAMILY) && target.getTermbyName(stripDagger(it.getName(KnownField.SUBFAMILY))) != null){
						final String parentName = stripDagger(it.getName(KnownField.SUBFAMILY));
						target.attachParent(genusTerm,target.getTermbyName(parentName));
					}
					else if (it.hasColumn(KnownField.FAMILY) && target.getTermbyName(stripDagger(it.getName(KnownField.FAMILY))) != null){
						final String parentName = stripDagger(it.getName(KnownField.FAMILY));
						target.attachParent(genusTerm,target.getTermbyName(parentName));						
					}
					else if (it.hasColumn(KnownField.ORDER) && target.getTermbyName(stripDagger(it.getName(KnownField.ORDER))) != null){ 
						final String parentName = stripDagger(it.getName(KnownField.ORDER));
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


	private void processSpeciesColumn(ItemList items, Term attachTerm, String prefix){
		for (final Item it : items.getContents()){
			if (it.getName(KnownField.SPECIES) != null){
				if (it.hasColumn(KnownField.GENUS) && target.getTermbyName(stripDagger(it.getName(KnownField.GENUS))) != null){
					final String parentName = stripDagger(it.getName(KnownField.GENUS));
					final boolean isExtinct = daggerPrefix(it.getName(KnownField.SPECIES));
					final String childName = stripDagger(it.getName(KnownField.SPECIES));
					final String speciesName = parentName + " " + childName;
					Term speciesTerm;
					if (target.getTermbyName(speciesName) == null){
						speciesTerm = target.addTerm(speciesName, prefix);
						target.setRankFromName(speciesTerm,KnownField.SPECIES.getCannonicalName());
						target.attachParent(speciesTerm,target.getTermbyName(parentName));
					}
					else{
						speciesTerm = target.getTermbyName(speciesName);  //already exists  - so just update xrefs and synonyms (? not sure about this - might be a homonymy)
					}
					if (isExtinct){
						target.setExtinct(speciesTerm);
					}
					decorateSpeciesTerm(it,speciesTerm);
				}
			}
		}
	}


	private void decorateSpeciesTerm(Item it, Term speciesTerm){
		for (String xRef : it.getSynonym_xrefs()){
			String[] components = xRef.split(":");
			if (components.length == 2){
				Collection <String> synonymsForXRef = it.getSynonymsFromSource(xRef);
				for (String syn : synonymsForXRef){
					SynonymI newSyn = target.makeSynonymWithXref(syn, components[0], components[1]);	
					speciesTerm.addSynonym(newSyn);
				}
			}
			else {
				throw new RuntimeException("While processing term " + speciesTerm.getLabel() + " encountered a mal-formed xref: " + xRef + " associated with synonyms");
			}
		}
		for (String syn : it.getPlainSynonyms()){
			speciesTerm.addSynonym(target.makeSynonym(syn));
		}
		for (String commonName : it.getVernacularNames()){
			speciesTerm.addSynonym(target.makeCommonName(commonName));   //TODO should be tagging these with appropriate synonym type
		}

		if (it.getComment() != null){
			speciesTerm.setComment(it.getComment());
		}
		if (it.getFieldValue(KnownField.STATUS) != null){
		}
		for (String xref : it.getTermXRefs()){
			String[] components = xref.split(":");
			if (components.length == 2){
				target.addXRefToTerm(speciesTerm,components[0],components[1]);
			}
			else {
				throw new RuntimeException("While processing term " + speciesTerm.getLabel() + " encountered a mal-formed xref: " + xref);
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
