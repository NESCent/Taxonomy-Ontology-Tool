package org.nescent.VTO.lib;

public class ColumnType {
	private String name;
	private final String type;
	private String xrefPrefix;

	public ColumnType(String colType){
		type = colType;
	}
	
	public String getName(){
		return name;
	}


	public String getType(){
		return type;
	}

	public void setName(String n){
		name = n;
	}
	
	public String getXrefPrefix(){
		return xrefPrefix;
	}

	public void setXrefPrefix(String p){
		xrefPrefix = p;
	}
	

}



