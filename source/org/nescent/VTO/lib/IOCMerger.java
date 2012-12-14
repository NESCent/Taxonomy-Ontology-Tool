package org.nescent.VTO.lib;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.nescent.VTO.Builder;
import org.obo.datamodel.IdentifiedObject;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class IOCMerger implements Merger {

	private File source;
	private TaxonStore target;
	private SynonymSource preserveSynonyms;
	private String subAction = Builder.SYNSUBACTION;  // default (currently only implemented) behavior is to merge synonyms
	private boolean updateObsoletes = false;
	
	static final Logger logger = Logger.getLogger(Builder.class.getName());


	@Override
	public boolean canAttach() {
		return true;
	}

	@Override
	public boolean canPreserveID(){
		return false;   //No ids apart from names in this format
	}

	@Override
	public void setSource(File sourceFile){
		source = sourceFile;
	}
	
	public void setTarget(TaxonStore targetStore){
		target = targetStore;
	}
	
	@Override
	public void setPreserveID(boolean v){
		throw new RuntimeException("This merger can't preserve IDs because IOC provides no identifiers apart from names");
	}
	
	@Override
	public void setPreserveSynonyms(SynonymSource s){
		preserveSynonyms = s;
	}

	@Override
	public void setUpdateObsoletes(boolean v){
		updateObsoletes = v;
	}

	/**
	 * @param sa specifies whether this merges synonyms or cross references
	 */
	@Override
	public void setSubAction(String sa){
		if (Builder.XREFSUBACTION.equals(sa)){
			throw new IllegalArgumentException("Xref merging not currently supported by IOCMerger");
		}
		subAction = sa;
	}

	@Override
	public void setURITemplate(String template) {
		// TODO Auto-generated method stub
		
	}



	@Override
	public void merge(String prefix) {
		ItemList items;
		try {
			items = processXML(source);
		}
		catch (IOException e){
			logger.fatal("Error processing file: " + source.toString());
			e.printStackTrace();
			return;
		}
		// TODO Auto-generated method stub

	}

	@Override
	public void attach(String attachment, String cladeRoot, String prefix) {
		ItemList items;
		try {
			items = processXML(source);
		}
		catch (IOException e){
			logger.fatal("Error processing file: " + source.toString());
			e.printStackTrace();
			return;
		}
		final Map<String,IdentifiedObject> classIDs = new HashMap<String,IdentifiedObject>();
		IdentifiedObject rootClass = null;
		Term parentTerm = null;
		if (!"".equals(attachment)){
			parentTerm = target.getTermbyName(attachment);
			if (parentTerm == null){   //parent is unknown
				if (!target.isEmpty()){
					System.err.println("Can not attach " + source.getAbsolutePath() + " specified parent: " + attachment + " is unknown to " + target);
					return;
				}
				else { // attachment will be added first to provide a root for an otherwise empty target
					parentTerm = target.addTerm(attachment, prefix);
					logger.info("Assigning " + attachment + " as root");
				}
			}
		}
		if (!cladeRoot.equals(attachment)){
			Term cladeTerm = target.addTerm(cladeRoot, prefix);
			target.attachParent(cladeTerm, parentTerm);
			if (cladeRoot.equals("Aves")){
				target.setRankFromName(cladeTerm, KnownField.CLASS.getCannonicalName());
			}
			parentTerm = cladeTerm;
		}

		if (items.hasColumn(KnownField.ORDER)){
			for (Item it : items.getContents()){
				final String orderName = it.getName(KnownField.ORDER);
				Term orderTerm = target.getTermbyName(orderName);
				if (orderTerm == null){
					orderTerm = target.addTerm(orderName, prefix);
					target.addXRefToTerm(orderTerm,"IOC",orderName);  // could be an alternate ID?					
					target.setRankFromName(orderTerm,KnownField.ORDER.getCannonicalName());
					target.attachParent(orderTerm,parentTerm);
				}
				else if (parentTerm != null)
					target.attachParent(orderTerm, parentTerm);
			}
		}
		if (items.hasColumn(KnownField.FAMILY)){
			for (Item it : items.getContents()){
				final String familyName = it.getName(KnownField.FAMILY);
				Term familyTerm = target.getTermbyName(familyName);
				if (familyTerm == null){
					familyTerm = target.addTerm(familyName, prefix);
					target.addXRefToTerm(familyTerm,"IOC",familyName);  // could be an alternate ID?					
					target.setRankFromName(familyTerm,KnownField.FAMILY.getCannonicalName());
					if (it.hasColumn(KnownField.ORDER) && target.getTermbyName(it.getName(KnownField.ORDER)) != null){
						final String parentName = it.getName(KnownField.ORDER);
						target.attachParent(familyTerm,target.getTermbyName(parentName));
					}
					else if (parentTerm != null)
						target.attachParent(familyTerm, parentTerm);
				}
			}
		}
		if (items.hasColumn(KnownField.SUBFAMILY)){
			for (Item it : items.getContents()){
				final String subFamilyName = it.getName(KnownField.SUBFAMILY);
				Term subFamilyTerm = target.getTermbyName(subFamilyName);
				if (subFamilyTerm == null){
					subFamilyTerm = target.addTerm(subFamilyName, prefix);
					target.setRankFromName(subFamilyTerm, KnownField.SUBFAMILY.getCannonicalName());
					if (it.hasColumn(KnownField.FAMILY) && target.getTermbyName(it.getName(KnownField.FAMILY)) != null){
						final String parentName = it.getName(KnownField.FAMILY);
						target.attachParent(subFamilyTerm,target.getTermbyName(parentName));
					}
					else if (parentTerm != null)
						target.attachParent(subFamilyTerm, parentTerm);
				}
			}
		}	
		if (items.hasColumn(KnownField.GENUS)){
			for (Item it : items.getContents()){
				final String genusName = it.getName(KnownField.GENUS);
				Term genusTerm = target.getTermbyName(genusName);
				if (genusTerm == null){
					genusTerm = target.addTerm(genusName, prefix);
					target.addXRefToTerm(genusTerm,"IOC",genusName);  // could be an alternate ID?
					target.setRankFromName(genusTerm, KnownField.GENUS.getCannonicalName());
					if (it.hasColumn(KnownField.SUBFAMILY) && target.getTermbyName(it.getName(KnownField.SUBFAMILY)) != null){
						final String parentName = it.getName(KnownField.SUBFAMILY);
						target.attachParent(genusTerm,target.getTermbyName(parentName));
					}
					else if (it.hasColumn(KnownField.FAMILY) && target.getTermbyName(it.getName(KnownField.FAMILY)) != null){
						final String parentName = it.getName(KnownField.FAMILY);
						target.attachParent(genusTerm,target.getTermbyName(parentName));						
					}
					else if (parentTerm != null)
						target.attachParent(genusTerm, parentTerm);
				}
			}
		}	
		if (items.hasColumn(KnownField.SPECIES)){
			for (Item it : items.getContents()){
				final String genusName = it.getName(KnownField.GENUS);
				String capGenusName = genusName.substring(0,1).toUpperCase() + genusName.substring(1);
				final String speciesName = capGenusName + " " + it.getName(KnownField.SPECIES);
				Term speciesTerm = target.getTermbyName(speciesName); 
				if (speciesTerm == null){
					speciesTerm = target.addTerm(speciesName, prefix);
					target.addXRefToTerm(speciesTerm,"IOC",speciesName);  // could be an alternate ID?
					target.setRankFromName(speciesTerm,KnownField.SPECIES.getCannonicalName());
				}
				if (it.hasColumn(KnownField.GENUS) && target.getTermbyName(it.getName(KnownField.GENUS)) != null){
					target.attachParent(speciesTerm,target.getTermbyName(genusName));
				}
				else if (parentTerm != null)
					target.attachParent(speciesTerm, parentTerm);
				else {
					throw new RuntimeException(speciesName + " has no parent");
				}
				Collection<String> synSources = it.getSynonym_xrefs();
				for (String synSource : synSources){
					for(String syn : it.getSynonymsFromSource(synSource))
						if (true) { //!syn.equals(speciesName)){
							String[] sourceComps = synSource.split(":",2);
							SynonymI s = target.makeSynonymWithXref(syn, sourceComps[0], sourceComps[1]);
							speciesTerm.addSynonym(s);
						}
				}		
			}
		}
		if (updateObsoletes){
			target.processObsoletes();
		}
	}

	private ItemList processXML(File source) throws IOException {
		ItemList result = new ItemList();
		DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
		f.setNamespaceAware(true);
		NodeList nl = null;
		try {
			DocumentBuilder db = f.newDocumentBuilder();
			db.setErrorHandler(new DefaultHandler());
			Document d = db.parse(source);
			nl = d.getElementsByTagNameNS("","ioclist");
		} catch (ParserConfigurationException e) {
			logger.fatal("Error in initializing parser");
			e.printStackTrace();
			return result;
		} catch (SAXException e) {
			logger.fatal("Error in parsing " + source.getCanonicalPath());
			logger.fatal("Exception message is: " + e.getLocalizedMessage());
			return result;
		} 
		if (nl.getLength() != 1){
			logger.fatal("Error - More than one ioclist element in " + source.getCanonicalPath());
			return result;
		}
		
		return result;
	}
	
	private List<Item>processOrderElement(Node orderElement, Map<String,String> parents){
		List<Item> result = new ArrayList<Item>();
		String orderName = getLatinName(orderElement);
		if (orderName == null)
			logger.warn("No latin name for order");
		orderName = orderName.substring(0,1) + orderName.substring(1).toLowerCase();
		parents.put("order", orderName);
		NodeList children = orderElement.getChildNodes();
		for(int childCount = 0;childCount<children.getLength();childCount++){
			if ("family".equals(children.item(childCount).getNodeName())){
				for (Item speciesItem : processFamilyElement(children.item(childCount), parents)){
					speciesItem.putName(KnownField.ORDER,orderName);
					result.add(speciesItem);										
				}
			}
		}
		return result;
	}
	
	private List<Item>processFamilyElement(Node familyElement, Map<String,String> parents){
		List<Item> result = new ArrayList<Item>();
		String familyName = getLatinName(familyElement);
		if (familyName == null)
			logger.warn("No latin name for family");
		parents.put("family", familyName);
		NodeList children = familyElement.getChildNodes();
		for(int childCount = 0;childCount<children.getLength();childCount++){
			if ("genus".equals(children.item(childCount).getNodeName())){
				for (Item speciesItem : processGenusElement(children.item(childCount), parents)){
					speciesItem.putName(KnownField.FAMILY,familyName);
					result.add(speciesItem);					
				}
			}
		}
		return result;
	}
	
	private List<Item>processGenusElement(Node genusElement, Map<String,String> parents){
		List<Item> result = new ArrayList<Item>();
		String genusName = getLatinName(genusElement);
		if (genusName == null)
			logger.warn("No latin name for genus");
		NodeList children = genusElement.getChildNodes();
		for(int childCount = 0;childCount<children.getLength();childCount++){
			if ("species".equals(children.item(childCount).getNodeName())){
				Item speciesItem = processSpeciesElement(children.item(childCount));
				speciesItem.putName(KnownField.GENUS,genusName);
				result.add(speciesItem);
			}
		}
		
		return result;
	}

	private Item processSpeciesElement(Node speciesElement){
		Item result = new Item();
		String speciesName = getLatinName(speciesElement);
		result.putName(KnownField.SPECIES, speciesName);		
		if (speciesName == null)
			logger.warn("No latin name for species");
		return result;
	}

	
	
	private String getLatinName(Node taxonElement){
		NodeList children = taxonElement.getChildNodes();
		for(int i=0;i<children.getLength();i++){
			Node childNode = children.item(i);
			if (childNode.getNodeName().equals("latin_name")){
				NodeList nameChildren = childNode.getChildNodes();
				return nameChildren.item(0).getNodeValue();     // assume only one child here
			}
		}
		return null;
	}

	
	
	
	
}
