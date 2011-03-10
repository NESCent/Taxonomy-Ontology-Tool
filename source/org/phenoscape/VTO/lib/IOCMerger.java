package org.phenoscape.VTO.lib;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.obo.datamodel.IdentifiedObject;
import org.phenoscape.VTO.Builder;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class IOCMerger implements Merger {

		
	static final Logger logger = Logger.getLogger(Builder.class.getName());


	@Override
	public boolean canAttach() {
		return true;
	}

	@Override
	public void merge(File source, TaxonStore target, String prefix) {
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
	public void attach(File source, TaxonStore target, String attachment, String cladeRoot, String prefix) {
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
					parentTerm = target.addTerm(attachment);
					logger.info("Assigning " + attachment + " as root");
				}
			}
		}
		if (!cladeRoot.equals(attachment)){
			Term cladeTerm = target.addTerm(cladeRoot);
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
					orderTerm = target.addTerm(orderName);
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
					familyTerm = target.addTerm(familyName);
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
					subFamilyTerm = target.addTerm(subFamilyName);
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
					genusTerm = target.addTerm(genusName);
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
		if (items.hasColumn(KnownField.CLADE)){   // This was used in amphibianet
			for (Item it : items.getContents()){
				String cladeLevelName = it.getName(KnownField.CLADE);  //call it a cladeLevel to reduce ambiguity
				if (it.hasColumn(KnownField.GENUS) && target.getTermbyName(it.getName(KnownField.GENUS)) != null){
					final String parentName = it.getName(KnownField.GENUS);   //if it has a known parent (it ought to) render the parent genus parenthetically
					cladeLevelName = cladeLevelName + " (" + parentName + ")";
				}
				Term cladeLevelTerm = target.getTermbyName(cladeLevelName);
				if (cladeLevelTerm == null){
					cladeLevelTerm = target.addTerm(cladeLevelName);
					target.setRankFromName(cladeLevelTerm, KnownField.CLADE.getCannonicalName());
					if (it.hasColumn(KnownField.GENUS) && target.getTermbyName(it.getName(KnownField.GENUS)) != null){
						final String parentName = it.getName(KnownField.GENUS);
						target.attachParent(cladeLevelTerm,target.getTermbyName(parentName));
					}
					else if (parentTerm != null)
						target.attachParent(cladeLevelTerm, parentTerm);
				}
			}
		}	
		if (items.hasColumn(KnownField.SPECIES)){
			for (Item it : items.getContents()){
				final String speciesName = it.getName(KnownField.GENUS) + " " + it.getName(KnownField.SPECIES);
				Term speciesTerm = target.getTermbyName(speciesName); 
				if (speciesTerm == null){
					speciesTerm = target.addTerm(speciesName);
					target.setRankFromName(speciesTerm,KnownField.SPECIES.getCannonicalName());
				}
				if (it.hasColumn(KnownField.GENUS) && target.getTermbyName(it.getName(KnownField.GENUS)) != null){
					final String parentName = it.getName(KnownField.GENUS);
					target.attachParent(speciesTerm,target.getTermbyName(parentName));
				}
				else if (it.hasColumn(KnownField.CLADE) && target.getTermbyName(it.getName(KnownField.CLADE)) != null){
					final String parentName = it.getName(KnownField.CLADE);
					target.attachParent(speciesTerm,target.getTermbyName(parentName));
				}
				else if (parentTerm != null)
					target.attachParent(speciesTerm, parentTerm);
				if (items.hasColumn(KnownField.XREF)){
					
				}
				Collection<String> synSources = it.getSynonymSources();
				for (String synSource : synSources){
					for(String syn : it.getSynonymsForSource(synSource))
						if (true) { //!syn.equals(speciesName)){
							String[] sourceComps = synSource.split(":",2);
							SynonymI s = target.makeSynonymWithXref(syn, sourceComps[0], sourceComps[1]);
							speciesTerm.addSynonym(s);
						}
				}		
			}
		}
	}

	private ItemList processXML(File source)  throws IOException {
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
		Node ioclistRoot = nl.item(0);
		NodeList children = ioclistRoot.getChildNodes();
		int listChild = -1;
		Map<String,String> parentTable = new HashMap<String,String>();        //Level(element name) -> taxon name
		for(int childCount = 0;childCount<children.getLength();childCount++){
			if ("list".equals(children.item(childCount).getNodeName())){
				listChild = childCount;
				break;
			}
		}
		Node taxonList = children.item(listChild);
		children = taxonList.getChildNodes();
		for(int childCount = 0;childCount<children.getLength();childCount++){
			if ("order".equals(children.item(childCount).getNodeName())){
				result.addList(processOrderElement(children.item(childCount), parentTable));
			}
		}
		List<KnownField> columns = new ArrayList<KnownField>();
		columns.add(KnownField.ORDER);
		columns.add(KnownField.FAMILY);
		columns.add(KnownField.GENUS);
		columns.add(KnownField.SPECIES);
		result.addColumns(columns);
		
		return result;
	}
	
	private List<Item>processOrderElement(Node orderElement, Map<String,String> parents){
		List<Item> result = new ArrayList<Item>();
		String orderName = getLatinName(orderElement);
		orderName = orderName.substring(0,1) + orderName.substring(1).toLowerCase();
		parents.put("order", orderName);
		if (orderName == null)
			logger.warn("No latin name for order");
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
		parents.put("family", familyName);
		if (familyName == null)
			logger.warn("No latin name for family");
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
				for(int j=0;j<nameChildren.getLength();j++){
					return nameChildren.item(j).getNodeValue();
				}
			}
		}
		return null;
	}
	
	
	
	
	
	
}
