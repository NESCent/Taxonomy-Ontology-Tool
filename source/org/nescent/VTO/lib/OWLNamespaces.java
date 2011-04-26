package org.nescent.VTO.lib;


/** 
 * Modeled after the Namespaces enum in the OWLAPI
 * @author peter
 *
 */
public enum OWLNamespaces {
	
	OBO("http://purl.org/obo/owl/obo#"),
	OBOINOWL("http://www.geneontology.org/formats/oboInOwl"),
	TAXRANK("http://purl.org/obo/owl/TAXRANK#");
	
	
	String nsStr;
	
	OWLNamespaces(String ns){
		nsStr = ns;
	}
	
	public String toString(){
		return nsStr;
	}

}
