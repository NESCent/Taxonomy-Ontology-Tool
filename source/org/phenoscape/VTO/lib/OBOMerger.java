package org.phenoscape.VTO.lib;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;

import org.apache.log4j.Logger;
import org.obo.dataadapter.DefaultOBOParser;
import org.obo.dataadapter.OBOParseEngine;
import org.obo.dataadapter.OBOParseException;
import org.obo.datamodel.OBOClass;
import org.obo.datamodel.OBOProperty;
import org.obo.datamodel.OBOSession;
import org.obo.datamodel.impl.DefaultObjectFactory;
import org.obo.util.TermUtil;

public class OBOMerger implements Merger {
	
	static final Logger logger = Logger.getLogger(OBOMerger.class.getName());


	@Override
	public boolean canAttach() {
		// TODO Auto-generated method stub
		return true;
	}

	@Override
	public void merge(File source, TaxonStore target, String prefix) {
		// TODO Auto-generated method stub

	}

	@Override
	public void attach(File source, TaxonStore target, String parent, String prefix) {
		// TODO Auto-generated method stub

	}


	
	  



}
