package org.phenoscape.VTO.lib;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.obo.datamodel.OBOClass;

public class ITISMerger implements Merger{

    static final Pattern pipePattern = Pattern.compile("\\|");   //try this pattern as it matches the documentation

    @Override
    public void merge(File itisSource, TaxonStore theStore, String prefix) {
    	final Map<Integer,ITISElement> taxonTable = new HashMap<Integer,ITISElement>();
    	final List <ITISElement> itisList = buildITISList(itisSource);
    	fillTaxonTable(itisList,taxonTable);
    	gatherSynonyms(itisList,taxonTable);

        final Collection<Term> terms = theStore.getTerms();	

    }
    
	private List<ITISElement> buildITISList(File nf){
		List <ITISElement> result = new ArrayList<ITISElement>();
        try {
            final BufferedReader br = new BufferedReader(new FileReader(nf));
            String raw = br.readLine();
            while (raw != null){
            	ITISElement e = processLine(raw);
            	result.add(e);
            	raw = br.readLine();
            }
        }
        catch (Exception e) {
            System.out.print(e);
            return result;
        }
        return result;
	}

    private ITISElement processLine(String raw){
        final String[] digest = pipePattern.split(raw);
        String eTypeStr = digest[0];
        ElementType eType = ElementType.NOTE;
        if ("[TU]".equals(eTypeStr)){
        	eType = ElementType.TAXONUNIT;
        }
        if ("[SY]".equals(eTypeStr)){
        	eType = ElementType.SYNONYM;
        }
        if ("[TA]".equals(eTypeStr)){
        	eType = ElementType.AUTHOR;
        }
        ITISElement result = new ITISElement(eType);
        switch (eType) {
        case TAXONUNIT: {
            String idStr = digest[1];
            if (idStr != null){
            	try {
            		int idNum = Integer.parseInt(idStr);
            		result.setID(idNum);
            	}
            	catch (NumberFormatException e){
            	}
            }
        	String name1 = digest[3];
        	String name2 = digest[4];
        	String taxonomicStatus = digest[11];
        	String itisStatus = digest[13];
        	if (name2 != null){
        		name1 = name1 + name2;
        	}
        	result.setName(name1);
        	result.makeSynTable();
        	break;
        }
        case SYNONYM: {
            String idStr = digest[2];
            if (idStr != null){
            	try {
            		int idNum = Integer.parseInt(idStr);
            		result.setID(idNum);
            	}
            	catch (NumberFormatException e){
            	}
            }
            String idStr2 = digest[3];
            if (idStr != null){
            	try {
            		int idNum = Integer.parseInt(idStr2);
            		result.setID2(idNum);
            	}
            	catch (NumberFormatException e){
            	}
            }
        	break;
        }
        case AUTHOR: {
            String idStr = digest[1];
            if (idStr != null){
            	try {
            		int idNum = Integer.parseInt(idStr);
            		result.setID(idNum);
            	}
            	catch (NumberFormatException e){
            	}
            }
            result.setAuthorString(digest[2]);
            result.setPubCode(digest[3]);
         	break;
        }
        }
        
    	return result;
    }
    
    private void fillTaxonTable(List<ITISElement> itisList,Map<Integer,ITISElement> taxonTable){
    	for(ITISElement e : itisList){
    		if (e.getType() == ElementType.TAXONUNIT){
    			taxonTable.put(e.getID(), e);
    		}
    	}
    }
    
    private void gatherSynonyms(List<ITISElement> itisList,Map<Integer,ITISElement> taxonTable){
    	for(ITISElement e : itisList){
    		if (e.getType() == ElementType.SYNONYM){
    			ITISElement primary = taxonTable.get(e.getID2());
    			ITISElement syn = taxonTable.get(e.getID());
    			if (primary == null || syn == null){
    				System.err.println("Couldn't add synonym; primary id = " + e.getID2() + "; primary = " 
    						+ primary + "; synonym id = " + e.getID() + "; synonym = " + syn);
    			}
    			else
    				primary.addSynonym(syn);
    		}
    	}    	
    }
    
	@Override
	public boolean canAttach() {
		return true;
	}


	@Override
	public void attach(File itisSource, TaxonStore target, String parent, String prefix) {
    	final Map<Integer,ITISElement> taxonTable = new HashMap<Integer,ITISElement>();
    	final List <ITISElement> itisList = buildITISList(itisSource);
    	fillTaxonTable(itisList,taxonTable);
    	gatherSynonyms(itisList,taxonTable);
		// TODO finish
		
	}

	
	enum ElementType{
		TAXONUNIT,
		SYNONYM,
		AUTHOR,
		NOTE
	}
	
	
	static class ITISElement{
		private ElementType etype;
		private int id;
		private int id2;
		private String name;
		private List<ITISElement> synonyms;
		
		ITISElement(ElementType e){
			etype = e;
		}
		
		ElementType getType(){
			return etype;
		}
		
		int getID(){
			return id;
		}
		
		void setID(int i){
			id = i;
		}
		
		int getID2(){
			return id2;
		}
		
		void setID2(int i){
			id2 = i;
		}
		
		String getName(){
			return name;
		}
		
		void setName(String s){
			name = s;
		}
		
		String getAuthorString(){
			return name;
		}
		
		void setAuthorString(String s){
			name = s;
		}
		
		String getPubCode(){
			return name;
		}
		
		void setPubCode(String s){
			name = s;
		}
		
		void makeSynTable(){
			synonyms = new ArrayList<ITISElement>();
		}
		
		void addSynonym(ITISElement e){
			if (ElementType.TAXONUNIT == getType()){
				synonyms.add(e);
			}
			else
				throw new IllegalArgumentException("Can't add a synonym to element of type " + getType());
		}
		
		List<ITISElement> getSynonyms(){
			if (ElementType.TAXONUNIT == getType()){
				return synonyms;
			}
			else
				throw new IllegalArgumentException("Can't retrieve synonyms from element of type " + getType());
		}
	
	}



}
