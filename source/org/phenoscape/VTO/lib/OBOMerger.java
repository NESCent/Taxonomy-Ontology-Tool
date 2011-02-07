package org.phenoscape.VTO.lib;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;

import org.apache.log4j.Logger;
import org.obo.dataadapter.DefaultOBOParser;
import org.obo.dataadapter.OBOParseEngine;
import org.obo.dataadapter.OBOParseException;
import org.obo.datamodel.Link;
import org.obo.datamodel.OBOClass;
import org.obo.datamodel.OBOProperty;
import org.obo.datamodel.OBOSession;
import org.obo.datamodel.impl.DefaultObjectFactory;
import org.obo.util.TermUtil;

public class OBOMerger implements Merger {
	
	static final Logger logger = Logger.getLogger(OBOMerger.class.getName());

	private OBOUtils u = null;
	
	String defaultPrefix;
	private String idSuffix = ":%07d";
	private String defaultFormat;


	@Override
	public boolean canAttach() {
		// TODO Auto-generated method stub
		return true;
	}

	@Override
	public void merge(File source, TaxonStore target, String prefix) {
		logger.info("Loading OBO file " + source);
		u = new OBOUtils(source.getAbsolutePath());
		logger.info("Finished loading");
		//u.setNameSpace(oboNameSpace, fileSpec);
		defaultPrefix = prefix;
		defaultFormat = defaultPrefix + idSuffix;
		
		
		//targetFile = fileSpec;
		//fillRankNames();
	}

	@Override
	public void attach(File source, TaxonStore target, String attachment, String prefix) {
		logger.info("Loading OBO file " + source);
		u = new OBOUtils(source.getAbsolutePath());
		logger.info("Finished loading");
		Term parentTerm = null;
		if (!"".equals(attachment)){
			parentTerm = target.getTermbyName(attachment);
			if (parentTerm == null){   //parent is unknown
				if (!target.isEmpty()){
					System.err.println("Can not attach " + source.getAbsolutePath() + " specified parent: " + attachment + " is unknown to " + target);
					return;
				}
				else { // attachment will be added first to provide a root for an otherwise empty target
					parentTerm = target.addTerm(attachment);
					logger.info("Assigning " + attachment + " as root");
				}
			}
		}
		Collection <Link> childCollection = parentTerm.asOBOClass().getChildren();
		//u.setNameSpace(oboNameSpace, fileSpec);
		defaultPrefix = prefix;
		defaultFormat = defaultPrefix + idSuffix;
		
		//targetFile = fileSpec;
		//fillRankNames();

	}


	
	  



}
