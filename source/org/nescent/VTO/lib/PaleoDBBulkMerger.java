package org.nescent.VTO.lib;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.nescent.VTO.lib.ITISMerger.ITISElement;

public class PaleoDBBulkMerger implements Merger{

	private File source;
	private TaxonStore target;
	
	private final Logger logger = Logger.getLogger(PaleoDBBulkMerger.class.getName());
	
	//These should be downloaded as itis format with pipe '|' delimiters
	private final String TAXONUNITSFILENAME = "taxonomic_units.dat";
	private final String SYNONYMLINKSFILENAME = "synonym_links.dat";
	
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
		// TODO Auto-generated method stub

	}

	@Override
	public void attach(String parent, String cladeRoot, String prefix) {
		final File validTaxonFile = new File(source.getAbsolutePath()+'/'+VALIDTAXAFILENAME);
		final File invalidTaxonFile = new File(source.getAbsolutePath()+'/'+INVALIDTAXAFILENAME);

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
	
	
	
	
	
	

}
