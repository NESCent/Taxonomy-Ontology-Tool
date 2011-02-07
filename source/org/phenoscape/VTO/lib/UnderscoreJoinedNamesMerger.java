package org.phenoscape.VTO.lib;

import java.io.File;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

/**
 * This merger handles files with names joined by underscore in one column (eg., Homo_sapiens or Homo_sapiens_sapiens).  It currently
 * looks for a single column tagged with the knownfield DELIMITEDNAME and treats it as columns of genus, species and optionally subspecies.  
 * Apart from the absent higher levels, it should work as a merger.
 * 
 * @author peter
 *
 */




public class UnderscoreJoinedNamesMerger implements Merger, ColumnFormat {

	private final String columnSeparator; 
	
	private final String nameSeparator = "_";
	
	private final ColumnReader reader;
	
	private int namesCounter = 0;

	static Logger logger = Logger.getLogger(OBOStore.class.getName());

	
	public UnderscoreJoinedNamesMerger(String separator){
		columnSeparator = separator;
		reader = new ColumnReader(columnSeparator);
	}


	@Override
	public void setColumns(List<String> columns,Map<Integer, String> synPrefixes) {
		reader.setColumns(columns,synPrefixes);  // and what else?
		// at the moment, this reader can ignore synonym columns
	}

	@Override
	public boolean canAttach() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void merge(File source, TaxonStore target, String prefix) {
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
				SynonymI s = target.makeSynonym(subSpeciesName, "VSWG:", Integer.toString(namesCounter++));
				t.addSynonym(s);
				matchCount++;
				logger.info("Found matching subspecies: " + subSpeciesName + " total is " + matchCount);
			} 
			else if (subSpeciesName != null && speciesName != null && target.getTermbyName(speciesName) != null){
				Term t = target.getTermbyName(speciesName);
				SynonymI s = target.makeSynonym(subSpeciesName, "VSWG:", Integer.toString(namesCounter++));
				t.addSynonym(s);
				matchCount++;
				logger.info("Matched subspecies to parent species: " + subSpeciesName + " total is " + matchCount);
			}
			else if	(speciesName != null && target.getTermbyName(speciesName) != null){
				Term t = target.getTermbyName(speciesName);
				SynonymI s = target.makeSynonym(speciesName, "VSWG:", Integer.toString(namesCounter++));
				t.addSynonym(s);
				matchCount++;
				//System.out.println("Found matching species: " + speciesName + " total is " + matchCount);
			}
			else if(genusName != null && target.getTermbyName(genusName) != null){
				Term t = target.getTermbyName(genusName);
				SynonymI s = target.makeSynonym(genusName, "VSWG:", Integer.toString(namesCounter++));
				t.addSynonym(s);
				matchCount++;
				logger.info("Found matching genus: " + genusName + " total is " + matchCount);
			}			
		}
		logger.info("Final match total = " + matchCount);
	}

	@Override
	public void attach(File source, TaxonStore target, String parent,
			String prefix) {
		// TODO Auto-generated method stub

	}

}
