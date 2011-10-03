package org.nescent.VTO.lib;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
		List<PBDBItem> itemList;
		try{
			itemList = buildPBDBList(taxonUnitsFile);
		}
		catch (IOException e){
			logger.error("An IO Exception was thrown while parsing: " + taxonUnitsFile);
			e.printStackTrace();
		}
		Map<Integer,Integer> synonymMap;
		try{
			synonymMap = buildSynonymLinks(synonymLinksFile);
		}
		catch (IOException e){
			logger.error("An IO Exception was thrown while parsing: " + synonymLinksFile);
		}

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
	
	
	
	

}
