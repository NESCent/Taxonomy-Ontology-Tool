/**
 * VTOTool - a utility build taxonomy ontologies from multiple sources 
 * 
 * Copyright (c) 2007-2010 Peter E. Midford
 *
 * Licensed under the 'MIT' license (http://opensource.org/licenses/mit-license.php)
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 *
 * Created on June 3, 2010
 * Last updated on June 4, 2010
 *
 */

package org.nescent.VTO.lib;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

/**
 * 
 * @author pmidford
 *
 */
public class NCBIMerger implements Merger {

	static final private String NAMESFILENAME = "names.dmp";
	static final private String NODESFILENAME = "nodes.dmp";
	static final private String NCBIVERTEBRATE = "10";
	static final private String NCBIMAMMAL = "2";
	static final private String NCBIPRIMATE = "5";
	static final private String NCBIRODENT = "6";
	static final private String NCBIINVERTEBRATE = "1";
	
	static final private String SCIENTIFICNAMETYPE = "scientific name";
	static final private String SYNONYMNAMETYPE = "synonym";
	
	static final private String NCBIDBNAME = "NCBITaxon";
	
    static final Pattern tabPattern = Pattern.compile("\t|\t");   //try this pattern as it matches the documentation

    private int count = 0;
    private File source;
    private TaxonStore target;
    
	static Logger logger = Logger.getLogger(NCBIMerger.class.getName());

    /* Metadata methods */
    
	/**
	 * @return this merger supports attachment (splicing in tree structure) as well as merging
	 */
	public boolean canAttach(){
		return true;
	}
	
	public boolean canPreserveID(){
		return true;
	}

	/**
	 * @arg sourceDirectory until most mergers, the NCBI dump is a set of files in a common directory
	 */
	public void setSource(File sourceDirectory){
		source = sourceDirectory;
	}
	
	public void setTarget(TaxonStore targetStore){
		target = targetStore;
	}
	

    /**
     * This collects NCBI IDs as synonyms
     * @param prefix
     */
    public void merge(String prefix) {
		final File namesFile = new File(source.getAbsolutePath()+'/'+NAMESFILENAME);
		final File nodesFile = new File(source.getAbsolutePath()+'/'+NODESFILENAME);
		final Set <Integer> nodesInScope = new HashSet<Integer>(10000);
		final Map<Integer,String> nodeRanks = new HashMap<Integer,String>(10000);
		final Map<Integer,Set<Integer>> nodeChildren = new HashMap<Integer,Set<Integer>>(10000);
		buildScopedNodesList(nodesFile,nodesInScope,nodeRanks,nodeChildren);
		Map <String,Integer> namesInScope = new HashMap<String,Integer>(nodesInScope.size());
		Map <Integer,String> termToName = new HashMap<Integer,String>(nodesInScope.size());
		Map <Integer,Set<String>> synonymsInScope = new HashMap<Integer,Set<String>>(nodesInScope.size());
		buildScopedNamesList(namesFile,nodesInScope,namesInScope, termToName, synonymsInScope);
		logger.info("Node count = " + nodesInScope.size());
		logger.info("Name count = " + namesInScope.size());
		logger.info("Synonym count = " + synonymsInScope.size());
		int nameHits = 0;
        for(Integer termid : synonymsInScope.keySet()){
        	String primaryName = termToName.get(termid);
        	Term primaryTerm = target.getTermbyName(primaryName);
        	for (String syn : synonymsInScope.get(termid)){
        		SynonymI newSyn = target.makeSynonymWithXref(syn, NCBIDBNAME, Integer.toString(termid));
        		primaryTerm.addSynonym(newSyn);
        	}
        }
 		logger.info("Count of names matching NCBI names = " + nameHits);
	}


