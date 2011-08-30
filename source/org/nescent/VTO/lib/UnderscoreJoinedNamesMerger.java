package org.nescent.VTO.lib;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

/**
 * This merger handles files with names joined by underscore in one column (e.g., Homo_sapiens or Homo_sapiens_sapiens).  It currently
 * looks for a single column tagged with the KnownField DELIMITEDNAME and treats it as columns of genus, species and optionally subspecies.  
 * Apart from the absent higher levels, it should work as a merger.
 * 
 * @author pmidford
 *
 */


public class UnderscoreJoinedNamesMerger implements Merger, ColumnFormat {

	private final String columnSeparator; 

	private final String nameSeparator = "_";

	private final ColumnReader reader;

	private int namesCounter = 0;

	private Set<Item> resolvedItems = new HashSet<Item>();
	
	private File source;
	private TaxonStore target;

	static Logger logger = Logger.getLogger(OBOStore.class.getName());


	public UnderscoreJoinedNamesMerger(String separator){
		columnSeparator = separator;
		reader = new ColumnReader(columnSeparator);
	}

	/* Metadata methods */
	@Override
	/**
	 * In theory this could contain the set of taxa within a family, but that's a stretch at this point.
	 */
	public boolean canAttach() {
		return false;
	}

	@Override
	public boolean canPreserveID(){
		return false;
	}
	
	
	@Override
	public void setSource(File fileSource){
		source = fileSource;
	}
	
	@Override
	public void setTarget(TaxonStore taxonTarget){
		target = taxonTarget;
	}
	
	@Override
	public void setColumns(List<String> columns,Map<Integer, String> synPrefixes) {
		reader.setColumns(columns,synPrefixes);  // and what else?
		// at the moment, this reader can ignore synonym columns
	}

