package org.nescent.VTO.lib;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

public class PaleoDBBulkMerger implements Merger{

	private File source = null;
	private TaxonStore target = null;
	
	private final Logger logger = Logger.getLogger(PaleoDBBulkMerger.class.getName());
	


	//Need to download using full fields (to get extinction status and other reasons)
	private static final String VALIDTAXAFILENAME = "valid_taxa.csv";
	private static final String INVALIDTAXAFILENAME = "invalid_taxa.csv";
	
	private static final String TARGETNOTSETMESSAGE = "Target ontology for PBDB bulk attach/merge not set";
	private static final String SOURCENOTSETMESSAGE = "Source directory of files for PBDB bulk attach/merge not set";
	
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
			throw new IllegalStateException(TARGETNOTSETMESSAGE);
		}
		if (source == null){
			throw new IllegalStateException(SOURCENOTSETMESSAGE);
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
			throw new IllegalStateException(TARGETNOTSETMESSAGE);
		}
		if (source == null){
			throw new IllegalStateException(SOURCENOTSETMESSAGE);
		}
		
		final File validTaxaFile = new File(source.getAbsolutePath()+'/'+VALIDTAXAFILENAME);
		final File invalidTaxaFile = new File(source.getAbsolutePath()+'/'+INVALIDTAXAFILENAME);
		final Term defaultParentTaxon = target.getTermbyName(defaultParent);
		if (defaultParentTaxon == null){
			logger.info("Target size is " + target.getTerms().size());
			throw new IllegalArgumentException("Supplied parent " + defaultParent + " is not in the taxonomy");
		}
		Map<String,PBDBItem> validTaxa = null; 
		try{
			validTaxa = buildPBDBList(validTaxaFile);
		}
		catch (IOException e){  
			logger.error("An IO Exception was thrown while parsing: " + validTaxaFile);
			throw new RuntimeException("",e);
		}
		Map<String,PBDBItem> invalidTaxa = null;
		try{
			invalidTaxa = buildPBDBList(invalidTaxaFile);
		}
		catch (IOException e){   //TODO think hard about allowing mergers to just pass these through
			logger.error("An IO Exception was thrown while parsing: " + invalidTaxaFile);
			throw new RuntimeException("",e);
		}

		final Map<String,String>taxonTree = buildTree(validTaxa,invalidTaxa);

		final Set<String> orphans = orphanCheck(taxonTree,defaultParent);
		
		pruneOrphans(taxonTree,orphans);
				
		logger.info("Taxontree contains " + taxonTree.keySet().size() + " entries and " + orphans.size() + " orphans");
		
		final Map<String,Term> termDictionary = new HashMap<String,Term>();
		for (Term t : target.getTerms()){
			termDictionary.put(t.getLabel(),t);
		}

		final int startingSize = target.getTerms().size();
		final Set <Term>newTerms = new HashSet<Term>();
		for(String tName : taxonTree.keySet()){
			if (!termDictionary.containsKey(tName)){
				final PBDBItem item = validTaxa.get(tName);
				final Term newTerm = target.addTerm(tName);
				target.addXRefToTerm(newTerm, "PALEODB", Integer.toString(item.getId()));
				if (item.isExtinct()){
					target.setExtinct(newTerm);
				}
				newTerms.add(newTerm);
				termDictionary.put(tName, newTerm);
				//System.out.println("Adding taxon: " + tName);
			}
		}
		
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
		final int endingSize = target.getTerms().size();
		logger.info("Taxon store grew from " + startingSize + " to " + endingSize);
		logger.info(defaultParent + " received " + defaultParentTaxon.getChildren().size() + " children");
	}


	Map<String,PBDBItem> buildPBDBList(File taxonFile) throws IOException{
		final Map<String,PBDBItem> result = new HashMap<String,PBDBItem>();
        final BufferedReader br = new BufferedReader(new FileReader(taxonFile));
        Map<String,Integer>columns = null;
        String raw = br.readLine();
        if (raw.startsWith("authorizer")){ //if column headers (in csv format) then skip them
        	columns = PBDBItem.processHeaders(raw);
        	raw = br.readLine();    //read the next line
        }	
        else {
        	throw new RuntimeException("No header row provided");
        }
        while (raw != null){
        	PBDBItem e = PBDBItem.getValidInstance(raw,columns);
        	result.put(e.getName(),e);
        	raw = br.readLine();
        }
		return result;
	}

	
	
	
	/**
	 * Note: this returns a String->String table because the only reference to the parent taxon is the name - the id adjacent to the
	 * parent name is not for the parent.
	 * @param validTaxa
	 * @return
	 */
	Map<String,String> buildTree(Map<String, PBDBItem> validTaxa,Map<String,PBDBItem> invalidTaxa){
		if (validTaxa == null)
			throw new RuntimeException("itemList was null - this code should not have been called!");
		final Map<String,String> result = new HashMap<String,String>();
		for(String name : validTaxa.keySet()){
			PBDBItem item = validTaxa.get(name);
			String finalParent = null;
			if (item.isValid()){
				String parentName = item.getParentName();
				if (validTaxa.containsKey(parentName)){
					finalParent = parentName;
				}
				else{
					PBDBItem parentItem = invalidTaxa.get(parentName);
					if (parentItem == null){
						finalParent = parentName;
					}
					else if (parentItem.isSynonym()){
						finalParent = parentItem.getValidName();
					}
					else if (parentItem.isInvalidSubgroup()){ //redundant, but a little clearer
						while(parentItem.isInvalidSubgroup()){
							parentName = parentItem.getValidName();
							if(validTaxa.containsKey(parentName)){
								parentItem = validTaxa.get(parentName);
							}
							else if (invalidTaxa.containsKey(parentName)){
								parentItem = invalidTaxa.get(parentName);
							}
							else {
								throw new RuntimeException("Transitive parent: " + parentName + " appears in neither valid or invalid names");								
							}
						}
						if (parentItem.isValid()){
							finalParent = parentItem.getName();
						}
						if (parentItem.isSynonym()){
							finalParent = parentItem.getValidName();
						}
					}
				}
				if (result.containsKey(name)){
					logger.error("Duplicate name " + name + " in treelist");
				}
				else {
					result.put(name, finalParent);
				}
			}
		}
		return result;
	}
	
	
	Set<String>orphanCheck(Map<String,String>taxonTree, String defaultParent){
		final Set<String>validNames = new HashSet<String>();  //strictly for orphan checking
		final Set<String> result = new HashSet<String>();
		for (Term t : target.getTerms()){
			validNames.add(t.getLabel());
		}
		for(String tName : taxonTree.keySet()){
			validNames.add(tName);
		}		
		for (String tName : taxonTree.keySet()){
			if (!validNames.contains(taxonTree.get(tName)) && !tName.equalsIgnoreCase(defaultParent)){
				result.add(tName);
				System.out.println("Orphan: " + tName);
			}
		}
		return result;
	}
	
	int orphanCount = 0;
	private void pruneOrphans(Map<String, String> taxonTree, Set<String> orphans) {
		final Set<String>treeNodes = new HashSet<String>();
		treeNodes.addAll(taxonTree.keySet());
		for(String orphan : orphans)
			pruneOrphan(taxonTree,treeNodes,orphan);
	}
	
	private void pruneOrphan(Map<String,String>taxonTree,Set<String>treeNodes,String orphan){
		orphanCount++;
		System.out.println("Orphan: " + orphan);
		for(String node : treeNodes){
			if (orphan.equals(taxonTree.get(node))){
				pruneOrphan(taxonTree,treeNodes,node);
			}
		}
		taxonTree.remove(orphan);
	}
	
	

}
