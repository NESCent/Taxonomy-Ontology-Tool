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


	private OBOUtils u = null;

	//String defaultPrefix;
	private final String idSuffix = ":%07d";
	
	private File source;
	private TaxonStore target;

	static final Logger logger = Logger.getLogger(OBOMerger.class.getName());

	@Override
	public boolean canAttach() {
		return true;
	}

	@Override
	public boolean canPreserveID(){
		return true;
	}

	@Override
	public void setSource(File sourceFile){
		source = sourceFile;
	}
	
	@Override
	public void setTarget(TaxonStore targetStore){
		target = targetStore;
	}
	

	@Override
	public void merge(String prefix) {
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
	 * @param attachment name of parent node for attached clade
	 * @param cladeRoot name of root node (child of parent) for attached clade
	 * @param prefix default prefix for target ontology
	 */
	@Override
	public void attach(String attachment, String cladeRoot, String prefix) {
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
				OBOClass attachmentClass = u.lookupTermByName(attachment);
				if (attachmentClass != null){
					parentTerm = target.addTermbyID(attachmentClass.getID(),attachment);
				}
				else {
					parentTerm = target.addTerm(attachment);
				}
				logger.info("Assigning " + attachment + " as root");
			}
		}
		if (cladeClass == null){
			logger.error("Clade root to attach " + cladeRoot + " was not found");
			return;
		}
		// first copy all the descendents of cladeClass into the target
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
			}
			else{
				cladeTerm = target.addTerm(cladeClass.getName());
				target.addXRefToTerm(cladeTerm,idFields[0],idFields[1]);  // could be an alternate ID?
			}
		}
		else{
			logger.warn("Could not split OBOID " + cladeClass.getID() + " to generate xref in target term");
		}
		if (cladeTerm != null){
			if (u.getRankString(cladeClass) != null)
				target.setRankFromName(cladeTerm, u.getRankString(cladeClass));
			target.attachParent(cladeTerm, parentTerm);
			addChildren(cladeClass,cladeTerm,target,prefix);
		}
		//u.setNameSpace(oboNameSpace, fileSpec);
		//defaultFormat = prefix + idSuffix;

		//targetFile = fileSpec;
		//fillRankNames();

	}

	// Note: parentClass is from the obo tree being attached, parentTerm is the copy in the target tree
	// so parentTerm.asOBOClass != parentClass
	private void addChildren(OBOClass parentClass, Term parentTerm, TaxonStore target, String prefix){
		final Collection<Link> childLinks = parentClass.getChildren();
		for(Link l : childLinks){
			OBOProperty lType = l.getType();
			if (OBOUtils.ISA_PROPERTY.equals(lType.getID())){
				OBOClass childClass = (OBOClass)l.getChild();
				Term childTerm = null;
				String[] idFields = childClass.getID().split(":");
				if (idFields.length == 2){
					if (idFields[0].equals(prefix)){
						childTerm = target.addTermbyID(childClass.getID(), childClass.getName());
						if (childClass.getDbxrefs() != null){
							for (Dbxref d : childClass.getDbxrefs()){
								target.addXRefToTerm(childTerm, d.getDatabase(), d.getDatabaseID());
							}
						}
						if (childClass.getSynonyms() != null){
							for (Synonym s : childClass.getSynonyms()){
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
									childTerm.addSynonym(newSyn);
							}
						}

					}
					else {
						childTerm = target.addTerm(childClass.getName());
						target.addXRefToTerm(childTerm,idFields[0],idFields[1]);  // could be an alternate ID?
					}
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
				addChildren(childClass,childTerm,target,prefix);
			}
		}
	}






}
