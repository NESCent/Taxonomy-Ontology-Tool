package org.nescent.VTO.lib;


//Ideally this should be derived in some fashion from the taxrank ontology.  This would probably require tricky class design

/**
 * @author pmidford
 */
public enum KnownField {
	CLASS("class",true),
	ORDER("order",true),
	FAMILY("family",true),
	SUBFAMILY("subfamily",true),
	GENUS("genus",true),
	SPECIES("species",true),
	SYNONYM("synonym",false),
	SYNONYMS("synonyms",false),
	DESCRIPTION("description",false),
	STATUS("status",false),
	XREF("xref",false),
	DELIMITEDNAME("delimitedname",false),
	IGNORE("ignore",false);
	
	private final String cannonicalName;
	private final boolean isTaxon;
	
	private KnownField(String c, boolean t){
		cannonicalName = c;
		isTaxon = t;
	}
	
	public String getCannonicalName() {return cannonicalName;}
	public boolean isTaxon() {return isTaxon;}
	
	
}

