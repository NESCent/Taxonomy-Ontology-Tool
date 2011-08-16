package org.nescent.VTO.lib;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Properties;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;


/**
 * This class merges names from the Catalog of Life.  The present implementation 
 * uses the CoL web interface to retrieve synonyms.  Therefore there is no source
 * file. It operates by getting the target's list of taxa and probes each against 
 * CoL and adds synonyms when it gets a hit.
 * 
 * It doesn't support attaching.
 * 
 * @author peter
 *
 */
public class CoLDBMerger implements Merger {


	static final Logger logger = Logger.getLogger(CoLDBMerger.class.getName());

	
	/**
	 * @return false because this merger supports attaching
	 */
	@Override
	public boolean canAttach() {
		return true;
	}

	/**
	 * 
	 * @param source
	 * @param target
	 * @param prefix
	 */
	@Override
	public void merge(File source, TaxonStore target, String prefix) {
		//TODO - filter out non-child taxa
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

	/**
	 * 
	 * @param name
	 * @param f
	 * @return
	 */
	public Collection<NamePair> queryOneName(String name,DocumentBuilderFactory f){
		String[] components = name.split(SPACEEXP);
		if (components.length > 1){
			String CoLQuery = COLURLPREFIX + components[0] + "+" + components[1] + COLFULLSUFFIX;
			return processXML(CoLQuery,f);
		}
		else return null;
	}
	
	
	/**
	 * 
	 * @param source
	 * @param target
	 * @param parent
	 * @param cladeRoot
	 * @param prefix
	 */
	@Override
	public void attach(File source, TaxonStore target, String parent, String cladeRoot, String prefix) {
		
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
	
	
	public Connection openKBFromConnections(String connectionsSpec) throws SQLException {
		final Properties properties = new Properties();
		try {
			properties.load(this.getClass().getResourceAsStream(connectionsSpec));
		} catch (Exception e1) {
			throw new RuntimeException("Failed to open connection properties file; path = " + connectionsSpec);
		} 
		try {
			Class.forName("com.mysql.jdbc.Driver");
		} catch(ClassNotFoundException e){
			System.err.println("Couldn't load MySQL Driver");
			e.printStackTrace();
		}
		final String host = properties.getProperty("host");
		final String db = properties.getProperty("db");
		final String user = properties.getProperty("user");
		final String password = properties.getProperty("pw");
		return DriverManager.getConnection(String.format("jdbc:mysql://%s/%s",host,db),user,password);

	}

}
