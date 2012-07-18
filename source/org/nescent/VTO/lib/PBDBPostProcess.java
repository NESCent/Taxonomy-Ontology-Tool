package org.nescent.VTO.lib;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

public class PBDBPostProcess implements Merger{
	
	private File source = null;
	private TaxonStore target = null;

	private static final String TARGETNOTSETMESSAGE = "Target ontology for PBDB post process not set";
	private static final String SOURCENOTSETMESSAGE = "Source file for PBDB post process not set";

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
	public void setSource(File source) {
		this.source = source;
	}

	@Override
	public void setTarget(TaxonStore target) {
		this.target = target;
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
		
		for(Action a : alist){
			String taxonName = a.taxa.get(0);
			Term taxonTerm = target.getTermbyName(taxonName);	
			if ("delete".equalsIgnoreCase(a.command)){
				deleteCount++;
				if (taxonTerm == null)
					badDeleteCount++;
				else {
					target.obsoleteTerm(taxonTerm);
				}
			}
			if ("modified".equalsIgnoreCase(a.command)){
				modifyCount++;
				if (taxonTerm == null)
					badModifyCount++;
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
