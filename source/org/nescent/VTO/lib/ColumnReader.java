/*
 * VTOTool - a utility build taxonomy ontologies from multiple sources 
 * 
 * Copyright (c) 2007-2011 Peter E. Midford
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
 * Created June 2010, based on ItemReader from OBOVocab
 * Last updated on August 22, 2011
 *
 */
package org.nescent.VTO.lib;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

/**
 * 
 * @author pmidford
 *
 */
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
	private List<ColumnType> headers;

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
	public void setColumns(final List<ColumnType> columns){
		headers = columns;
		for(ColumnType column : columns){
			boolean matched = false;
			for(KnownField k : KnownField.values()){
				if (k.toString().equalsIgnoreCase(column.getType())){
					fields.add(k);
					matched = true;
				}
			}
			if (!matched){
				logger.error("Unknown column type specified");
				fields.add(KnownField.IGNORE);
			}
		}
	}


	/**
	 * 
	 * @param f
	 * @param headersFirst
	 * @return list of items parsed from the spreadsheet file
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
			catch (IOException e) {
				System.out.print(e);
				return result;
			}
		}
		return result; // for now
	}

	// what checks are needed?
	private boolean checkEntry(String[] line){
		if (line.length < 3)
			return false;
		return true;
	}



	private Item processLine(String[] digest, ItemList resultList){
		final Item result = new Item(); 
		for(int i = 0;i<fields.size();i++){   //this allows ignoring trailing fields that are undefined in the xml columns element
			if (digest.length>i){ //this allows files that are (unfortunately) missing trailing empty fields (Excel can write tab files like this)
				String rawColumn = digest[i]; 
				if (rawColumn.length() > 2 && rawColumn.charAt(0) == '"' && rawColumn.charAt(rawColumn.length()-1) == '"')
					rawColumn = rawColumn.substring(1,rawColumn.length()-1);
				final String curColumn = rawColumn.trim();  //At least some sources have extra trailing white space in names 
				result.putName(fields.get(i),curColumn);
			}
		}
		return result;
	}

	public ColumnType getColumn(KnownField field) {
		for (ColumnType c : headers){
			if (field.getCannonicalName().equals(c.getType()))
				return c;
		}
		return null;
	}        

}
