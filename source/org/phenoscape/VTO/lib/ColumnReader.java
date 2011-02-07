/*
 * OBOVocab - a tool for building OBO files from term lists
 * 
 * Created on Jan 30, 2008
 * Last updated on July 24,2008
 *
 */
package org.phenoscape.VTO.lib;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

public class ColumnReader {
    
    
    static final String[] IGNORELIST = {};
    
    
    static final String SYNONYMCOLUMNHEADER1 = "synonym";
    static final String SYNONYMCOLUMNHEADER2 = "synonyms";
    static final String SYNONYMCOLUMNHEADER3 = "gaa_name";  //not sure what this is
    static final String SYNONYMCOLUMNHEADER4 = "itis_names";
    static final String DESCRIPTIONSTR = "Description";
    static final String STATUSSTR = "Status";

    final Pattern splitPattern;
    

    private List<KnownField> fields = new ArrayList<KnownField>();
    private List<Integer> synonymFields = new ArrayList<Integer>(); 
    
	static final Logger logger = Logger.getLogger(ColumnReader.class.getName());
    
    
    /**
     * Constructor just sets the column delimiting character (generally tab or comma) as a string.
     * @param splitString
     */
    public ColumnReader(String splitString){
    	splitPattern = Pattern.compile(splitString);
    }
    
    /**
     * This method attaches known tags to the columns (or ignore) as specified in columns element in taxonOptions.xml.
     * Besides tags, this method provides a way to specify the source of synonyms (e.g., from another known database).
     * This allows column configuration without guessing from labels appearing in column headers
     * @param columns
     * @param synonymRefs
     */
    public void setColumns(List<String> columns, Map<Integer,String>synonymRefs){
        for(String column : columns){
        	boolean matched = false;
        	for(KnownField k : KnownField.values()){
        		if (k.toString().equalsIgnoreCase(column)){
        			fields.add(k);
        			matched = true;
        		}
        	}
        	if (!matched){
        		logger.error("Unknown column type specified");
        		fields.add(KnownField.IGNORE);
        	}
        }
        for(KnownField k : fields){
        	if (k == KnownField.SYNONYM || k == KnownField.SYNONYMS){
        		//???
        	}
        }
    }


    /**
     * 
     * @param f
     * @param headersFirst
     * @return
     */
    public ItemList processCatalog(File f,boolean headersFirst) {
        final ItemList result = new ItemList();
        result.addColumns(fields);
        String raw = "";
        if (f != null){
            try {
                final BufferedReader br = new BufferedReader(new FileReader(f));
                if (headersFirst){  //ignore headers, fields are defined in the xml configuration
                    raw=br.readLine();
                }
                raw = br.readLine();
                while (raw != null){
                    final String[] digest = splitPattern.split(raw);
                    if (checkEntry(digest)){
                    	Item foo = processLine(digest,result);
                    	result.addItem(foo);
                    }
                    else{
                        System.err.println("Bad line: " + raw);
                    }
                    raw = br.readLine();
                }
            }
            catch (Exception e) {
                System.out.print(e);
                return result;
            }
        }
        return result; // for now
    }

    // what checks are needed?
    private boolean checkEntry(String[] line){
        return true; 
    }
    


    private Item processLine(String[] digest, ItemList resultList){
    	final Item result = new Item(); 
    	for(int i = 0;i<fields.size();i++){   //this allows ignoring trailing fields that are undefined in the xml columns element
    		String curColumn = digest[i];
    		if (curColumn.length() > 2 && curColumn.charAt(0) == '"' && curColumn.charAt(curColumn.length()-1) == '"')
    			curColumn = curColumn.substring(1,curColumn.length()-1);
    		curColumn = curColumn.trim();  //At least some sources have extra trailing white space in names 
    		if (fields.get(i).isTaxon()){
    			if (curColumn.length()>0){
    				result.putName(fields.get(i),curColumn);
    			}
    		}
    		else if (synonymFields.contains(i)){
    			if (curColumn.length()>2){
    				String [] syns = curColumn.split(", ");
    				for(String syn : syns)
    					result.addSynonym(fields.get(i).toString(),syn,"", resultList);
    			}
    		}
    		else if (fields.get(i).equals(KnownField.DELIMITEDNAME)){
    			if (curColumn.length()>0){
    				result.putName(fields.get(i), curColumn);
    			}
    		}
    	}
    	return result;
   	}



    
    private void setupDefaultFields(){
        System.err.println("Please set up default Fields!");
        fields = null;
    }
    
    
    
        

}
