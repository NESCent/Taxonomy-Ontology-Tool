package org.nescent.VTO.lib;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collection;
import java.util.HashSet;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class CoLMerger implements Merger {

	
	final private static String COLURLPREFIX = "http://www.catalogueoflife.org/annual-checklist/2011/webservice?name=";
	final private static String COLFULLSUFFIX = "&response=full";
	final private static String SPACEEXP = " ";

	static final Logger logger = Logger.getLogger(CoLMerger.class.getName());

	
	@Override
	public boolean canAttach() {
		return false;
	}

	
	//Note: This works differently from other mergers. It gets the target's list of taxa and probe each against CoL and add synonyms
	//when it gets a hit (no source file)
	//TODO - filter out non-child taxa
	@Override
	public void merge(File source, TaxonStore target, String prefix) {
		DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
		f.setNamespaceAware(true);
		final Collection<Term> terms = target.getTerms();
		for (Term term : terms){
			if (true){
				String[] nameSplit = term.getLabel().split(" ");
				if (nameSplit.length == 2 || nameSplit.length == 3){
					logger.info("Processing: " + term.getLabel());
					Collection<NamePair> synList = queryOneName(term.getLabel(),f);
					if (synList != null){
						for (NamePair syn : synList){
							SynonymI newSyn = target.makeSynonymWithXref(syn.getName(), prefix, syn.getID());
							term.addSynonym(newSyn);
						}
						logger.info("Waiting 0.8 seconds");
						try {
							Thread.sleep(800);
						} catch (InterruptedException e) {
							logger.info("Got an interrupted exception");
						}
						logger.info("Done");
					}
				}
			}
		}
	}

	
	public Collection<NamePair> queryOneName(String name,DocumentBuilderFactory f){
		String[] components = name.split(SPACEEXP);
		if (components.length > 1){
			String CoLQuery = COLURLPREFIX + components[0] + "+" + components[1] + COLFULLSUFFIX;
			return processXML(CoLQuery,f);
		}
		else return null;
	}
	
	
	
	@Override
	public void attach(File source, TaxonStore target, String parent, String cladeRoot, String prefix) {
		throw new RuntimeException("CoLMerger doesn't support attach");
	}

	private Collection<NamePair> processXML(String CoLQuery, DocumentBuilderFactory f){
		Collection<NamePair> result = new HashSet<NamePair>();
		NodeList nl = null;
		try {
			URL gnirequest = new URL(CoLQuery);
			DocumentBuilder db = f.newDocumentBuilder();
			db.setErrorHandler(new DefaultHandler());
			InputStream colStream = gnirequest.openStream();
			Document d = db.parse(colStream);
			nl = d.getElementsByTagNameNS("","synonyms");
		} catch (ParserConfigurationException e) {
			logger.fatal("Error in initializing parser");
			e.printStackTrace();
			return result;
		} catch (SAXException e) {
			logger.fatal("Error in parsing from " + CoLQuery);
			logger.fatal("Exception message is: " + e.getLocalizedMessage());
			return result;
		} catch (IOException e) {
			logger.fatal("Error in reading from " + CoLQuery);
			logger.fatal("Exception message is: " + e.getLocalizedMessage());
			return result;
		} 
		System.out.println("Query is " + CoLQuery);
		if (nl != null && nl.getLength() > 0){
			logger.debug("nl length = " + nl.getLength());
			for(int j=0;j<nl.getLength();j++){
				Node synNode = nl.item(j);
				NodeList synonymChildren = synNode.getChildNodes();
				if (synonymChildren.getLength()>0){
					for(int i=0;i<synonymChildren.getLength();i++){
						NamePair synPair = extractSynonym(synonymChildren.item(i));
						if (synPair != null){
							result.add(synPair);
						}
					}
				}
				else {
					System.out.println("nl is empty");
				}
			}
		}
		logger.info("Taxon has " + result.size() + " synonyms");
		return result;
	}
	
	private NamePair extractSynonym(Node syn){
		String name = null;
		String id = null;
		NodeList children = syn.getChildNodes();
		for (int i=0;i<children.getLength();i++){
			Node child = children.item(i);
			if ("name".equals(child.getNodeName())){
				NodeList grandChildren = child.getChildNodes();
				for (int j=0;j<grandChildren.getLength();j++){
					Node grandChild = grandChildren.item(j);
					name = grandChild.getNodeValue();
				}
			}
			if ("id".equals(child.getNodeName())){
				NodeList grandChildren = child.getChildNodes();
				for (int j=0;j<grandChildren.getLength();j++){
					Node grandChild = grandChildren.item(j);
					id = grandChild.getNodeValue();
				}
			}
		}
		if (name != null && id != null)
			return new NamePair(name,id); 
		else
			return null;
	}
	
	
	public static class NamePair{
		String id;
		String name;
		
		NamePair(String nameStr, String idStr){
			id = idStr;
			name = nameStr;
		}
		
		String getName(){
			return name;
		}
		
		String getID(){
			return id;
		}
	}
}
