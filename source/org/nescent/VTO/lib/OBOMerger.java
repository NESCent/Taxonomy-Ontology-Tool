package org.nescent.VTO.lib;

import java.io.File;
import java.util.Collection;
import java.util.Iterator;

import org.apache.log4j.Logger;
import org.obo.datamodel.Dbxref;
import org.obo.datamodel.Link;
import org.obo.datamodel.OBOClass;
import org.obo.datamodel.OBOProperty;
import org.obo.datamodel.Synonym;

public class OBOMerger implements Merger {


	private OBOUtils sourceUtils = null;

	//String defaultPrefix;
	private final String idSuffix = ":%07d";
	
	private File sourceFile;
	private TaxonStore target;
	
	private boolean preserveID = false;
	private SynonymSource preserveSynonyms;

	static final Logger logger = Logger.getLogger(OBOMerger.class.getName());

	@Override
	public final boolean canAttach() {
		return true;
	}

	@Override
	public boolean canPreserveID(){
		return true;
	}

	@Override
	public void setPreserveID(boolean v){
		preserveID = v;
	}
	
	@Override
	public void setPreserveSynonyms(SynonymSource s){
		preserveSynonyms = s;
	}
	
	@Override
	public void setSource(File source){
		sourceFile = source;
	}
	
	@Override
	public void setTarget(TaxonStore targetStore){
		target = targetStore;
	}
	

