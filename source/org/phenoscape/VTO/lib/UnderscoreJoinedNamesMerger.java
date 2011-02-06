package org.phenoscape.VTO.lib;

import java.io.File;
import java.util.List;
import java.util.Map;

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
			String speciesName = null;
			String subSpeciesName = null;
			final String taxonName = item.getFieldValue(KnownField.DELIMITEDNAME);
			String[] splitName = taxonName.split(nameSeparator);
			if (splitName.length <2){
				System.err.println("No delimiter found in split name");
			}
			else if (splitName.length == 2){
				speciesName = splitName[0] + splitName[1];
			}
			else {
				speciesName = splitName[0] + splitName[1];
				subSpeciesName = splitName[0] + splitName[1] + splitName[2];
			}
			if (target.getTermbyName(speciesName) != null){
				matchCount++;
				System.out.println("Found matching species: " + speciesName + " total is " + matchCount);
			}
			else if (target.getTermbyName(subSpeciesName) != null){
				matchCount++;
				System.out.println("Found matching subspecies: " + subSpeciesName + " total is " + matchCount);
			}
		}
	}

	@Override
	public void attach(File source, TaxonStore target, String parent,
			String prefix) {
		// TODO Auto-generated method stub

	}

}
