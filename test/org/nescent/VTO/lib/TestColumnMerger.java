package org.nescent.VTO.lib;

import static org.junit.Assert.*;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestColumnMerger {

	private ColumnMerger testMergerCommaFormat;
	private ColumnMerger testMergerTabFormat;
	private TaxonStore testStore;
	
	final static private Map<Integer,String> emptySynPrefixes = new HashMap<Integer,String>();
	
	final static String AMPHIBIASTR = "AmphibiaWeb.txt";
	final static List<String>amphibiaColumns = new ArrayList<String>();

	private static String testImportsPath;
	
	static final Logger logger = Logger.getLogger(TestColumnMerger.class.getName());

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
        ClassLoader sysCL = ClassLoader.getSystemClassLoader();
        if (!(sysCL instanceof URLClassLoader)){
            throw new RuntimeException("Data area can not be located");
        }
        final URL headURL = ((URLClassLoader)sysCL).getURLs()[0];
        String headPath = headURL.getPath();
        int cutPoint;
        if (headPath.charAt(headPath.length()-1) == '/'){
            cutPoint = headPath.lastIndexOf('/',headPath.length()-2);
        }
        else {
            cutPoint = headPath.lastIndexOf('/');
            cutPoint = headPath.lastIndexOf('/',cutPoint-2);  // cut twice
        }
        testImportsPath = headPath.substring(0,cutPoint+1) + "src/imports/";
        amphibiaColumns.add("order");
        amphibiaColumns.add("family");
        amphibiaColumns.add("subfamily");
        amphibiaColumns.add("genus");
        amphibiaColumns.add("species");
        
	}

	@Before
	public void setUp() throws Exception {
		BasicConfigurator.configure();
		testStore = new OBOStore(null,null,null);
	}


	@Test
	public void testSetColumns() {
		ColumnMerger dummyMerger = new ColumnMerger("\t");
		dummyMerger.setColumns(amphibiaColumns, emptySynPrefixes);
	}

	@Test
	public void testMergeTabFormat() {
		testMergerTabFormat = new ColumnMerger("\t");
		testMergerTabFormat.setTarget(testStore);
		File test1 = new File (testImportsPath + AMPHIBIASTR);
		testMergerTabFormat.setColumns(amphibiaColumns, emptySynPrefixes);
		testMergerTabFormat.merge("ATO");
		//fail("Not yet implemented");
	}


	@Test
	public void testCanAttach() {
		assert(testMergerCommaFormat.canAttach());
		assert(testMergerTabFormat.canAttach());
	}

	@Test
	public void testAttachTabFormat() {
		testMergerTabFormat = new ColumnMerger("\t");
		testMergerTabFormat.setTarget(testStore);
		File test1 = new File (testImportsPath + AMPHIBIASTR);
		testMergerTabFormat.setColumns(amphibiaColumns, emptySynPrefixes);
		testMergerTabFormat.attach("Amphibia","Amphibia","ATO");
	}


}
