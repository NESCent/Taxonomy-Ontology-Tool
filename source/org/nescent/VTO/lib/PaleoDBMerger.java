package org.nescent.VTO.lib;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.ParseException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.log4j.Logger;


/**
 * This class merges names from PaleoDB using the web api (only known documentation for this is at
 * https://www.nescent.org/wg_evoinfo/PaleoDB).  Unlike the web-based CoLMerger, this merger uses a
 * source file of taxonomic names to merge (just a plain-text list, one name per line).  It queries
 * each name against paleoDB if the query is successful, it parses out the primary name, synonyms and
 * the path to root.  If the name already exists as a synomym...  If the name already exists as a primary
 * name....  Otherwise it creates a new term for the primary name, adds synonyms and then adds terms for each
 * ancestor until one matches an existing term.  If an ancestor name matches a synonym of an existing term...
 * 
 * It doesn't support attaching in the usual sense of building a tree below a particular node, because paleoDB
 * doesn't presently support queries for or return results containing children of a specified node.  However, if 
 * every child of a taxon is included in a list, you will get the same result.  
 * 
 * PaleoDB does return identifiers, but it is presently unknown if these are stable.
 * 
 * @author peter
 *
 */
public class PaleoDBMerger implements Merger {

	private static final Pattern numeric = Pattern.compile("[0-9]+");
	private static final Pattern monomial = Pattern.compile("[A-Za-z]+");
	private static final Pattern binomial = Pattern.compile("[A-Za-z]+\\s[a-z]+");
	
	private File source;
	private TaxonStore target;
	private SynonymSource preserveSynonyms;
	
	final private static String PBDBURL = "http://www.catalogueoflife.org";
	final private static String PBDBSUFFIX = "/annual-checklist/";

	
	static final Logger logger = Logger.getLogger(PaleoDBMerger.class.getName());
	@Override
	public boolean canAttach() {
		return false;
	}

	@Override
	/**
	 * may return true if PaleoDB ID's are found to be stable
	 */
	public boolean canPreserveID() {
		return false;
	}

	@Override
	public void setSource(File source) {
		this.source = source;
	}

	@Override
	public void setTarget(TaxonStore target) {
		this.target=target;
	}

	@Override
	public void setPreserveID(boolean v){
		throw new RuntimeException("This merger can't preserve IDs because TBD");
	}
	
	@Override
	public void setPreserveSynonyms(SynonymSource s){
		preserveSynonyms = s;
	}


	
	@Override
	public void merge(String prefix) {
		DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
		f.setNamespaceAware(true);
	    try {
			final Collection<String> names = getNames(source);
			for(String name : names){
				
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	Collection <String>getNames(File source) throws IOException, ParseException{
		Set<String> result = new HashSet<String>();
        if (source != null){
        	BufferedReader br = null;
        	String raw = "";
            try {
            	br = new BufferedReader(new FileReader(source));
                raw = br.readLine();
                while (raw != null){
                	Matcher monomialMatch = monomial.matcher(raw);
                	if (!monomialMatch.matches()){
                		Matcher binomialMatch = binomial.matcher(raw);
                		if (!binomialMatch.matches()){
                    		throw new ParseException("name has bad characters or syntax: " + raw,0);                			
                		}
                	}                		
                	result.add(raw);   //TODO checking here (non alpha chars, empty lines?)
                	raw = br.readLine();
                }
            }
            finally{
					br.close();
            }
        }
		return result;
	}

	@Override
	public void attach(String parent, String cladeRoot, String prefix) {
		throw new UnsupportedOperationException("PaleoDB Merger can't bulk attach");
	}

}