	@Override
	public void merge(String prefix) {
		logger.info("Loading OBO file " + sourceFile);
		sourceUtils = new OBOUtils(sourceFile.getAbsolutePath());
		logger.info("Finished loading");
		int termCount = 0;
		int synCount = 0;
		target.updateIDGenerator(prefix);
		Collection <OBOClass> allTerms = sourceUtils.getTerms();
		for (OBOClass term : allTerms){
			Term matchingTerm = target.getTermbyName(term.getName());
			if (matchingTerm != null){
				termCount++;
				//SynonymI s = target.makeSynonymWithXref(term.getName(), prefix, term.getID());
				//matchingTerm.addSynonym(s);
				// add synonyms from term
				for (Synonym syn : term.getSynonyms()){
					for (Dbxref ref : syn.getXrefs()){  //need to avoid common names....
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

	/**
	 * @param targetParentName name of parent node for attached clade
	 * @param sourceRootName name of root node (child of parent) for attached clade
	 * @param prefix default prefix for target ontology
	 */
	@Override
	public void attach(String targetParentName, String sourceRootName, String prefix) {
		logger.info("Loading OBO file " + sourceFile);
		sourceUtils = new OBOUtils(sourceFile.getAbsolutePath());
		logger.info("Finished loading");
		logger.info("Attach started target size = " + target.getTerms().size());
		logger.info("               source size = " + sourceUtils.getTerms().size());
		if (targetParentName == null)
			copyRootToTarget(sourceRootName,prefix);
		else if (!targetParentName.equalsIgnoreCase(sourceRootName)){
			OBOClass sourceRoot = sourceUtils.lookupTermByName(sourceRootName);  //this is the root of the clade - copy this and its children
			Term targetParent = target.getTermbyName(targetParentName);
			Term targetRoot = copyTerm(sourceRoot,prefix);
			logger.info("Checkpoint 1: targetParent = " + targetParent);
			logger.info("Checkpoint 1: " + targetParentName + " = " + target.getTermbyName(targetParentName));
			logger.info("Checkpoint 1: Target size = " + target.getTerms().size());
			logger.info("Checkpoint 1b: targetParent = " + targetParent);
			logger.info("Checkpoint 1b: node named by targetParentName (" + targetParentName + ") = " + target.getTermbyName(targetParentName));
			logger.info("Checkpoint 1b: targetRoot = " + targetRoot + "; name is " + targetRoot.getLabel());
			target.attachParent(targetRoot, targetParent);
			addChildren(sourceRoot,targetRoot,target,prefix);
			logger.info("Checkpoint 2: " + targetParentName + " = " + target.getTermbyName(targetParentName));
			logger.info("Checkpoint 2: Target size = " + target.getTerms().size());
			
			//anything special here?
		}
		

	}

	private void copyRootToTarget(String sourceRootName, String prefix){
		OBOClass sourceRoot = sourceUtils.lookupTermByName(sourceRootName);  //this is the root of the clade - copy this and its children
		if (sourceRoot == null){
			logger.error("Can not attach " + sourceFile.getAbsolutePath() + " specified root: " + sourceRootName + " is unknown to " + sourceUtils);
			return;
		}
		if (target.getTermbyName(sourceRootName) != null){
			logger.error("Can not attach " + sourceFile.getAbsolutePath() + " specified root: " + sourceRootName + " already exists in " + target);
		}
		Term targetRoot;
		// this should be copyTerm
		if (preserveID){
			targetRoot = target.addTermbyID(sourceRoot.getID(),sourceRootName);
		}
		else {
			targetRoot = target.addTerm(sourceRootName);
		}			
		logger.info("Assigning " + sourceRootName + " as root");
		if (sourceUtils.getRankString(sourceRoot) != null)
			target.setRankFromName(targetRoot, sourceUtils.getRankString(sourceRoot));
		addChildren(sourceRoot,targetRoot,target,prefix);
	}

	
	private void checkTarget(String s){
		for (Term t : target.getTerms()){
			if (s.equals(t.getLabel())){
				logger.info("Found " + s + " on through search on load check");
			}
		}
		
	}

	// Note: parentClass is from the obo tree being attached, parentTerm is the copy in the target tree
	// so parentTerm.asOBOClass != parentClass
	private void addChildren(OBOClass sourceParent, Term targetParent, TaxonStore target, String prefix){
		//logger.info("adding children of " + sourceParent.getName() + " target Term is " + targetParent.getLabel());
		final Collection<Link> childLinks = sourceParent.getChildren();
		for(Link l : childLinks){
			OBOProperty lType = l.getType();
			if (OBOUtils.ISA_PROPERTY.equals(lType.getID())){
				OBOClass childClass = (OBOClass)l.getChild();
				Term childTerm = copyTerm(childClass,prefix);
//				if ("Actinopterygii".equals(childTerm.asOBOClass().getName())){
//					System.out.println("Copying Actinopterygii;  checking lookup: " + target.getTermbyName("Actinopterygii"));
//					System.out.println("Checking Chondrichthyes;  checking lookup: " + target.getTermbyName("Chondrichthyes"));
//				}
				if ("Chondrichthyes".equals(childTerm.asOBOClass().getName())){
					System.out.println("Copying Chondrichthyes;  checking lookup: " + target.getTermbyName("Chondrichthyes"));
					System.out.println("Checking Actinopterygii;  checking lookup: " + target.getTermbyName("Actinopterygii"));
				}
				if (sourceUtils.getRankString(childClass) != null)
					target.setRankFromName(childTerm, sourceUtils.getRankString(childClass));
//				if ("Actinopterygii".equals(childTerm.asOBOClass().getName())){
//					System.out.println("Check before adding synonyms Actinopterygii;  checking lookup: " + target.getTermbyName("Actinopterygii"));
//				}
//				if ("Chondrichthyes".equals(childTerm.asOBOClass().getName())){
//					System.out.println("Check before adding synonyms Chondrichthyes;  checking lookup: " + target.getTermbyName("Chondrichthyes"));
//				}
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
				target.attachParent(childTerm, targetParent);
//				if ("Actinopterygii".equals(childTerm.asOBOClass().getName())){
//					System.out.println("Check before adding children Actinopterygii;  checking lookup: " + target.getTermbyName("Actinopterygii"));
//				}
//				if ("Chondrichthyes".equals(childTerm.asOBOClass().getName())){
//					System.out.println("Check before adding children Chondrichthyes;  checking lookup: " + target.getTermbyName("Chondrichthyes"));
//				}
				addChildren(childClass,childTerm,target,prefix);
//				if ("Actinopterygii".equals(childTerm.asOBOClass().getName())){
//					System.out.println("Check after adding children Actinopterygii;  checking lookup: " + target.getTermbyName("Actinopterygii"));
//				}
//				if ("Chondrichthyes".equals(childTerm.asOBOClass().getName())){
//					System.out.println("Check after adding children Chondrichthyes;  checking lookup: " + target.getTermbyName("Chondrichthyes"));
//				}
			}
		}
	}

	private Term copyTerm(OBOClass sourceClass, String prefix){
		Term targetTerm = null;
		if (preserveID){
			targetTerm = target.addTermbyID(sourceClass.getID(),sourceClass.getName());
		}
		else{
			String[] idFields = sourceClass.getID().split(":");
			if (idFields.length == 2){
				targetTerm = target.addTerm(sourceClass.getName());				
				target.addXRefToTerm(targetTerm,idFields[0],idFields[1]);  // could be an alternate ID?
			}
			else{
				logger.warn("Could not split OBOID " + sourceClass.getID() + " to generate xref in target term");
				targetTerm = target.addTerm(sourceClass.getName());				
			}
		}
		if (sourceClass.getDbxrefs() != null){
			for (Dbxref d : sourceClass.getDbxrefs()){
				target.addXRefToTerm(targetTerm, d.getDatabase(), d.getDatabaseID());
			}
		}
		if (sourceClass.getSynonyms() != null){
			for (Synonym s : sourceClass.getSynonyms()){
				SynonymI newSyn = null; 
				if (s.getXrefs() != null && !s.getXrefs().isEmpty()){
					Iterator<Dbxref> xIter = s.getXrefs().iterator();
					if (xIter.hasNext()){
						Dbxref d = xIter.next();
						newSyn = target.makeSynonymWithXref(s.getText(), d.getDatabase(), d.getDatabaseID());
					}
				}
				else {
					newSyn = target.makeSynonym(s.getText());
				}
				if (newSyn != null)
					targetTerm.addSynonym(newSyn);
			}
		}
		if (sourceUtils.isExtinct(sourceClass))
			sourceUtils.setExtinct(targetTerm.asOBOClass());
	return targetTerm;
}



}
