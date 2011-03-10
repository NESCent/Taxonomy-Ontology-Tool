package org.phenoscape.VTO.lib;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.util.Collection;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.phenoscape.VTO.Builder;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class CoLMerger implements Merger {

	
	final private static String COLURLPREFIX = "http://www.catalogueoflife.org/annual-checklist/2010/webservice?name=";
	final private static String COLFULLSUFFIX = "&response=full";
	final private static String SPACEEXP = " ";

	static final Logger logger = Logger.getLogger(CoLMerger.class.getName());

	
	@Override
	public boolean canAttach() {
		return false;
	}

	
	//Note: This should work differently - it should get the target's list of taxa and probe each against CoL and add synonyms
	//when it gets a hit (no source file)
	@Override
	public void merge(File source, TaxonStore target, String prefix) {
		DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
		f.setNamespaceAware(true);
		NodeList nl = null;
		final Collection<Term> terms = target.getTerms();
		for (Term term : terms){
			String[] components = term.getLabel().split(SPACEEXP);
			String GNIQuery = COLURLPREFIX + components[0] + "+" + components[1] + COLFULLSUFFIX;
			try {
				URL gnirequest = new URL(GNIQuery);
				DocumentBuilder db = f.newDocumentBuilder();
				db.setErrorHandler(new DefaultHandler());
				InputStream colStream = gnirequest.openStream();
				Document d = db.parse(colStream);
				nl = d.getElementsByTagNameNS("","synonyms");
				} catch (ParserConfigurationException e) {
					logger.fatal("Error in initializing parser");
					e.printStackTrace();
					return;
				} catch (SAXException e) {
					logger.fatal("Error in parsing " + GNIQuery);
					logger.fatal("Exception message is: " + e.getLocalizedMessage());
					return;
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} 
		}
	}

	@Override
	public void attach(File source, TaxonStore target, String parent, String cladeRoot, String prefix) {
		throw new RuntimeException("CoLMerger doesn't support attach");
	}

}
