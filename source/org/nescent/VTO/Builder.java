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
import org.nescent.VTO.lib.CoLDBMerger;
import org.nescent.VTO.lib.CoLMerger;
import org.nescent.VTO.lib.ColumnMerger;
import org.nescent.VTO.lib.ColumnType;
import org.nescent.VTO.lib.IOCMerger;
import org.nescent.VTO.lib.ITISMerger;
import org.nescent.VTO.lib.Merger;
import org.nescent.VTO.lib.NCBIMerger;
import org.nescent.VTO.lib.OBOMerger;
import org.nescent.VTO.lib.OBOStore;
import org.nescent.VTO.lib.OWLMerger;
import org.nescent.VTO.lib.OWLStore;
import org.nescent.VTO.lib.PBDBPostProcess;
import org.nescent.VTO.lib.PaleoDBBulkMerger;
import org.nescent.VTO.lib.SynonymSource;
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
	final static String PIPESEPARATEDSTR = "Pipe";  //Pipe (|) separated columns
	final static String OWLFORMATSTR = "OWL";     //W3C OWL via OWLAPI
	final static String IOCFORMATSTR = "IOC";     //XML format used by IOC checklist (birds)
	final static String COLFORMATSTR = "COL";     //Catalogue of Life website
	final static String COLDBFORMATSTR = "COLDB";  //Catalogue of Life via MySQL
	final static String PBDBBULKFORMATSTR = "PBDBbulk";  //Paleobiology Database bulk taxon downloads
	final static String PBDBUPDATEFORMATSTR = "PBDBupdate";  //Paleobiology Database update (list of names to add from PBDB)
	final static String PBDBPOSTPROCESSSTR = "PBDBPostProcess"; //Paleobiology postprocess file (corrections to apply after a PBDBbulk merge operation) 
	
	final static String JOINEDNAMETABBEDCOLUMNS = "JOINEDNAMETAB";
	final static String XREFFORMATSTR = "XREF";    //This isn't a store format, but is a target
	final static String COLUMNFORMATSTR = "COLUMN";  //This isn't (necessary) a store format, but is a target
	final static String SYNONYMFORMATSTR = "SYNONYM"; //This a variant of the column format
	final static String ALLCOLUMNSFORMATSTR = "ALLCOLUMNS"; //IS THIS NECESSARY?

	final static String TARGETTAXONOMYSTR = "target";
	final static String TARGETFORMATSTR = "format";
	final static String TARGETROOTSTR = "root";

	final static String ATTACHACTIONSTR = "attach";
	final static String MERGEACTIONSTR = "merge";
	final static String TRIMACTIONSTR = "trim";

	final static String COLUMNSYNTAXSTR = "columns";

	final static String ATTACHFORMATSTR = "format";
	final static String ATTACHROOTSTR = "root";   //the root of the attached tree - a new child of node named by ATTACHPARENTSTR
	final static String ATTACHPARENTSTR = "parent";
	final static String ATTACHPREFIXSTR = "prefix";
	final static String PRESERVEIDSSTR = "preserveIds";
	final static String PRESERVESYNONYMSSTR = "preserveSynonyms";

	final static String PREFIXITEMSTR = "prefix";
	final static String FILTERPREFIXITEMSTR = "filterprefix";
	
	final static String NAMESPACESUFFIX = "-namespace";
	final static String URITEMPLATESTR = "uritemplate";
	
	//These are additions to merge for column formats
	final static String SUBACTIONSTR = "action";
	final public static String XREFSUBACTION = "ADDXREFS";
	final public static String SYNSUBACTION = "ADDSYNONYMS";
	

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
		final String targetStr = getAttribute(taxonomyRoot,TARGETTAXONOMYSTR);
		final String targetFormatStr = getAttribute(taxonomyRoot,TARGETFORMATSTR);
		final String targetRootStr = getAttribute(taxonomyRoot,TARGETROOTSTR);
		final String targetPrefixStr = getAttribute(taxonomyRoot,PREFIXITEMSTR);
		final String targetFilterPrefixStr = getAttribute(taxonomyRoot,FILTERPREFIXITEMSTR);
		final TaxonStore target = getStore(targetStr, targetPrefixStr, targetFormatStr);
		logger.info("Building taxonomy to save at " + targetStr + " in the " + targetFormatStr + " format\n");
		final NodeList actions = taxonomyRoot.getChildNodes();
		for(int i=0;i<actions.getLength();i++){
			processChildNode(actions.item(i),target,targetRootStr,targetFormatStr,targetPrefixStr);
		}
		for(String reportStr : target.countTerms()){
			logger.info(reportStr);
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
			logger.info("Processing trim action: " + getAttribute(action,"node"));
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
		Map<Integer,String> synPrefixes = new HashMap<Integer,String>();  
		final String formatStr = getAttribute(action,ATTACHFORMATSTR);  //specifies format and storage model of the attached file
		final String sourceRootStr = getAttribute(action,ATTACHROOTSTR);  //specifies the root of the clade in this tree, which will be assigned a new parent
		final String targetParentStr = getAttribute(action,ATTACHPARENTSTR);  //specifies a node in the target tree, which will receive sourceRoot as a child
		final String sourcePrefixStr = getAttribute(action,ATTACHPREFIXSTR);
		final String preserveIDsStr = getAttribute(action,PRESERVEIDSSTR);
		final String preserveSynonymsStr = getAttribute(action,PRESERVESYNONYMSSTR);
		final List<ColumnType> columns = processAttachElement(action,synPrefixes);
		Merger m = getMerger(formatStr,columns,synPrefixes);
		if (!m.canAttach()){
			throw new RuntimeException("Error - Merger for format " + formatStr + " can't attach branches to the tree");
		}
		if (!m.canPreserveID() && (sourcePrefixStr != null) && (!sourcePrefixStr.equals(targetPrefixStr))){
			throw new RuntimeException("Error - Merger for format " + formatStr + " can't preserve id prefixes - remove prefix attribute in attach");
		}
		if (m.canPreserveID() && null!=preserveIDsStr){
			if ("true".equalsIgnoreCase(preserveIDsStr) || "yes".equalsIgnoreCase(preserveIDsStr))
				m.setPreserveID(true);
			else
				m.setPreserveID(false);
		}
		String sourceStr = action.getAttributes().getNamedItem("source").getNodeValue();
		File sourceFile = getSourceFile(sourceStr);
		logger.info("Attaching taxonomy from " + sourceStr);
		if (targetPrefixStr == null){
			logger.warn("No prefix for newly generated ids specified - will default to filename component");
			targetPrefixStr = sourceFile.getName();
		}
		m.setPreserveSynonyms(processSynonymSourceAttribute(getAttribute(action,PRESERVESYNONYMSSTR)));
			
		m.setSource(sourceFile);
		m.setTarget(target);
		m.attach(targetParentStr,sourceRootStr,sourcePrefixStr);
	}
	
	
	boolean processBooleanAttribute(String attStr){
		if (attStr == null)
			return false;
		if ("yes".equalsIgnoreCase(attStr))
			return true;
		if ("true".equalsIgnoreCase(attStr))
			return true;
		return false;
	}
	
	SynonymSource processSynonymSourceAttribute(String synSourceStr){
		if ("Neither".equalsIgnoreCase(synSourceStr) || "None".equalsIgnoreCase(synSourceStr))
			return SynonymSource.NEITHER;
		if ("Target".equalsIgnoreCase(synSourceStr))
			return SynonymSource.TARGET;
		if ("Both".equalsIgnoreCase(synSourceStr))
			return SynonymSource.BOTH;
		if (synSourceStr == null || "Source".equalsIgnoreCase(synSourceStr))
			return SynonymSource.SOURCE;
		throw new RuntimeException("Unrecognized Synonym processing attribute for Merge: " + synSourceStr);
	}

	private void processMergeAction(Node action, TaxonStore target, String targetPrefixStr){
		final Map<Integer,String> synPrefixes = new HashMap<Integer,String>(); 
		final List<ColumnType>columns = processAttachElement(action,synPrefixes);
		final String formatStr = getAttribute(action,"format");
		final Merger m = getMerger(formatStr,columns,synPrefixes);
		final String sourceStr = getAttribute(action,"source");
		final String mergePrefix = getAttribute(action,PREFIXITEMSTR);
		final String subAction = getAttribute(action,SUBACTIONSTR);
		final String uriTemplate = getAttribute(action,URITEMPLATESTR);

		if (!"".equals(sourceStr)){
			m.setSource(getSourceFile(sourceStr));
			logger.info("Merging names from " + sourceStr);
		}
		else
			m.setSource(null); //CoL doesn't specify a fixed URL, we're not loading from one source - maybe this is too much of a special case
		m.setTarget(target);
		if (subAction == null){
			m.setSubAction("SYNSUBACTION");
		}
		else {
			m.setSubAction(subAction.toUpperCase());
		}
		if (uriTemplate != null){
			m.setURITemplate(uriTemplate);
		}
		if (mergePrefix == null){
			m.merge(targetPrefixStr);
		}
		else {
			m.merge(mergePrefix);
		}
	}


	@SuppressWarnings("unchecked")
	final List<ColumnType> emptyColumns = (List<ColumnType>)Collections.EMPTY_LIST;
	
	private List<ColumnType> processAttachElement(Node action,Map<Integer,String> synPrefixes){
		final NodeList childNodes = action.getChildNodes();
		if (childNodes.getLength()>0){
			return processChildNodesOfAttach(childNodes,synPrefixes);
		}
		else {
			return emptyColumns;
		}
	}

	private List<ColumnType> processChildNodesOfAttach(NodeList childNodes, Map<Integer, String> synPrefixes) {
		List<ColumnType> result = new ArrayList<ColumnType>();
		for(int i = 0; i<childNodes.getLength();i++){
			Node child = childNodes.item(i);
			String childName = child.getNodeName();
			if (COLUMNSYNTAXSTR.equals(childName)){
				int columnCount = 0;  //because not all children are column elements
				NodeList columnElements = child.getChildNodes();
				for(int j = 0; j<columnElements.getLength();j++){
					final Node column = columnElements.item(j);
					if (column.getNodeType() == Node.ELEMENT_NODE){
						result.add(processColumnElement(column,columnCount,synPrefixes));
						columnCount++;
					}
				}
			}
			else if (Node.TEXT_NODE != child.getNodeType()){
				logger.warn("Unknown subelement" + child);
			}
		}
		return result;
	}
	
	
	
	private ColumnType processColumnElement(Node column, int index, Map<Integer, String> synPrefixes){
		if (column.getAttributes().getNamedItem("type") != null){
			final ColumnType col = new ColumnType(column.getAttributes().getNamedItem("type").getNodeValue());
			if (column.getAttributes().getNamedItem("name") != null)
				col.setName(column.getAttributes().getNamedItem("name").getNodeValue());
			else
				col.setName("Column " + Integer.toString(index));
			if (column.getAttributes().getNamedItem(PREFIXITEMSTR) != null){
				String synPrefix = column.getAttributes().getNamedItem(PREFIXITEMSTR).getNodeValue();
				synPrefixes.put(index, synPrefix);
			}
			return col;
		}
		else
			throw new RuntimeException("Column " + index + " has no type specified");
	}

	
	private TaxonStore getStore(String targetStr, String prefixStr, String formatStr) {
		File targetFile = getSourceFile(targetStr);
		if (targetFile.exists())
			targetFile.delete();
		if (OBOFORMATSTR.equals(formatStr)){
			return new OBOStore(targetFile.getAbsolutePath(), prefixStr, prefixStr.toLowerCase() + NAMESPACESUFFIX);
		}
		if (OWLFORMATSTR.equals(formatStr)){
			try{
				return new OWLStore(targetFile.getAbsolutePath(), prefixStr, prefixStr.toLowerCase() + NAMESPACESUFFIX);
			} catch (OWLOntologyCreationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		// these source formats aren't storage formats (there's no ontology library for them) so the store is implementation dependent (currently OBO)
		if (XREFFORMATSTR.equals(formatStr) || //XREF isn't a storage format, so the store is 
				COLUMNFORMATSTR.equals(formatStr) ||
				SYNONYMFORMATSTR.equals(formatStr) ||
				ALLCOLUMNSFORMATSTR.equals(formatStr)){      
			return new OBOStore(targetFile.getAbsolutePath(), prefixStr, prefixStr.toLowerCase() + NAMESPACESUFFIX);
		}
		logger.error("Format " + formatStr + " not supported for merging");
		return null;
	}


	private Merger getMerger(String formatStr, List<ColumnType> columns, Map<Integer, String> synPrefixes){
		if (OBOFORMATSTR.equalsIgnoreCase(formatStr))
			return new OBOMerger();
		if (ITISFORMATSTR.equalsIgnoreCase(formatStr))
			return new ITISMerger();
		if (NCBIFORMATSTR.equalsIgnoreCase(formatStr))
			return new NCBIMerger();
		if (CSVFORMATSTR.equalsIgnoreCase(formatStr)){
			ColumnMerger result = new ColumnMerger(",");
			result.setColumns(columns);
			return (Merger)result;
		}
		if (TSVFORMATSTR.equalsIgnoreCase(formatStr)){
			ColumnMerger result = new ColumnMerger("\t");
			result.setColumns(columns);
			return (Merger)result;
		}
		if (PIPESEPARATEDSTR.equalsIgnoreCase(formatStr)){
			ColumnMerger result = new ColumnMerger("|");
			result.setColumns(columns);
			return (Merger)result;
		}
		if (JOINEDNAMETABBEDCOLUMNS.equalsIgnoreCase(formatStr)){
			UnderscoreJoinedNamesMerger result = new UnderscoreJoinedNamesMerger("\t");
			result.setColumns(columns);
			return (Merger)result;
		}
		if (OWLFORMATSTR.equalsIgnoreCase(formatStr)){
			return new OWLMerger();
		}
		if (IOCFORMATSTR.equalsIgnoreCase(formatStr)){
			return new IOCMerger();
		}
		if (COLFORMATSTR.equalsIgnoreCase(formatStr)){
			return new CoLMerger();
		}
		if (COLDBFORMATSTR.equalsIgnoreCase(formatStr)){
			return new CoLDBMerger();
		}
		if (PBDBBULKFORMATSTR.equalsIgnoreCase(formatStr)){
			return new PaleoDBBulkMerger();
		}
		if (PBDBPOSTPROCESSSTR.equalsIgnoreCase(formatStr)){
			return new PBDBPostProcess();
			
		}
		logger.error("Format " + formatStr + " not supported for merging");
		return null;
	}

	//Requiring URL's was unnecessary - now will allow local files
	private File getSourceFile(String sourceStr) {
		URL u;
		try{
			u = new URL(sourceStr);
			if (!"file".equals(u.getProtocol())){
				System.err.println("Can only load from a local file");
				return null;
			}
			return new File(u.getFile());
		}
		catch (MalformedURLException e) {
			return new File(sourceStr);
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
		else if (ALLCOLUMNSFORMATSTR.equals(targetFormatStr)){
			target.saveAllColumnFormat(targetFormatStr);
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




