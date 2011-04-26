package org.phenoscape.VTO.lib;

import java.io.File;

public class OWLMerger implements Merger {

	@Override
	public boolean canAttach() {
		return false;
	}

	@Override
	public void merge(File source, TaxonStore target, String prefix) {
		throw new RuntimeException("OWLMerger not implemented yet");
		// TODO Auto-generated method stub

	}

	@Override
	public void attach(File source, TaxonStore target, String parent, String cladeRoot, String prefix) {
		throw new RuntimeException("OWLMerger does not currently implement attach");
		// TODO Auto-generated method stub

	}

}
