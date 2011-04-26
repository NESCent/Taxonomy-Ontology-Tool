package org.nescent.VTO.lib;


//Ideally this should be derived in some fashion from the taxrank ontology.  This would probably require tricky class design

public enum KnownField {
	CLASS("class",true),
	ORDER("order",true),
	FAMILY("family",true),
	SUBFAMILY("subfamily",true),
	GENUS("genus",true),
	CLADE("clade",true),
	SPECIES("species",true),
	SYNONYM("",false),
	SYNONYMS("",false),
	DESCRIPTION("",false),
	STATUS("",false),
	XREF("",false),
	DELIMITEDNAME("",false),
	IGNORE("",false);
	
	private final String cannonicalName;
	private final boolean isTaxon;
	
	private KnownField(String c, boolean t){
		cannonicalName = c;
		isTaxon = t;
	}
	
	public String getCannonicalName() {return cannonicalName;}
	public boolean isTaxon() {return isTaxon;}
	
	
}

