package org.nescent.VTO.lib;

public class ColumnType {
	private final String name;
	private String type;
	private String xrefPrefix;
	private String uri;

	public ColumnType(String colName){
		name = colName;
	}
	
	public String getName(){
		return name;
	}


	public String getType(){
		return type;
	}

	public void setType(String t){
		type = t;
	}
	public String getXrefPrefix(){
		return xrefPrefix;
	}

	public void setXrefPrefix(String p){
		xrefPrefix = p;
	}
	
	public String getURI(){
		return uri;
	}

	public void setURI(String u){
		uri = u;
	}


}



