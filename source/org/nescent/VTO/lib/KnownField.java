package org.nescent.VTO.lib;


/**
 * @author pmidford
 */
public enum KnownField {
	CLASS("class",true),
	ORDER("order",true),
	FAMILY("family",true),
	SUBFAMILY("subfamily",true),
	GENUS("genus",true),
	SUBGENUS("subgenus",true),
	SPECIES("species",true),
	SYNONYM("synonym",false),
	VERNACULAR("vernacular",false),
	COMMENT("comment",false),
	STATUS("status",false),
	URI("uri",false),
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

