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

package org.phenoscape.VTO.lib;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.obo.datamodel.Dbxref;
import org.obo.datamodel.OBOClass;

public class NCBIMerger implements Merger {

	static final private String NAMESFILENAME = "names.dmp";
	static final private String NODESFILENAME = "nodes.dmp";
	static final private String NCBIVERTEBRATE = "10";
	static final private String NCBIPRIMATE = "5";
	static final private String NCBIRODENT = "6";
	
	static final private String SCIENTIFICNAMETYPE = "scientific name";
	static final private String SYNONYMNAMETYPE = "synonym";
	
    static final Pattern tabPattern = Pattern.compile("\t|\t");   //try this pattern as it matches the documentation

	static Logger logger = Logger.getLogger(NCBIMerger.class.getName());

    /**
     * This collects NCBI IDs as synonyms
     * @param ncbiSource
     * @param theStore
     */
    public void merge(File ncbiSource, TaxonStore target, String prefix) {
		final File namesFile = new File(ncbiSource.getAbsolutePath()+'/'+NAMESFILENAME);
		final File nodesFile = new File(ncbiSource.getAbsolutePath()+'/'+NODESFILENAME);
		final Set <Integer> nodesInScope = new HashSet<Integer>(10000);
		final Map<Integer,String> nodeRanks = new HashMap<Integer,String>(10000);
		final Map<Integer,Integer> nodeParents = new HashMap<Integer,Integer>(10000);
		buildScopedNodesList(nodesFile,nodesInScope,nodeRanks,nodeParents);
		Map <String,Integer> namesInScope = new HashMap<String,Integer>(nodesInScope.size());
		Map <Integer,String> termToName = new HashMap<Integer,String>(nodesInScope.size());
		Map <String,Integer> synonymsInScope = new HashMap<String,Integer>(nodesInScope.size());
		buildScopedNamesList(namesFile,nodesInScope,namesInScope, termToName, synonymsInScope);
		System.out.println("Node count = " + nodesInScope.size());
		System.out.println("Name count = " + namesInScope.size());
		System.out.println("Synonym count = " + synonymsInScope.size());
		int nameHits = 0;
        final Collection<Term> terms = target.getTerms();	
		for(Term term : terms){		
			if(namesInScope.containsKey(term.getLabel())){
				SynonymI oldSyn = term.getOldSynonym("NCBITaxon");
				if (synonymsInScope.get(term.getLabel()) == null)
					logger.warn("Null scope for synonym " + term.getLabel() + "; old syn = " + oldSyn);
				String newID = synonymsInScope.get(term.getLabel()).toString();
				if (oldSyn != null && !(newID.equals(oldSyn.getID()))){
					System.err.println("An NCBI name (" + term.getLabel() + ") has a changed ID (" + newID + "); was " + oldSyn.getID());  // what to do
				}
				SynonymI newSyn = target.makeSynonym(namesInScope.get(term.getLabel()).toString(), synonymsInScope.get(term.getLabel()).toString(), "NCBITaxon");
				term.addSynonym(newSyn);
				nameHits++;
			}
		}
		System.out.println("Names matching NCBI names = " + nameHits);
	}


	private void buildScopedNodesList(File nf, Set<Integer> nodes, Map<Integer,String> ranks, Map<Integer,Integer> parents){
        try {
            final BufferedReader br = new BufferedReader(new FileReader(nf));
            String raw = br.readLine();
            while (raw != null){
                final String[] digest = tabPattern.split(raw);
                final String scope = digest[8];
                if (NCBIVERTEBRATE.equals(scope) || NCBIRODENT.equals(scope) || NCBIPRIMATE.equals(scope)){
                    final Integer nodeVal = Integer.parseInt(digest[0]);
                	nodes.add(nodeVal);                	
                    Integer parentVal = Integer.parseInt(digest[2]);
                    parents.put(nodeVal, parentVal);
                    String nodeRank = digest[4];
                    ranks.put(nodeVal, nodeRank);
                }
                raw = br.readLine();
            }
        }
        catch (Exception e) {
            System.out.print(e);
            return;
        }
        return;
	}
	
