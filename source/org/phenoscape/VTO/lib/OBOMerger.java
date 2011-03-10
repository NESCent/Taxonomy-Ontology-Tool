package org.phenoscape.VTO.lib;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.log4j.Logger;
import org.obo.dataadapter.DefaultOBOParser;
import org.obo.dataadapter.OBOParseEngine;
import org.obo.dataadapter.OBOParseException;
import org.obo.datamodel.Dbxref;
import org.obo.datamodel.Link;
import org.obo.datamodel.OBOClass;
import org.obo.datamodel.OBOProperty;
import org.obo.datamodel.OBOSession;
import org.obo.datamodel.Synonym;
import org.obo.datamodel.impl.DefaultObjectFactory;
import org.obo.util.TermUtil;

public class OBOMerger implements Merger {
	
	static final Logger logger = Logger.getLogger(OBOMerger.class.getName());

	private OBOUtils u = null;
	
	String defaultPrefix;
	private String idSuffix = ":%07d";
	private String defaultFormat;


	@Override
	public boolean canAttach() {
		// TODO Auto-generated method stub
		return true;
	}

	@Override
	public void merge(File source, TaxonStore target, String prefix) {
		logger.info("Loading OBO file " + source);
		u = new OBOUtils(source.getAbsolutePath());
		logger.info("Finished loading");
		int termCount = 0;
		int synCount = 0;
		Collection <OBOClass> allTerms = u.getTerms();
		for (OBOClass term : allTerms){
			Term matchingTerm = target.getTermbyName(term.getName());
			if (matchingTerm != null){
				termCount++;
				//SynonymI s = target.makeSynonymWithXref(term.getName(), prefix, term.getID());
				//matchingTerm.addSynonym(s);
				// add synonyms from term
				for (Synonym syn : term.getSynonyms()){
					Collection<Dbxref> xrefs = syn.getXrefs();
					for (Dbxref ref : xrefs){  //need to avoid common names....
						SynonymI newSyn = target.makeSynonymWithXref(syn.getText(), ref.getDatabase(), ref.getDatabaseID());
						matchingTerm.addSynonym(newSyn);
						synCount++;
					}
				}
			}
			if (termCount % 1000 == 0){
				logger.info("Processed " + termCount + " terms; added " + synCount + " synonyms");
			}
		}
	}

	@Override
	public void attach(File source, TaxonStore target, String attachment, String cladeRoot, String prefix) {
		logger.info("Loading OBO file " + source);
		u = new OBOUtils(source.getAbsolutePath());
		logger.info("Finished loading");
		Term parentTerm = null;
		OBOClass cladeClass = u.lookupTermByName(cladeRoot);
		parentTerm = target.getTermbyName(attachment);
		if (parentTerm == null){   //parent is unknown
			if (!target.isEmpty()){
				logger.error("Can not attach " + source.getAbsolutePath() + " specified parent: " + attachment + " is unknown to " + target);
				return;
			}
			else { // attachment will be added first to provide a root for an otherwise empty target
				parentTerm = target.addTerm(attachment);
				logger.info("Assigning " + attachment + " as root");
			}
		}
		if (cladeClass == null){
			logger.error("Clade root to attach " + cladeRoot + " was not found");
			return;
		}
		// first copy all the descendents of cladeClass into the target
		Term cladeTerm = target.addTerm(cladeClass.getName());
		String[] idFields = cladeClass.getID().split(":");
		if (idFields.length == 2){
			target.addXRefToTerm(cladeTerm,idFields[0],idFields[1]);  // could be an alternate ID?
		}
		else{
			logger.warn("Could not split OBOID " + cladeClass.getID() + " to generate xref in target term");
		}
		if (u.getRankString(cladeClass) != null)
			target.setRankFromName(cladeTerm, u.getRankString(cladeClass));
		target.attachParent(cladeTerm, parentTerm);
		addChildren(cladeClass,cladeTerm,target);
		//u.setNameSpace(oboNameSpace, fileSpec);
		defaultPrefix = prefix;
		defaultFormat = defaultPrefix + idSuffix;
		
		//targetFile = fileSpec;
		//fillRankNames();

	}

	// Note: parentClass is from the obo tree being attached, parentTerm is the copy in the target tree
	// so parentTerm.asOBOClass != parentClass
	private void addChildren(OBOClass parentClass, Term parentTerm, TaxonStore target){
		final Collection<Link> childLinks = parentClass.getChildren();
		for(Link l : childLinks){
			OBOProperty lType = l.getType();
			if (OBOUtils.ISA_PROPERTY.equals(lType.getID())){
				OBOClass childClass = (OBOClass)l.getChild();
				Term childTerm = target.addTerm(childClass.getName());
				String[] idFields = childClass.getID().split(":");
				if (idFields.length == 2){
					target.addXRefToTerm(childTerm,idFields[0],idFields[1]);  // could be an alternate ID?
				}
				else{
					logger.warn("Could not split OBOID " + childClass.getID() + " to generate xref in target term");
				}
				if (u.getRankString(childClass) != null)
					target.setRankFromName(childTerm, u.getRankString(childClass));
				for (Synonym syn : childClass.getSynonyms()){
					String synText = syn.getText();
					Collection <Dbxref> xrefs = syn.getXrefs();
					if (xrefs != null && !xrefs.isEmpty()){
						for (Dbxref xref : xrefs){
							SynonymI newSyn = target.makeSynonymWithXref(synText, xref.getDatabase(), xref.getDatabaseID());	
							childTerm.addSynonym(newSyn);
						}
					}
					else {  // no xrefs
						SynonymI newSyn = target.makeSynonym(synText);
						childTerm.addSynonym(newSyn);
					}
				}
				target.attachParent(childTerm, parentTerm);
				addChildren(childClass,childTerm,target);
			}
		}
	}

	
	  



}
