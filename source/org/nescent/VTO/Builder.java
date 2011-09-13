/**
 * VTOTool - a tool for merging and building ontologies from multiple taxonomic sources
 * 
 * Peter Midford
 * 
 */
package org.nescent.VTO;

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
import org.nescent.VTO.lib.CoLMerger;
import org.nescent.VTO.lib.IOCMerger;
import org.nescent.VTO.lib.ITISMerger;
import org.nescent.VTO.lib.Merger;
import org.nescent.VTO.lib.NCBIMerger;
import org.nescent.VTO.lib.OBOMerger;
import org.nescent.VTO.lib.OBOStore;
import org.nescent.VTO.lib.OWLMerger;
import org.nescent.VTO.lib.ColumnMerger;
import org.nescent.VTO.lib.OWLStore;
import org.nescent.VTO.lib.TaxonStore;
import org.nescent.VTO.lib.UnderscoreJoinedNamesMerger;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class Builder {
	
	/* formats, supported for input, output (store) or both */
	final static String OBOFORMATSTR = "OBO"; 	//OBO format (as supported by OBO-Edit); supports merge, attach, output
	final static String ITISFORMATSTR = "ITIS";		//ITIS dump format (merge, attach)
	final static String NCBIFORMATSTR = "NCBI";		//NCBI dump format (merge, attach)	
	final static String CSVFORMATSTR = "CSV";	  //Comma separated text	
	final static String TSVFORMATSTR = "TSV";	  //Tab separated text
	final static String OWLFORMATSTR = "OWL";     //W3C OWL via OWLAPI
	final static String IOCFORMATSTR = "IOC";     //XML format used by IOC checklist (birds)
	final static String COLFORMATSTR = "COL";     //Catalogue of Life website
	final static String COLDBFORMATSTR = "COLDB";  //Catalogue of Life via MySQL
	final static String JOINEDNAMETABBEDCOLUMNS = "JOINEDNAMETAB";
	final static String XREFFORMATSTR = "XREF";    //This isn't a store format, but is a target
	final static String COLUMNFORMATSTR = "COLUMN";  //This isn't (necessary) a store format, but is a target
	final static String SYNONYMFORMATSTR = "SYNONYM"; //This a variant of the column format
	
	final static String TARGETTAXONOMYSTR = "target";
	final static String TARGETFORMATSTR = "format";
	final static String TARGETROOTSTR = "root";
	
	final static String ATTACHACTIONSTR = "attach";
	final static String MERGEACTIONSTR = "merge";
	final static String TRIMACTIONSTR = "trim";
	
	final static String COLUMNSYNTAXSTR = "column";
	
	final static String ATTACHFORMATSTR = "format";
	final static String ATTACHROOTSTR = "root";   //the root of the attached tree - a new child of node named by ATTACHPARENTSTR
	final static String ATTACHPARENTSTR = "parent";
	final static String ATTACHPREFIXSTR = "prefix";
	
	final static String PREFIXITEMSTR = "prefix";
	final static String FILTERPREFIXITEMSTR = "filterprefix";
	
	final private File optionsFile;
	
	
	static final Logger logger = Logger.getLogger(Builder.class.getName());


	/**
	 * Constructor just sets up the optionsFile field
	 * @param options
	 */
	Builder(File options){
		super();
		if (options == null)
			throw new IllegalArgumentException("No options file specified");
		optionsFile = options;
	}

	/**
	 * This processes the options file, builds the target and loops through the actions (attach, trim, merge)
	 * which are child elements of the root taxonomy element
	 * @throws IOException
	 * @throws ParserConfigurationException
	 * @throws SAXException
	 */
	public void build() throws IOException, ParserConfigurationException, SAXException {
		NodeList parseList = parseXMLOptionsFile();
		if (parseList.getLength() != 1){
			logger.fatal("Error - More than one taxonomy element in " + optionsFile.getCanonicalPath());
			return;
		}
		final Node taxonomyRoot = parseList.item(0);
		final String targetURLStr = getAttribute(taxonomyRoot,TARGETTAXONOMYSTR);
		final String targetFormatStr = getAttribute(taxonomyRoot,TARGETFORMATSTR);
		final String targetRootStr = getAttribute(taxonomyRoot,TARGETROOTSTR);
		final String targetPrefixStr = getAttribute(taxonomyRoot,PREFIXITEMSTR);
		final String targetFilterPrefixStr = getAttribute(taxonomyRoot,FILTERPREFIXITEMSTR);
		final TaxonStore target = getStore(targetURLStr, targetPrefixStr, targetFormatStr);
		logger.info("Building taxonomy to save at " + targetURLStr + " in the " + targetFormatStr + " format\n");
		NodeList actions = taxonomyRoot.getChildNodes();
		for(int i=0;i<actions.getLength();i++){
			processChildNode(actions.item(i),target,targetRootStr,targetFormatStr,targetPrefixStr);
		}
		saveTarget(targetFormatStr, targetFilterPrefixStr, target);
	}
	
	private NodeList parseXMLOptionsFile() throws ParserConfigurationException, SAXException, IOException{
		DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
		f.setNamespaceAware(true);
		DocumentBuilder db = f.newDocumentBuilder();
		db.setErrorHandler(new DefaultHandler());
		Document d = db.parse(optionsFile);
		return d.getElementsByTagNameNS("","taxonomy");
	}
	
	private void processChildNode(Node action, TaxonStore target,String targetRootStr, String targetFormatStr, String targetPrefixStr){
		final String actionName = action.getNodeName();
		if (ATTACHACTIONSTR.equalsIgnoreCase(actionName)){
			processAttachAction(action,target, targetRootStr, targetPrefixStr);
		}
		else if (MERGEACTIONSTR.equalsIgnoreCase(actionName)){
			processMergeAction(action, target, targetPrefixStr);
		}
		else if (TRIMACTIONSTR.equalsIgnoreCase(actionName)){
			target.trim(getAttribute(action,"node"));
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
	
	
	/**
	 * 
	 * @param action
	 * @param target
	 * @param targetRootStr
	 * @param targetPrefixStr prefix for new nodes in target - if this prefix equals the prefix of a term's ID, don't generate a new ID for the term copy
	 */
	private void processAttachAction(Node action, TaxonStore target, String targetRootStr, String targetPrefixStr){
		@SuppressWarnings("unchecked")
		List<String> columns = (List<String>)Collections.EMPTY_LIST;
		Map<Integer,String> synPrefixes = new HashMap<Integer,String>();  
		final String formatStr = getAttribute(action,ATTACHFORMATSTR);
		final String cladeRootStr = getAttribute(action,ATTACHROOTSTR);
		final String sourceParentStr = getAttribute(action,ATTACHPARENTSTR);
		final String sourcePrefixStr = getAttribute(action,ATTACHPREFIXSTR);
		NodeList childNodes = action.getChildNodes();
		if (childNodes.getLength()>0){
			columns = processChildNodesOfAttach(childNodes,synPrefixes);
		}
		Merger m = getMerger(formatStr,columns,synPrefixes);
		if (!m.canAttach()){
			throw new RuntimeException("Error - Merger for format " + formatStr + " can't attach branches to the tree");
		}
		if (!m.canPreserveID() && (sourcePrefixStr != null) && (!sourcePrefixStr.equals(targetPrefixStr))){
			throw new RuntimeException("Error - Merger for format " + formatStr + " can't preserve id prefixes - remove prefix attribute in attach");
		}
		String sourceURLStr = action.getAttributes().getNamedItem("source").getNodeValue();
		File sourceFile = getSourceFile(sourceURLStr);
		logger.info("Attaching taxonomy from " + sourceURLStr);
		if (targetPrefixStr == null){
			logger.warn("No prefix for newly generated ids specified - will default to filename component");
			targetPrefixStr = sourceFile.getName();
		}
		m.setSource(sourceFile);
		m.setTarget(target);
		if (sourceParentStr != null){   //need to specify the clade within the sourceFile (or null?)
			if (cladeRootStr != null)
				m.attach(sourceParentStr,cladeRootStr,targetPrefixStr);
			else
				m.attach(sourceParentStr,sourceParentStr,targetPrefixStr);
		}
		else {
			if (cladeRootStr != null)
				m.attach(targetRootStr,cladeRootStr,targetPrefixStr);
			else
				m.attach(targetRootStr,targetRootStr,targetPrefixStr);
		}

	}
	
	private void processMergeAction(Node action, TaxonStore target, String targetPrefixStr){
		@SuppressWarnings("unchecked")
		List<String> columns = (List<String>)Collections.EMPTY_LIST;
		final Map<Integer,String> synPrefixes = new HashMap<Integer,String>();  
		NodeList childNodes = action.getChildNodes();
		if (childNodes.getLength()>0){
			columns = processChildNodesOfAttach(childNodes,synPrefixes);
		}
		String formatStr = getAttribute(action,"format");
		Merger m = getMerger(formatStr,columns,synPrefixes);
		String sourceURLStr = getAttribute(action,"source");
		String mergePrefix = getAttribute(action,PREFIXITEMSTR);
		File sourceFile = null;  //CoL doesn't specify a fixed URL, we're not loading from one source - maybe this is too much of a special case
		if (!"".equals(sourceURLStr))
			sourceFile = getSourceFile(sourceURLStr);
		logger.info("Merging names from " + sourceURLStr);
		m.setSource(sourceFile);
		m.setTarget(target);
		if (mergePrefix == null){
			m.merge(targetPrefixStr);
		}
		else {
			m.merge(mergePrefix);
		}

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

	private TaxonStore getStore(String targetURLStr, String prefixStr, String formatStr) {
		URL u = null;
		try{
			u = new URL(targetURLStr);
		} catch (MalformedURLException e) {
			e.printStackTrace();
			return null;
		}
		if (!"file".equals(u.getProtocol())){
			logger.error("OBO format must save to a local file");
			return null;
		}
		File oldFile = new File(u.getFile());
		if (oldFile.exists())
			oldFile.delete();
		if (OBOFORMATSTR.equals(formatStr)){
			return new OBOStore(u.getFile(), prefixStr, prefixStr.toLowerCase() + "-namespace");
		}
		if (OWLFORMATSTR.equals(formatStr)){
			try{
				return new OWLStore(u.getFile(), prefixStr, prefixStr.toLowerCase() + "-namespace");
			} catch (OWLOntologyCreationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		// these source formats aren't storage formats (there's no ontology library for them) so the store is implementation dependent (currently OBO)
		if (XREFFORMATSTR.equals(formatStr) ||
			COLUMNFORMATSTR.equals(formatStr) ||
			SYNONYMFORMATSTR.equals(formatStr)){      //XREF isn't a storage format, so the store is 
			return new OBOStore(u.getFile(), prefixStr, prefixStr.toLowerCase() + "-namespace");
		}
		logger.error("Format " + formatStr + " not supported for merging");
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
		if (OWLFORMATSTR.equals(formatStr)){
			return new OWLMerger();
		}
		if (IOCFORMATSTR.equals(formatStr)){
			return new IOCMerger();
		}
		if (COLFORMATSTR.equals(formatStr)){
			return new CoLMerger();
		}
		logger.error("Format " + formatStr + " not supported for merging");
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

	private void saveTarget(String targetFormatStr, String targetFilterPrefixStr, TaxonStore target){
		if (XREFFORMATSTR.equals(targetFormatStr)){
			target.saveXref(targetFilterPrefixStr);
		}
		else if (COLUMNFORMATSTR.equals(targetFormatStr)){
			target.saveColumnsFormat(targetFilterPrefixStr);
		}
		else if (SYNONYMFORMATSTR.equals(targetFormatStr)){
			target.saveSynonymFormat(targetFilterPrefixStr);
		}
		else{
			target.saveStore();
		}
	}

// Utilities
	private String getAttribute(Node n,String attribute_id){
		final Node attNode = n.getAttributes().getNamedItem(attribute_id);
		if (attNode != null)
			return attNode.getNodeValue();
		else
			return null;
	}
	
	


}