	private void buildScopedNodesList(File nf, Set<Integer> nodes, Map<Integer,String> ranks, Map<Integer,Set<Integer>> children){
        try {
            final BufferedReader br = new BufferedReader(new FileReader(nf));
            String raw = br.readLine();
            while (raw != null){
                final String[] digest = tabPattern.split(raw);
                final String scope = digest[8];
                if (NCBIVERTEBRATE.equals(scope) || NCBIMAMMAL.equals(scope) || NCBIRODENT.equals(scope) || NCBIPRIMATE.equals(scope)){
                    final Integer nodeVal = Integer.parseInt(digest[0]);
                	nodes.add(nodeVal);                	
                    Integer parentVal = Integer.parseInt(digest[2]);
                    if (children.containsKey(parentVal)){
                    	children.get(parentVal).add(nodeVal);
                    }
                    else {
                    	Set<Integer> childSet = new HashSet<Integer>();
                    	childSet.add(nodeVal);
                    	children.put(parentVal,childSet);
                    }
                    String nodeRank = digest[4];
                    ranks.put(nodeVal, nodeRank);
                }
                raw = br.readLine();
            }
        }
        catch (Exception e) {
            logger.error(e);
            return;
        }
        return;
	}
	
	private void buildScopedNamesList(File nf, Set<Integer> nodes, Map<String,Integer> names, Map<Integer,String> termToName, Map<Integer,Set<String>> synonyms) {
		try{
            final BufferedReader br = new BufferedReader(new FileReader(nf));
            String raw = br.readLine();
            while (raw != null){
                final String[] digest = tabPattern.split(raw);
                Integer nodeNum = Integer.parseInt(digest[0]);
                String name = digest[2].trim();  //Unfortunately, there are some names with leading spaces in NCBI...
                String nameType = digest[6];
                if (nodes.contains(nodeNum) && SCIENTIFICNAMETYPE.equalsIgnoreCase(nameType)){
                	names.put(name,nodeNum);
                	termToName.put(nodeNum,name);
                }
                if (nodes.contains(nodeNum) && SYNONYMNAMETYPE.equalsIgnoreCase(nameType)){
                	if (synonyms.containsKey(nodeNum)){
                		synonyms.get(nodeNum).add(name);
                	}
                	else{
                		Set<String> synSet = new HashSet<String>();
                		synSet.add(name);
                		synonyms.put(nodeNum,synSet);
                	}
                }
                raw = br.readLine();
            }			
		}
		catch (Exception e){
			logger.error(e);
			return;
		}
		// TODO Auto-generated method stub
		return;
	}

	

	/**
	 * 
	 */
	@Override
	public void attach(String attachment, String cladeRoot, String prefix) {
		final File namesFile = new File(source.getAbsolutePath()+'/'+NAMESFILENAME);
		final File nodesFile = new File(source.getAbsolutePath()+'/'+NODESFILENAME);
		final Set <Integer> nodesInScope = new HashSet<Integer>(10000);
		final Map<Integer,String> nodeRanks = new HashMap<Integer,String>(10000);
		final Map<Integer,Set<Integer>> nodeChildren = new HashMap<Integer,Set<Integer>>(10000);
		buildScopedNodesList(nodesFile,nodesInScope,nodeRanks,nodeChildren);
		Map <String,Integer> namesInScope = new HashMap<String,Integer>(nodesInScope.size());
		Map <Integer,String> termToName = new HashMap<Integer,String>(nodesInScope.size());
		Map <Integer,Set<String>> synonymsInScope = new HashMap<Integer,Set<String>>(nodesInScope.size());
		buildScopedNamesList(namesFile,nodesInScope,namesInScope, termToName,synonymsInScope);
		logger.info("Node count = " + nodesInScope.size());
		logger.info("Name count = " + namesInScope.size());
		logger.info("Synonym count = " + synonymsInScope.size());
		Term parentTerm = null;
		if (!"".equals(attachment)){
			parentTerm = target.getTermbyName(attachment);
			if (parentTerm == null){   //parent is unknown
				if (!target.isEmpty()){
					logger.error("Can not attach " + namesFile.getAbsolutePath() + " specified parent: " + attachment + " is unknown to " + target);
					return;
				}
				else { // attachment will be added first to provide a root for an otherwise empty target
					parentTerm = target.addTerm(attachment);
					logger.info("Assigning " + attachment + " as root");
				}
			}
		}
        final Integer parentNode = namesInScope.get(parentTerm.getLabel());
        logger.info("Building tree");
        addChildren(parentNode, target,nodeChildren, termToName, nodeRanks);
        logger.info("Finished building tree" + parentNode);
        for(Integer termid : synonymsInScope.keySet()){
        	String primaryName = termToName.get(termid);
        	if (primaryName == null){
        		//logger.warn("No termname for id " + termid);
        		continue;
        	}
        	Term primaryTerm = target.getTermbyName(primaryName);
        	if (primaryTerm == null){
        	//	logger.warn("No term for name " + primaryName);
        		continue;
        	}
        	for (String syn : synonymsInScope.get(termid)){
        		SynonymI newSyn = target.makeSynonymWithXref(syn, NCBIDBNAME, Integer.toString(termid));
        		primaryTerm.addSynonym(newSyn);
        	}
        }
        logger.info("Done; count = " + count);		
	}

