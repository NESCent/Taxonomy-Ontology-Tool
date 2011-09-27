package org.nescent.VTO.lib;

import java.io.File;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

public class PaleoDBBulkMerger implements Merger, ColumnFormat {

	private File source;
	private TaxonStore target;
	
	private final Logger logger = Logger.getLogger(PaleoDBBulkMerger.class.getName());
	
	private final String VALIDTAXAFILENAME = "valid_taxa";
	private final String INVALIDTAXAFILENAME = "invalid_taxa";
	
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

	@Override
	public void setColumns(List<String> columns, Map<Integer, String> synPrefixes) {
		// TODO Auto-generated method stub
		
	}

}
