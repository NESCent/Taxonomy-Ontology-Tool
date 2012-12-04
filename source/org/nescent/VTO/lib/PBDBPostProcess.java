package org.nescent.VTO.lib;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.obo.datamodel.Dbxref;
import org.obo.datamodel.Link;
import org.obo.datamodel.OBOClass;
import org.obo.datamodel.OBOProperty;
import org.obo.datamodel.Synonym;

public class PBDBPostProcess implements Merger{
	
	private File source = null;
	private TaxonStore target = null;

	private static final String TARGETNOTSETMESSAGE = "Target ontology for PBDB post process not set";
	private static final String SOURCENOTSETMESSAGE = "Source file for PBDB post process not set";
	private static final String NOURITEMPLATEMESSAGE = "PBDB postprocessor merger does not accept URITemplate attributes";

	private final Logger logger = Logger.getLogger(PBDBPostProcess.class.getName());
	
	@Override
	public boolean canAttach() {
		return false;
	}

	@Override
	public boolean canPreserveID() {
		return true;
	}

	@Override
	public void setPreserveID(boolean v) {
	}

	@Override
	public void setPreserveSynonyms(SynonymSource s) {
	}

	@Override
	public void setUpdateObsoletes(boolean v) {
		throw new RuntimeException("PBDBPostProcess doesn't support updating obsoletes because it does not support attaching");		
	}

	@Override
	public void setSource(File source) {
		this.source = source;
	}

	@Override
	public void setTarget(TaxonStore target) {
		this.target = target;
	}
	
	/**
	 * @param sa specifies whether this merges synonyms or cross references
	 */
	@Override
	public void setSubAction(String sa){
		//throw new IllegalArgumentException(NOSUBACTIONMESSAGE);  //no real need to fail here
	}

	@Override
	public void setURITemplate(String template) {
		throw new IllegalArgumentException(NOURITEMPLATEMESSAGE);
	}
	
	private void checkInitialization(){
		if (target == null){
			throw new IllegalStateException(TARGETNOTSETMESSAGE);
		}
		if (source == null){
			throw new IllegalStateException(SOURCENOTSETMESSAGE);
		}		
	}

	@Override
	public void merge(String prefix) {
		checkInitialization();
		try {
			final List<Action> actions = buildActions(source);
			if (actions != null)
				logger.info("Read " + actions.size() + " actions from file");
			else
				logger.info("No actions read from file");
			processActions(actions);
		}
		catch(IOException e){
			logger.error("An IO Exception was thrown while parsing: " + source.getAbsolutePath());
		}
	}
	
	@Override
	public void attach(String parent, String cladeRoot, String prefix) {
		throw new RuntimeException("PBDBPostProcess doesn't support attach");		
	}

	
	private List<Action> buildActions(File source) throws IOException{
		final ArrayList<Action> result = new ArrayList<Action>();
        final BufferedReader br = new BufferedReader(new FileReader(source));
        String raw = br.readLine();
        while(raw != null){
        	Action a = Action.getValidInstance(raw);
        	if (a != null)
        		result.add(a);
        	raw=br.readLine();
        }

        br.close();
		return result;
	}
	
	private void processActions(List<Action> alist){
		int modifyCount = 0;
		int deleteCount = 0;
		int badModifyCount = 0;
		int badDeleteCount = 0;
		
		//run the modifications first - this may prevent orphans from getting created in the first place
		for(Action a : alist){
			String taxonName = a.taxa.get(0);
			Term taxonTerm = target.getTermbyName(taxonName);	
			if ("modified".equalsIgnoreCase(a.command)){
				modifyCount++;
				if (taxonTerm == null)
					badModifyCount++;
			}
		}
		// now run the delete actions - and if an obsoleted node has children, attach them to the nearest ancestor node
		for(Action a : alist){
			String taxonName = a.taxa.get(0);
			Term taxonTerm = target.getTermbyName(taxonName);	
			if ("delete".equalsIgnoreCase(a.command)){
				deleteCount++;
				if (taxonTerm == null)
					badDeleteCount++;
				else {
					Set <Term> children = taxonTerm.getChildren();
					List <Term> ancestors = taxonTerm.getAncestors();
					final Term termParent = ancestors.get(0);  //save this for disconnection
					for (Term anc : ancestors){
						if (!anc.isObsolete()){
							for (Term child : children){
								child.removeParent(taxonTerm);
								target.attachParent(child, anc);
							}
							break;
						}
					}
					taxonTerm.removeParent(termParent);
					taxonTerm.removeProperties();
					target.obsoleteTerm(taxonTerm);
				}
			}
		}
		logger.info("Processed " + modifyCount + " modify actions, of which " + badModifyCount + " had unresolvable key taxa");
		logger.info("Processed " + deleteCount + " delete actions, of which " + badDeleteCount + " had unresolvable key taxa");
		
	}
	


	
	static class Action{
		final List<String> taxa = new ArrayList<String>();
		final String command;
	
		private Action(String c){
			command = c;
		}
		/**
		 * Parses a line specifying a taxon: tab delimited columns; first column is an action (empty = no-op) other columns are
		 * a taxonomic hierarchy path up to (hopefully) the root taxon of the TaxonStore of the PostProcess using this
		 * @param line
		 * @return
		 */
		static Action getValidInstance(String line){
			String[] parsed = line.split("\t");
			if (parsed.length<2 || "".equals(parsed[0]))   // empty line or taxon needing no modification
				return null;
			final Action result = new Action(parsed[0]);
			for(int i = 1;i<parsed.length;i++)
				result.taxa.add(parsed[i]);
			return result;
		}
	}

}
