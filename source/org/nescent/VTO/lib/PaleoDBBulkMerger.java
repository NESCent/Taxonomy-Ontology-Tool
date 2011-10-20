package org.nescent.VTO.lib;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.nescent.VTO.lib.ITISMerger.ITISElement;

public class PaleoDBBulkMerger implements Merger{

	private File source = null;
	private TaxonStore target = null;
	
	private final Logger logger = Logger.getLogger(PaleoDBBulkMerger.class.getName());
	
	
	


	//Need to download using full fields (to get extinction status and other reasons)
	static private final String VALIDTAXAFILENAME = "valid_taxa.csv";
	static private final String INVALIDTAXAFILENAME = "invalid_taxa.csv";
	

	
	
	@Override
	public boolean canAttach() {
		return true;
	}

	@Override
	public boolean canPreserveID() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void setSource(File source) {
		this.source = source;
	}

	@Override
	public void setTarget(TaxonStore target) {
		this.target = target;
	}

	@Override
	public void merge(String prefix) {
		if (target == null){
			throw new IllegalStateException("Target ontology for PBDB bulk merge not set");
		}
		if (source == null){
			throw new IllegalStateException("Source directory of files for PBDB bulk merge not set");
		}
		// TODO Auto-generated method stub

	}

	/**
	 * @param defaultParent generally ignored, except if no names in a term's lineage resolves
	 * @param cladeRoot ignored in this merger
	 */
	@Override
	public void attach(String defaultParent, String cladeRoot, String prefix) {
		if (target == null){
			throw new IllegalStateException("Target ontology for PBDB bulk merge not set");
		}
		if (source == null){
			throw new IllegalStateException("Source directory of files for PBDB bulk merge not set");
		}
		
		
		final File validTaxaFile = new File(source.getAbsolutePath()+'/'+VALIDTAXAFILENAME);
		final File invalidTaxaFile = new File(source.getAbsolutePath()+'/'+INVALIDTAXAFILENAME);
		final Term defaultParentTaxon = target.getTermbyName(defaultParent);
		if (defaultParentTaxon == null){
			logger.info("Target size is " + target.getTerms().size());
			throw new IllegalArgumentException("Supplied parent " + defaultParent + " is not in the taxonomy");
		}
		logger.info("Checkpoint 2: Chordata = " + target.getTermbyName("Chordata"));
		logger.info("Checkpoint 2: Target size = " + target.getTerms().size());
		for (Term t : target.getTerms()){
			if ("Chordata".equals(t.getLabel())){
				logger.info("Found Chordata on through search on load check");
			}
		}
		List<PBDBItem> validTaxaList = null; 
		try{
			validTaxaList = buildPBDBList(validTaxaFile);
		}
		catch (IOException e){  
			logger.error("An IO Exception was thrown while parsing: " + validTaxaFile);
			e.printStackTrace();
			throw new RuntimeException("");
		}
		Map<String,String> synonymMap;
		try{
			synonymMap = buildSynonymLinks(invalidTaxaFile);
		}
		catch (IOException e){   //TODO think hard about allowing mergers to just pass these through
			logger.error("An IO Exception was thrown while parsing: " + invalidTaxaFile);
		}
		Map<String,String>taxonTree = buildTree(validTaxaList);
		Map<String,Term> termDictionary = new HashMap<String,Term>();
		for (Term t : target.getTerms()){
			termDictionary.put(t.getLabel(),t);
		}

		int startingSize = target.getTerms().size();
		Set <Term>newTerms = new HashSet<Term>();
		for(String tName : taxonTree.keySet()){
			if (!termDictionary.containsKey(tName)){
				Term newTerm = target.addTerm(tName);
				newTerms.add(newTerm);
				termDictionary.put(tName, newTerm);
				//System.out.println("Adding taxon: " + tName);
			}
		}
		int orphanCount = 0;
		for (String tName : taxonTree.keySet()){
			if (termDictionary.get(taxonTree.get(tName)) == null){
				orphanCount++;
			}
		}
		
		logger.info("Taxontree contains " + taxonTree.keySet().size() + " entries and " + orphanCount + " orphans");
		
		for (String tName : taxonTree.keySet()){
			Term child = termDictionary.get(tName);
			Term parent = termDictionary.get(taxonTree.get(tName));
			if (parent == null){
				parent = defaultParentTaxon;
			}
			if (newTerms.contains(child) && !parent.getChildren().contains(child)){
				target.attachParent(child, parent);
			}
		}
		for(PBDBItem item : validTaxaList){
			if (item.isValid()){
			}
			else{
			}
		}
		int endingSize = target.getTerms().size();
		logger.info("Taxon store grew from " + startingSize + " to " + endingSize);
		logger.info(defaultParent + " received " + defaultParentTaxon.getChildren().size() + " children");
	}

	List<PBDBItem> buildPBDBList(File taxonFile) throws IOException{
		List <PBDBItem> result = new ArrayList<PBDBItem>();
        final BufferedReader br = new BufferedReader(new FileReader(taxonFile));
        String raw = br.readLine();
        if (raw.startsWith("authorizer"))
        	raw = br.readLine();    //if column headers (in csv format) then skip them
        while (raw != null){
        	PBDBItem e = PBDBItem.getValidInstance(raw);
        	result.add(e);
        	raw = br.readLine();
        }
		return result;
	}

	
	//This reads from a three column file: syn_id, accepted_taxon_id, date of last modification 
	Map<String,String> buildSynonymLinks(File synonymFile) throws IOException{
		Map<String,String> result = new HashMap<String,String>();
		final BufferedReader br = new BufferedReader(new FileReader(synonymFile));
		String raw = br.readLine();
        if (raw.startsWith("authorizer"))
        	raw = br.readLine();    //if column headers (in csv format) then skip them
		while(raw != null){
			PBDBItem e = PBDBItem.getInvalidInstance(raw);
			String synonym = e.getName();
			String validName = e.getValidName();
			result.put(synonym, validName);
			raw = br.readLine();
		}
		return result;
	}
	
	
	/**
	 * Note: this returns a String->String table because the only reference to the parent taxon is the name - the id adjacent to the
	 * parent name is not for the parent.
	 * @param itemList
	 * @return
	 */
	Map<String,String> buildTree(List<PBDBItem> itemList){
		if (itemList == null)
			throw new RuntimeException("itemList was null - this code should not have been called!");
		Map<String,String> result = new HashMap<String,String>();
		for(PBDBItem item : itemList){
			if (item.isValid()){
				String name = item.getName();
				String parentName = item.getParentName();
				if (result.containsKey(name)){
					logger.error("Duplicate name in treelist");
				}
				else {
					result.put(name, parentName);
				}
			}
		}
		return result;
	}
	

}
