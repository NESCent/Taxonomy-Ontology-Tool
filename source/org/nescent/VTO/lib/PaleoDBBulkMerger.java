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
	
	//These should be downloaded as itis format with pipe '|' delimiters
	private final String TAXONUNITSFILENAME = "taxonomic_units.dat";
	private final String SYNONYMLINKSFILENAME = "synonym_links.dat";
	
	static final Pattern pipePattern = Pattern.compile("\\|");
	
	//columns for itis format
	static final int SYNIDCOLUMN = 0;
	static final int ACCEPTEDIDCOLUMN = 1;
	static final int DATECOLUMN = 2;


	
	//These are not currently used, but if csv downloads are supported in the future...
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
	 * @param parent ignored in this merger
	 * @param cladeRoot ignored in this merger - each term provides it's own parentage
	 */
	@Override
	public void attach(String parent, String cladeRoot, String prefix) {
		if (target == null){
			throw new IllegalStateException("Target ontology for PBDB bulk merge not set");
		}
		if (source == null){
			throw new IllegalStateException("Source directory of files for PBDB bulk merge not set");
		}
		final File taxonUnitsFile = new File(source.getAbsolutePath()+'/'+TAXONUNITSFILENAME);
		final File synonymLinksFile = new File(source.getAbsolutePath()+'/'+SYNONYMLINKSFILENAME);
		List<PBDBItem> itemList = null; 
		try{
			itemList = buildPBDBList(taxonUnitsFile);
		}
		catch (IOException e){   //TODO think hard about allowing mergers to just pass these through
			logger.error("An IO Exception was thrown while parsing: " + taxonUnitsFile);
			e.printStackTrace();
		}
		Map<Integer,Integer> synonymMap;
		try{
			synonymMap = buildSynonymLinks(synonymLinksFile);
		}
		catch (IOException e){   //TODO think hard about allowing mergers to just pass these through
			logger.error("An IO Exception was thrown while parsing: " + synonymLinksFile);
		}
		Map<String,Set<String>>taxonTree = buildTree(itemList);
		
	}

	List<PBDBItem> buildPBDBList(File taxonFile) throws IOException{
		List <PBDBItem> result = new ArrayList<PBDBItem>();
        final BufferedReader br = new BufferedReader(new FileReader(taxonFile));
        String raw = br.readLine();
        while (raw != null){
        	PBDBItem e = processLine(raw);
        	result.add(e);
        	raw = br.readLine();
        }
		return result;
	}
	
	PBDBItem processLine(String raw){
		return new PBDBItem(raw);
	}
	
	enum TaxonomicStatus{
		VALID,
		JUNIOR_SYNONYM,
		NOMEN_NUDUM,
		NOMEN_DUBIUM,
		ORIGINALNAME_COMBINATION,
		UNRECOGNIZED
	}
	
	//This reads from a three column file: syn_id, accepted_taxon_id, date of last modification 
	Map<Integer,Integer> buildSynonymLinks(File synonymFile) throws IOException{
		Map<Integer,Integer> result = new HashMap<Integer,Integer>();
		final BufferedReader br = new BufferedReader(new FileReader(synonymFile));
		String raw = br.readLine();
		while(raw != null){
			final String[] digest = pipePattern.split(raw);
			int synID = -1;
			int acceptedID = -1;
			if (digest.length != 3){  // should be exactly 3
				throw new RuntimeException("Line had wrong number of elements: " + digest.length);
			}
			try {
				synID =Integer.parseInt(digest[SYNIDCOLUMN]);
				acceptedID = Integer.parseInt(digest[ACCEPTEDIDCOLUMN]);
			}
			catch (NumberFormatException e){
				throw new RuntimeException("Misformatted ID (synid = " + digest[SYNIDCOLUMN] + "; acceptedid = " + digest[ACCEPTEDIDCOLUMN] + ") in line " + raw);
			}
			result.put(synID, acceptedID);
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
	Map<String,Set<String>> buildTree(List<PBDBItem> itemList){
		if (itemList == null)
			throw new RuntimeException("itemList was null - this code should not have been called!");
		Map<String,Set<String>> result = new HashMap<String,Set<String>>();
		for(PBDBItem item : itemList){
			if (item.isValid()){
				String name = item.getName();
				String parentName = item.getParentName();
				if (result.containsKey(parentName)){
					Set<String> children = result.get(parentName);
					children.add(name);
				}
				else {
					Set<String> children = new HashSet<String>();
					children.add(name);
					result.put(parentName, children);
				}
			}
		}
		return result;
	}
	

}
