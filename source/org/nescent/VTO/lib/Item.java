/**
 * VTOTool - a tool for merging and building ontologies from multiple taxonomic sources
 * 
 * Peter Midford
 */
package org.nescent.VTO.lib;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * 
 * @author pmidford
 *
 */
public class Item {
	
	// This list ought to be constructed from TAXRANK  - perhaps need to add 'clade' to TAXRANK appears in ATO and NCBI taxonomy not sure
	// its position between genus and species is consistent across authorities
	
	private final Map<KnownField,String> names;  //maps a rank term to a taxon name
	private final Set<String> synonyms;  //synonyms w/o xrefs
	private final Map<String,Set<String>> synonyms_xref;  //maps a synonym source (xref) to a synonym  
	private final Set<String> xrefs; //xrefs and uris for the term
	private final Set<String> commonNames;
	private String comment = null;  //Maybe this should be a set (OWL can handle multiple comments, OBO?)
    private boolean is_extinct;  // may not use this
    
    public Item(){
        names = new HashMap<KnownField,String>();
        synonyms = new HashSet<String>();
        synonyms_xref = new HashMap<String,Set<String>>();
        commonNames = new HashSet<String>();
        xrefs = new HashSet<String>();
        is_extinct = false;
    }
    
    public void putName(KnownField rank, String name){
    	names.put(rank, name);
    }
    
    public boolean hasColumn(KnownField rank){
    	if (!rank.isTaxon())
    		throw new IllegalArgumentException("Unknown rank");
    	return names.containsKey(rank);
    }

    public String getName(KnownField rank){
    	if (!rank.isTaxon())
    		throw new IllegalArgumentException("Unknown rank");
    	return names.get(rank);
    }
    
    public String getFieldValue(KnownField field){
    	return names.get(field);
    }
    
    
    public void addSynonymWithXref(String source, String name, String identifier, ItemList container){
    	String dbSource;
    	if ("".equals(identifier)){
    		if (container.hasSynonymSourcecontainsKey(source))
    			dbSource = container.getSynonymSource(source);
    		else
    			dbSource = source;
    	}
    	else
    		dbSource = source + ":" + identifier;
    	Set<String> contents;
    	if (synonyms_xref.containsKey(dbSource))
    		contents = synonyms_xref.get(dbSource);
    	else{
    		contents = new HashSet<String>();
    		synonyms_xref.put(dbSource, contents);
    	}
    	contents.add(name);
    }
    
    public Collection<String> getSynonym_xrefs(){
    	return synonyms_xref.keySet();
    }
    
    public Set<String> getPlainSynonyms(){
    	return synonyms;
    }

    public void addPlainSynonym(String rawColumn) {
    	synonyms.add(rawColumn);
	}
    
    public void addVernacular(String name){
    	commonNames.add(name);
    }
    
    public Set<String> getVernacularNames(){
    	return commonNames;
    }
    
    public Set<String> getTermXRefs(){
    	return xrefs;
    }
    
    public Collection <String> getSynonymsForSource(String source){
    	if (synonyms_xref.containsKey(source))
    		return synonyms_xref.get(source);
    	else
    		return Collections.emptySet();
    }
    
    public void addXref(String xref){
    	xrefs.add(xref);
    }
    
    public void setComment(String c){
    	comment = c;
    }
    
    public String getComment(){
    	return comment;
    }
    
    
    public String toString(){
    	StringBuilder b = new StringBuilder(200);
    	for (KnownField field : KnownField.values()){
    		if (field.isTaxon()){
    			if (b.length() > 0)
    				b.append("; ");
    			if (names.containsKey(field)){
    				b.append(field + ": " + names.get(field));
    			}
    		}
    	}
    	if (!synonyms_xref.isEmpty()  || !synonyms.isEmpty()){
    		b.append("; Synonyms: ");
    		for(Entry<String, Set<String>> e : synonyms_xref.entrySet()){
    			b.append( e.getKey()+ ": ");
    			for(String syn : e.getValue()){
    				b.append(syn + ", ");
    			}
    		}
    		for(String syn : synonyms){
    			b.append(":" + syn + ", ");
    		}
    	}
    	if (!xrefs.isEmpty()){
    		b.append("; xrefs: ");
    		for(String x : xrefs){
    			b.append(x);
    			b.append("; ");
    		}
    	}
    	if (b.toString().length() < 2)
    		return "Item is empty";
    	else
    		return b.toString();
    }



}