	private void buildScopedNamesList(File nf, Set<Integer> nodes, Map<String,Integer> names, Map<Integer,String> termToName, Map<String,Integer> synonyms) {
		try{
            final BufferedReader br = new BufferedReader(new FileReader(nf));
            String raw = br.readLine();
            while (raw != null){
                final String[] digest = tabPattern.split(raw);
                Integer nodeNum = Integer.parseInt(digest[0]);
                String name = digest[2];
                String nameType = digest[6];
                if (nodes.contains(nodeNum) && SCIENTIFICNAMETYPE.equalsIgnoreCase(nameType)){
                	names.put(name,nodeNum);
                	termToName.put(nodeNum,name);
                }
                if (nodes.contains(nodeNum) && SYNONYMNAMETYPE.equalsIgnoreCase(nameType)){
                	synonyms.put(name,nodeNum);   //should we be trying to capture more than one here?
                }
                raw = br.readLine();
            }			
		}
		catch (Exception e){
			System.out.print(e);
			return;
		}
		// TODO Auto-generated method stub
		return;
	}

	
	/**/
	/* Attachment code here */
	public boolean canAttach(){
		return true;
	}


	@Override
	public void attach(File ncbiSource, TaxonStore target, String attachment, String prefix) {
		final File namesFile = new File(ncbiSource.getAbsolutePath()+'/'+NAMESFILENAME);
		final File nodesFile = new File(ncbiSource.getAbsolutePath()+'/'+NODESFILENAME);
		final Set <Integer> nodesInScope = new HashSet<Integer>(10000);
		final Map<Integer,String> nodeRanks = new HashMap<Integer,String>(10000);
		final Map<Integer,Integer> nodeParents = new HashMap<Integer,Integer>(10000);
		buildScopedNodesList(nodesFile,nodesInScope,nodeRanks,nodeParents);
		Map <String,Integer> namesInScope = new HashMap<String,Integer>(nodesInScope.size());
		Map <Integer,String> termToName = new HashMap<Integer,String>(nodesInScope.size());
		Map <String,Integer> synonymsInScope = new HashMap<String,Integer>(nodesInScope.size());
		buildScopedNamesList(namesFile,nodesInScope,namesInScope, termToName,synonymsInScope);
		System.out.println("Node count = " + nodesInScope.size());
		System.out.println("Name count = " + namesInScope.size());
		System.out.println("Synonym count = " + synonymsInScope.size());
		int nameHits = 0;
		Term parentTerm = null;
		if (!"".equals(attachment)){
			parentTerm = target.getTermbyName(attachment);
			if (parentTerm == null){   //parent is unknown
				if (!target.isEmpty()){
					System.err.println("Can not attach " + namesFile.getAbsolutePath() + " specified parent: " + attachment + " is unknown to " + target);
					return;
				}
				else { // attachment will be added first to provide a root for an otherwise empty target
					parentTerm = target.addTerm(attachment);
					logger.info("Assigning " + attachment + " as root");
				}
			}
		}
        final Collection<Term> terms = target.getTerms();	
        final Integer parentNode = namesInScope.get(parentTerm.getLabel());
        logger.info("parent Node is " + parentNode);
        Deque<Integer> processList = new ArrayDeque<Integer>(100);
        processList.add(parentNode);
    	//target.addTerm(termToName.get(parentNode));
        int count = 0;
        while(!processList.isEmpty()){
        	Integer current = processList.pop();
        	//logger.info("Current = " + current.intValue());
        	for(Integer node : nodeParents.keySet()){   //add the children
        		if (nodeParents.get(node).equals(current)){
        			Term child = target.getTermbyName(termToName.get(node));
        			if (child == null){
        				child = target.addTermbyID("NCBITaxon:" + node.toString(),  termToName.get(node));
        			}
        			target.attachParent(child, target.getTermbyName(termToName.get(current)));
        			processList.add(node);
        		}
        	}
        	
        	count++;
        	if (count % 100 == 0)
        		logger.info("Count = " + count);
        }
        logger.info("Done; count = " + count);
//		for(Term term : terms){		
//			if(namesInScope.containsKey(term.getLabel())){
//				SynonymI oldSyn = term.getOldSynonym("NCBITaxon");
//				String newID = synonymsInScope.get(term.getLabel()).toString();
//				if (oldSyn != null && !(newID.equals(oldSyn.getID()))){
//					System.err.println("An NCBI name (" + term.getLabel() + ") has a changed ID (" + newID + "); was " + oldSyn.getID());  // what to do
//				}
//				SynonymI newSyn = target.makeSynonym(namesInScope.get(term.getLabel()).toString(), synonymsInScope.get(term.getLabel()).toString(), "NCBITaxon");
//				term.addSynonym(newSyn);
//				nameHits++;
//			}
//		}
//		System.out.println("Names matching NCBI names = " + nameHits);
		// TODO Auto-generated method stub
		
	}

	
	
}
