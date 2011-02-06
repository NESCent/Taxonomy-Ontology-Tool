/**
 * VTOTool - a tool for merging and building ontologies from multiple taxonomic sources
 * 
 * Peter Midford
 * 
 */
package org.phenoscape.VTO;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.phenoscape.VTO.lib.ITISMerger;
import org.phenoscape.VTO.lib.Merger;
import org.phenoscape.VTO.lib.NCBIMerger;
import org.phenoscape.VTO.lib.OBOMerger;
import org.phenoscape.VTO.lib.OBOStore;
import org.phenoscape.VTO.lib.OWLMerger;
import org.phenoscape.VTO.lib.ColumnMerger;
import org.phenoscape.VTO.lib.TaxonStore;
import org.phenoscape.VTO.lib.UnderscoreJoinedNamesMerger;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class Builder {
	
	final static String OBOFORMATSTR = "OBO";
	final static String ITISFORMATSTR = "ITIS";
	final static String NCBIFORMATSTR = "NCBI";
	final static String CSVFORMATSTR = "CSV";
	final static String TSVFORMATSTR = "TSV";
	final static String OWLFORMATSTR = "OWL";
	final static String JOINEDNAMETABBEDCOLUMNS = "JOINEDNAMETAB";
	
	final static String PREFIXITEMSTR = "prefix";
	

	final private File optionsFile;
	private TaxonStore target;
	
	
	static final Logger logger = Logger.getLogger(Builder.class.getName());



	Builder(File options){
		optionsFile = options;
	}

	public void build() {
		DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
		f.setNamespaceAware(true);
		NodeList nl = null;
		try {
			DocumentBuilder db = f.newDocumentBuilder();
			db.setErrorHandler(new DefaultHandler());
			Document d = db.parse(optionsFile);
			nl = d.getElementsByTagNameNS("","taxonomy");
		} catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if (nl.getLength() != 1){
			System.err.println("Error - More than one taxonomy element");
			return;
		}
		Node taxonomyRoot = nl.item(0);
		String targetURLStr = taxonomyRoot.getAttributes().getNamedItem("target").getNodeValue();
		String targetFormatStr = taxonomyRoot.getAttributes().getNamedItem("format").getNodeValue();
		String targetRootStr = taxonomyRoot.getAttributes().getNamedItem("root").getNodeValue();
		String targetPrefixStr = taxonomyRoot.getAttributes().getNamedItem(PREFIXITEMSTR).getNodeValue();
		TaxonStore target = getStore(targetURLStr, targetFormatStr);
		logger.info("Building taxonomy to save at " + targetURLStr + " in the " + targetFormatStr + " format\n");
		NodeList actions = taxonomyRoot.getChildNodes();
		for(int i=0;i<actions.getLength();i++){
			Node action = actions.item(i);
			String actionName = action.getNodeName();
			@SuppressWarnings("unchecked")
			List<String> columns = (List<String>)Collections.EMPTY_LIST;
			Map<Integer,String> synPrefixes = new HashMap<Integer,String>();  
			if ("attach".equalsIgnoreCase(actionName)){
				String formatStr = action.getAttributes().getNamedItem("format").getNodeValue();
				NodeList childNodes = action.getChildNodes();
				if (childNodes.getLength()>0){
					columns = processChildNodesOfAttach(childNodes,synPrefixes);
				}
				Merger m = getMerger(formatStr,columns,synPrefixes);
				if (!m.canAttach()){
					logger.error("Error - Merger for format " + formatStr + " can't attach branches to the tree");
				}
				String sourceURLStr = action.getAttributes().getNamedItem("source").getNodeValue();
				File sourceFile = getSourceFile(sourceURLStr);
				Node sourceParentNode = action.getAttributes().getNamedItem("parent");
				if (targetPrefixStr == null)
					targetPrefixStr = "XXX";
				if (sourceParentNode != null){
					m.attach(sourceFile,target,sourceParentNode.getNodeValue(),targetPrefixStr);
				}
				else {
					m.attach(sourceFile,target,targetRootStr,targetPrefixStr);
				}
			}
			else if ("merge".equalsIgnoreCase(actionName)){
				NodeList childNodes = action.getChildNodes();
				if (childNodes.getLength()>0){
					columns = processChildNodesOfAttach(childNodes,synPrefixes);
				}
				String formatStr = action.getAttributes().getNamedItem("format").getNodeValue();
				Merger m = getMerger(formatStr,columns,synPrefixes);
				String sourceURLStr = action.getAttributes().getNamedItem("source").getNodeValue();
				File sourceFile = getSourceFile(sourceURLStr);
				logger.info("Merging names from " + sourceURLStr);
				m.merge(sourceFile, target, "XXX");
			}
			else if (action.getNodeType() == Node.TEXT_NODE){
				//ignore
			}
			else if (action.getNodeType() == Node.COMMENT_NODE){
				//ignore
			}
			else{
				logger.warn("Unknown action: " + action);
			}
		}
		target.saveStore();
	}

	private List<String> processChildNodesOfAttach(NodeList childNodes, Map<Integer, String> synPrefixes) {
		List<String> result = new ArrayList<String>();
		for(int i = 0; i<childNodes.getLength();i++){
			Node child = childNodes.item(i);
			String childName = child.getNodeName();
			if ("columns".equals(childName)){
				NodeList columnNames = child.getChildNodes();
				for(int j = 0; j<columnNames.getLength();j++){
					Node column = columnNames.item(j);
					if (column.getNodeType() == Node.ELEMENT_NODE){
						result.add(column.getNodeName());
						if (column.getAttributes().getLength()>0){
							String synPrefix = column.getAttributes().getNamedItem(PREFIXITEMSTR).getNodeValue();
							synPrefixes.put(j, synPrefix);
						}
					}
				}
				
			}
			else if (child.getNodeType() == Node.TEXT_NODE){
				//ignore
			}
			else{
				logger.warn("Unknown subelement" + child);
			}
		}
		return result;
	}

	private TaxonStore getStore(String targetURLStr, String formatStr) {
		if (OBOFORMATSTR.equals(formatStr)){
			try {
				URL u = new URL(targetURLStr);
				if (!"file".equals(u.getProtocol())){
					System.err.println("OBO format must save to a local file");
					return null;
				}
				File oldFile = new File(u.getFile());
				if (oldFile.exists())
					oldFile.delete();
				return new OBOStore(u.getFile(),"TEST","test-namespace");
			} catch (MalformedURLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return null;
			}
		}
		System.err.println("Format " + formatStr + " not supported for merging");
		return null;
	}
	
	
	private Merger getMerger(String formatStr, List<String> columns, Map<Integer, String> synPrefixes){
		if (OBOFORMATSTR.equals(formatStr))
			return new OBOMerger();
		if (ITISFORMATSTR.equals(formatStr))
			return new ITISMerger();
		if (NCBIFORMATSTR.equals(formatStr))
			return new NCBIMerger();
		if (CSVFORMATSTR.equals(formatStr)){
			ColumnMerger result = new ColumnMerger(",");
			result.setColumns(columns, synPrefixes);
			return (Merger)result;
		}
		if (TSVFORMATSTR.equals(formatStr)){
			ColumnMerger result = new ColumnMerger("\t");
			result.setColumns(columns, synPrefixes);
			return (Merger)result;
		}
		if (JOINEDNAMETABBEDCOLUMNS.equals(formatStr)){
			UnderscoreJoinedNamesMerger result = new UnderscoreJoinedNamesMerger("\t");
			result.setColumns(columns, synPrefixes);
			return (Merger)result;
		}
		if (OWLFORMATSTR.equals(formatStr))
			return new OWLMerger();
		System.err.println("Format " + formatStr + " not supported for merging");
		return null;
	}
	
	private File getSourceFile(String sourceURLStr) {
		try {
			URL u = new URL(sourceURLStr);
			if (!"file".equals(u.getProtocol())){
				System.err.println("Can only load from a local file");
				return null;
			}
			return new File(u.getFile());
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
	}




}




