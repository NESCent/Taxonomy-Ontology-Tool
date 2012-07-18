package org.nescent.VTO.lib;



import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 
 * @author pmidford
 *
 */
public class ItemList {

	private final List<Item> contents;
	private final Set<KnownField> columnNames;
    private Map<String,String> synonymSources;  //maps a synonym column to an attributed source URL (these ought to be in the source sheet)
    private Map<String,String> dbxRefSources;  //maps a column name to a database prefix
    private List<String> uriList;

    //TODO these don't belong here - these aren't general or reusable
    private final static String ITISCOLUMNTAG = "itis_names";
    private final static String ITISURL = "http://www.itis.gov/";
    private final static String GAACOLUMNTAG = "gaa_name";
    private final static String GAAURL = "http://www.iucnredlist.org/initiatives/amphibians";

	public ItemList(){
		contents = new ArrayList<Item>();
		columnNames = new HashSet<KnownField>();
		synonymSources = new HashMap<String,String>();
        synonymSources.put(ITISCOLUMNTAG, ITISURL);
        synonymSources.put(GAACOLUMNTAG, GAAURL);
	}
	
	public void addItem (Item i){
		contents.add(i);
	}
	
	public void addList (List<Item> items){
		contents.addAll(items);
	}
	
	public List<Item> getContents(){
		return contents;
	}

	public void addColumns(List<KnownField> fields) {
		columnNames.addAll(fields);
	}
	
	public boolean hasColumn(KnownField column){
		return columnNames.contains(column);
	}

	public String getSynonymSource(String source) {
		return synonymSources.get(source);
	}

	public boolean hasSynonymSourcecontainsKey(String source) {
		return synonymSources.containsKey(source);
	}
	
	public int size(){
		return contents.size();
	}
	
}
