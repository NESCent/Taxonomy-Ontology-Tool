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
		Term targetParent = target.getTermbyName(targetParentName);  //the node in target that will be the parent of the attached clade
		if (targetParent == null){   //parent is unknown
			if (!target.isEmpty()){
				logger.error("Can not attach " + sourceFile.getAbsolutePath() + " specified parent: " + targetParentName + " is unknown to " + target);
				return;
			}
			else { // attachment will be added first to provide a root for an otherwise empty target
				OBOClass attachmentClass = sourceUtils.lookupTermByName(targetParentName);
				if (attachmentClass != null){
					targetParent = target.addTermbyID(attachmentClass.getID(),targetParentName);
				}
				else {
					targetParent = target.addTerm(targetParentName);
				}
				logger.info("Assigning " + targetParentName + " as root");
			}
		}
		OBOClass sourceRoot = sourceUtils.lookupTermByName(sourceRootName);  //this is the root of the clade - copy this and its children
		if (sourceRoot == null){
			logger.warn("source root (" + sourceRootName + ") to copy from was not found");
			sourceRoot = targetParent.asOBOClass();
		}
		// first copy all the descendents of cladeClass into the target
		logger.info("Checkpoint 1: targetParent = " + targetParent);
		logger.info("Checkpoint 1: " + targetParentName + " = " + target.getTermbyName(targetParentName));
		logger.info("Checkpoint 1: Target size = " + target.getTerms().size());
		checkTarget("Temnospondyli");
		Term targetRoot = copyTerm(sourceRoot,prefix);
		logger.info("Checkpoint 1b: targetParent = " + targetParent);
		logger.info("Checkpoint 1b: node named by targetParentName (" + targetParentName + ") = " + target.getTermbyName(targetParentName));
		logger.info("Checkpoint 1b: targetRoot = " + targetRoot + "; name is " + targetRoot.getLabel());
	
		if (targetRoot != null){
			if (sourceUtils.getRankString(sourceRoot) != null)
				target.setRankFromName(targetRoot, sourceUtils.getRankString(sourceRoot));
			if (!targetRoot.getID().equals(targetParent.getID()))
				target.attachParent(targetRoot, targetParent);
			addChildren(sourceRoot,targetRoot,target,prefix);
		}
		logger.info("Checkpoint 2: " + targetParentName + " = " + target.getTermbyName("Amphibia"));
		logger.info("Checkpoint 2: Temnospondyli = " + target.getTermbyName("Temnospondyli"));
		logger.info("Checkpoint 2: Target size = " + target.getTerms().size());
		checkTarget("Amphibia");
		//u.setNameSpace(oboNameSpace, fileSpec);
		//defaultFormat = prefix + idSuffix;

		//targetFile = fileSpec;
		//fillRankNames();

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
	private void addChildren(OBOClass parentClass, Term parentTerm, TaxonStore target, String prefix){
		//logger.info("adding children of " + parentClass.getName() + " parentTerm is " + parentTerm.getLabel());
		final Collection<Link> childLinks = parentClass.getChildren();
		for(Link l : childLinks){
			OBOProperty lType = l.getType();
			if (OBOUtils.ISA_PROPERTY.equals(lType.getID())){
				OBOClass childClass = (OBOClass)l.getChild();
				Term childTerm = copyTerm(childClass,prefix);
				if (sourceUtils.getRankString(childClass) != null)
					target.setRankFromName(childTerm, sourceUtils.getRankString(childClass));
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
				addChildren(childClass,childTerm,target,prefix);
			}
		}
	}

	private Term copyTerm(OBOClass cladeClass, String prefix){
		Term cladeTerm = null;
		String[] idFields = cladeClass.getID().split(":");
		if (idFields.length == 2){
			if (idFields[0].equals(prefix)){
				cladeTerm = target.addTermbyID(cladeClass.getID(),cladeClass.getName());
				if (cladeClass.getDbxrefs() != null){
					for (Dbxref d : cladeClass.getDbxrefs()){
						target.addXRefToTerm(cladeTerm, d.getDatabase(), d.getDatabaseID());
					}
				}
				if (cladeClass.getSynonyms() != null){
					for (Synonym s : cladeClass.getSynonyms()){
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
							cladeTerm.addSynonym(newSyn);
					}
				}
				if (sourceUtils.isExtinct(cladeClass))
					sourceUtils.setExtinct(cladeTerm.asOBOClass());
			}
			else{
				cladeTerm = target.addTerm(cladeClass.getName());
				target.addXRefToTerm(cladeTerm,idFields[0],idFields[1]);  // could be an alternate ID?
			}
		}
		else{
			logger.warn("Could not split OBOID " + cladeClass.getID() + " to generate xref in target term");
		}
		return cladeTerm;
	}



}
