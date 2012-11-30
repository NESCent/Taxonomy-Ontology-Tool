package org.nescent.VTO.lib;

import org.apache.log4j.Logger;

public class ColumnType {
	private String name;
	private final String type;
	private String xrefPrefix;
	private KnownField fieldType;

	
	static final Logger logger = Logger.getLogger(ColumnType.class.getName());
	
	public ColumnType(String colType){
		type = colType;
		boolean matched = false;
		for(KnownField k : KnownField.values()){
			String knownType = k.getCannonicalName();
			if (knownType.equalsIgnoreCase(colType)){
				fieldType = k;
				matched = true;
				break;
			}
		}
		if (!matched){
			logger.error("Unknown column type specified: " + colType);
			fieldType=KnownField.IGNORE;
		}
	}
	
	public String getName(){
		return name;
	}


	public String getType(){
		return type;
	}
	
	public KnownField getFieldType(){
		return fieldType;
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