	@Override
	public void merge(String prefix) {
		int matchCount = 0;
		ItemList items = reader.processCatalog(source, true);
		if (!items.hasColumn(KnownField.DELIMITEDNAME)){
			throw new RuntimeException("No delimitedname column specified for joined name formatted file");
		}
		for(Item item : items.getContents()){
			String genusName = null;
			String speciesName = null;
			String subSpeciesName = null;
			final String taxonName = item.getFieldValue(KnownField.DELIMITEDNAME);
			String[] splitName = taxonName.split(nameSeparator);
			if (splitName.length == 1 ){
				genusName = splitName[0];
			}
			else if (splitName.length == 2){
				StringBuilder b = new StringBuilder();
				b.append(splitName[0]);
				b.append(" ");
				b.append(splitName[1]);
				speciesName =  b.toString();
			}
			else {
				StringBuilder b = new StringBuilder();
				b.append(splitName[0]);
				b.append(" ");
				b.append(splitName[1]);
				speciesName = b.toString();
				b.append(" ");
				b.append(splitName[2]);
				subSpeciesName = b.toString();
			}
			if (subSpeciesName != null && target.getTermbyName(subSpeciesName) != null){
				Term t = target.getTermbyName(subSpeciesName);
				SynonymI s = target.makeSynonymWithXref(subSpeciesName, prefix, Integer.toString(namesCounter++));
				t.addSynonym(s);
				matchCount++;
				resolvedItems.add(item);
				//System.out.println("Found matching subspecies: \t" + subSpeciesName + " total is " + matchCount);
			} 
			else if (subSpeciesName != null && speciesName != null && target.getTermbyName(speciesName) != null){
				Term t = target.getTermbyName(speciesName);
				SynonymI s = target.makeSynonymWithXref(subSpeciesName, prefix, Integer.toString(namesCounter++));
				t.addSynonym(s);
				matchCount++;
				resolvedItems.add(item);
				//System.out.println("Matched subspecies to parent species: \t" + subSpeciesName + " total is " + matchCount);
			}
			else if	(speciesName != null && target.getTermbyName(speciesName) != null){
				Term t = target.getTermbyName(speciesName);
				SynonymI s = target.makeSynonymWithXref(speciesName, prefix, Integer.toString(namesCounter++));
				t.addSynonym(s);
				matchCount++;
				resolvedItems.add(item);
				//System.out.println("Found matching species: \t" + speciesName + " total is " + matchCount);
			}
			else if(genusName != null && target.getTermbyName(genusName) != null){
				Term t = target.getTermbyName(genusName);
				SynonymI s = target.makeSynonymWithXref(genusName, prefix, Integer.toString(namesCounter++));
				t.addSynonym(s);
				matchCount++;
				resolvedItems.add(item);
				//System.out.println("Found matching genus: \t" + genusName + " total is " + matchCount);
			}
		}
		//Second pass looking for matches to synonyms
		logger.info("Starting synonym search pass");
		int unresolvedCount = 0;
		for(Item item : items.getContents()){ 
			if (!resolvedItems.contains(item)) {
				String genusName = null;
				String speciesName = null;
				String subSpeciesName = null;
				final String taxonName = item.getFieldValue(KnownField.DELIMITEDNAME);
				String[] splitName = taxonName.split(nameSeparator);
				if (splitName.length == 1 ){
					genusName = splitName[0];
				}
				else if (splitName.length == 2){
					StringBuilder b = new StringBuilder();
					b.append(splitName[0]);
					b.append(" ");
					b.append(splitName[1]);
					speciesName =  b.toString();
				}
				else {
					StringBuilder b = new StringBuilder();
					b.append(splitName[0]);
					b.append(" ");
					b.append(splitName[1]);
					speciesName = b.toString();
					b.append(" ");
					b.append(splitName[2]);
					subSpeciesName = b.toString();
				}
				if (subSpeciesName != null){
					for (Term t : target.getTerms()){
						for(SynonymI s : t.getSynonyms()){
							String synText = s.getText();
							if (subSpeciesName.equals(synText)){
								SynonymI sn = target.makeSynonymWithXref(subSpeciesName, prefix, Integer.toString(namesCounter++));
								t.addSynonym(sn);
								matchCount++;
								resolvedItems.add(item);
								//System.out.println("Found matching subspecies synonym: \t" + t.getLabel() + " total is " + matchCount);
								break;
							}
							else if (speciesName != null && speciesName.equals(synText)){
								SynonymI sn = target.makeSynonymWithXref(subSpeciesName, prefix, Integer.toString(namesCounter++));
								t.addSynonym(sn);
								matchCount++;
								resolvedItems.add(item);
								//System.out.println("Matched subspecies to parent species synonym: \t" + t.getLabel() + " total is " + matchCount);
								break;
							}
						}
					}
				}
				else if	(speciesName != null){
					for (Term t : target.getTerms()){
						for(SynonymI s : t.getSynonyms()){
							String synText = s.getText();
							if (speciesName.equals(synText)){
								SynonymI sn = target.makeSynonymWithXref(speciesName, prefix, Integer.toString(namesCounter++));
								t.addSynonym(sn);
								matchCount++;
								resolvedItems.add(item);
								//System.out.println("Found matching species synonym: \t" + t.getLabel() + " total is " + matchCount);
								break;
							}
						}
					}
				}
				else if(genusName != null){
					for (Term t : target.getTerms()){
						for(SynonymI s : t.getSynonyms()){
							String synText = s.getText();
							if (genusName.equals(synText)){
								SynonymI sn = target.makeSynonymWithXref(genusName, prefix, Integer.toString(namesCounter++));
								t.addSynonym(sn);
								matchCount++;
								resolvedItems.add(item);
								//System.out.println("Found matching genus synonym: \t" + t.getLabel() + " total is " + matchCount);
								break;
							}
						}
					}
				}
			}
		}
		logger.info("Final match total = " + matchCount);
		logger.info("Unresolved count = " + (items.getContents().size()-matchCount));
	}

	@Override
	public void attach(String parent, String cladeRoot, String prefix, boolean preserveIDs) {
		throw new RuntimeException("UnderscoreJoinedName does not support attach");
		// TODO Auto-generated method stub
	}

}