	private void addChildren(Integer parent, TaxonStore target, Map<Integer, Set<Integer>> nodeChildren, Map<Integer, String> termToName, Map<Integer, String> nodeRanks) {
		Set<Integer> children = nodeChildren.get(parent);
		if (children != null){
			String parentName = termToName.get(parent);
			if (parentName == null)
				throw new RuntimeException("parent name is null");
			Term parentTerm = target.getTermByXRef(NCBIDBNAME,parent.toString());
			if (parentTerm == null)
				parentTerm = target.getTermbyName(parentName);
			if (parentTerm == null)
				throw new RuntimeException("parent term is null, name is " + termToName.get(parent));
			for (Integer childID : children){
				String childName = termToName.get(childID);
				Term childTerm = target.getTermbyName(childName);
				
				if (childTerm == null){
					String rankStr = nodeRanks.get(childID);
					if (rankStr != null && !"no rank".equals(rankStr)){
						if ("subspecies".equals(rankStr)){
							//merge subspecies as synonyms of their parent species (following CoF/TTO practice)
							SynonymI subSyn = target.makeSynonymWithXref(childName, NCBIDBNAME, childID.toString());
							parentTerm.addSynonym(subSyn);
						}
						else {
							//standard case - make a child term and attach
							childTerm = target.addTerm(childName);
							target.addXRefToTerm(childTerm,NCBIDBNAME,childID.toString());  // could be an alternate ID?
							target.setRankFromName(childTerm,rankStr);
							target.attachParent(childTerm, parentTerm);
							count++;
						}
					}
					else if ("species".equals(target.getRankString(parentTerm))) {  
						//a rankless term with a species as a parent is treated as a subspecies
						SynonymI subSyn = target.makeSynonymWithXref(childName, NCBIDBNAME, childID.toString());
						parentTerm.addSynonym(subSyn);
					}
					else {  
						// for now, we'll go ahead and add other rankless terms
						childTerm = target.addTerm(childName);
						target.addXRefToTerm(childTerm,NCBIDBNAME,childID.toString());  // could be an alternate ID?
						target.setRankFromName(childTerm,rankStr);
						target.attachParent(childTerm, parentTerm);
						count++;
					}
				}
				else {  //node with child's name already exists.  For now, we'll add the parent's name as a suffix
					String newChildName = childName + " (" + parentName + ")";
					if (target.getTermbyName(newChildName) != null){
						throw new RuntimeException("Unresolvable duplication " + childName + " " + newChildName);
					}
					childTerm = target.addTerm(newChildName);
					target.addXRefToTerm(childTerm,NCBIDBNAME,childID.toString());  // could be an alternate ID?
					String rankStr = nodeRanks.get(childID);
					if (rankStr != null && !"no rank".equals(rankStr)){
						target.setRankFromName(childTerm,rankStr);
						target.attachParent(childTerm, parentTerm);
			        	count++;
					}
					else {
						target.attachParent(childTerm, parentTerm);
			        	count++;						
					}
				}

	        	if (count % 1000 == 0)
	        		logger.info("Count = " + count + " term = " + childName);
				addChildren(childID,target,nodeChildren,termToName, nodeRanks);
			}
		}
	}


	
}
