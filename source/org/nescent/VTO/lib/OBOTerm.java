package org.nescent.VTO.lib;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.obo.datamodel.Dbxref;
import org.obo.datamodel.Link;
import org.obo.datamodel.OBOClass;
import org.obo.datamodel.OBOProperty;
import org.obo.datamodel.PropertyValue;
import org.obo.datamodel.Synonym;
import org.semanticweb.owlapi.model.OWLIndividual;

public class OBOTerm implements Term {
	
	private OBOClass term;

	public OBOTerm(OBOClass cl) {
		term = cl;
	}
	

	public SynonymI getOldSynonym(String dbID){
		Synonym old = null; 
		for (Dbxref dx : term.getDbxrefs()){
			if (dbID.equals(dx.getDatabase())){
				old = dx.getSynonym();
				return new OBOSynonym(old);
			}
		}
		return null;
	}

	
	public void addSynonym(SynonymI s){
		final Synonym newSyn = s.asOBOSynonym();
		for (Synonym existingSyn : term.getSynonyms()){
			if (matchSynonyms(existingSyn,newSyn)){
				return;
			}
		}
		term.addSynonym(s.asOBOSynonym());
	}
	
	private boolean matchSynonyms(Synonym oldSyn, Synonym newSyn){
		if (!oldSyn.getText().equals(newSyn.getText()))
			return false;
		final Collection<Dbxref> oldXRefs = oldSyn.getXrefs();
		final Collection<Dbxref> newXRefs = newSyn.getXrefs();
		if (newXRefs.size()==0)
			return true;  //If the new syn has no xref, but the names match, it has nothing to add...  
		if (newXRefs.size() > 1){  //neither syn should have more than one xref, but certainly not the new one
			throw new RuntimeException("New synonym: '" + newSyn.getText() + "' should have an most one xref; " + newXRefs.size() + " were found");
		}
		for (Dbxref newRef : newXRefs){  //only one newRef; if it matches any existing ref, it's a match (saves creating an iterator for the singleton)
			for (Dbxref d : oldXRefs){
				if (d.getDatabase().equals(newRef.getDatabase()) &&
					(d.getDatabaseID().equals(newRef.getDatabaseID()))){
					return true;
				}
			}
		}	
		return false;
	}
	
	
	
	public Set<SynonymI> getSynonyms(){
		Set<SynonymI> result = new HashSet<SynonymI>();
		for (Synonym s : term.getSynonyms()){
			result.add(new OBOSynonym(s));
		}
		return result;
	}


	@Override
	public OBOClass asOBOClass() {
		return term;
	}

	public OWLIndividual asOWLIndividualTerm(){
		return null;
	}

	
	@Override
	public String getLabel() {
		return term.getName();
	}
	
	@Override
	public String getID(){
		return term.getID();
	}


	@Override
	public Set<Term> getChildren() {
		Set<Term> results = new HashSet<Term>();
		Collection<Link> childLinks = term.getChildren();
		for(Link l : childLinks){
			OBOProperty lType = l.getType();
			if (OBOUtils.ISA_PROPERTY.equals(lType.getID())){
				OBOClass child = (OBOClass)l.getChild();
				results.add(new OBOTerm(child));
			}
		}
		return results;
	}
	
	
	public void removeParent(Term parent){
		final Collection<Link> childLinks = parent.asOBOClass().getChildren();
		Link targetLink = null;
		for(Link l : childLinks){
			OBOProperty lType = l.getType();
			if (OBOUtils.ISA_PROPERTY.equals(lType.getID())){
				OBOClass childClass = (OBOClass)l.getChild();
				if (asOBOClass().equals(childClass)){
					targetLink = l;
				}
			}
		}
		if (targetLink != null){
			parent.asOBOClass().removeChild(targetLink);
		}
	}
	
	

	public List<Term> getAncestors(){
		List<Term> results = new ArrayList<Term>();
		OBOClass parent = asOBOClass();
		while (parent != null ){
			boolean found = false;
			Iterator<Link> linkIter = parent.getParents().iterator();
			while(linkIter.hasNext() && !found){
				Link nextLink = linkIter.next();
				OBOProperty lType = nextLink.getType();
				if (OBOUtils.ISA_PROPERTY.equals(lType.getID())){
					parent = (OBOClass)nextLink.getParent();
					found = true;
					break;
				}
			}
			if (!found){
				parent = null;
			}
			else{
				results.add(new OBOTerm(parent));
			}
		}
		return results;
	}
	
	@Override
	public Set<String> getCrossReferences() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean hasAncestor(String string) {
		
		// TODO Auto-generated method stub
		return false;
	}


	@Override
	public boolean isExtinct() {
		for (PropertyValue v : asOBOClass().getPropertyValues()){
			String[] pair = v.getValue().split(" ");
			if ("is_extinct".equals(pair[0])){
				return true;
			}
		}
		return false;
	}

	@Override
	public void createAltID(String id){
		term.addSecondaryID(id);
	}


	@Override
	public boolean isObsolete() {
		return asOBOClass().isObsolete();
	}

	public void removeProperties(){
		Set <PropertyValue> copy = new HashSet<PropertyValue>();
		copy.addAll(asOBOClass().getPropertyValues());
		for (PropertyValue v : copy){
			asOBOClass().removePropertyValue(v);
		}
	}

	public void setComment(String s){
		term.setComment(s);
	}
	
}
