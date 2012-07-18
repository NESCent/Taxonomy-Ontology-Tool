package org.nescent.VTO.lib;

import static org.junit.Assert.*;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestColumnReader {
	
	private ColumnReader testReader;
	
	final static String AMPHIBIASTR = "AmphibiaWeb.txt";
	final static List<ColumnType>amphibiaColumns = new ArrayList<ColumnType>();

	private static String testImportsPath;

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
        amphibiaColumns.add(new ColumnType("order"));
        amphibiaColumns.add(new ColumnType("family"));
        amphibiaColumns.add(new ColumnType("subfamily"));
        amphibiaColumns.add(new ColumnType("genus"));
        amphibiaColumns.add(new ColumnType("species"));
	}


	@Before
	public void setUp() throws Exception {
	}

	@Test
	public void testColumnReader() {
		ColumnReader dummyReader = new ColumnReader("\t");
	}

	@Test
	public void testSetColumns() {		
		ColumnReader dummyReader = new ColumnReader("\t");
		dummyReader.setColumns(amphibiaColumns);
	}



	@Test
	public void testProcessCatalog() {
		testReader = new ColumnReader("\t");
		File test1 = new File (testImportsPath + AMPHIBIASTR);
		testReader.setColumns(amphibiaColumns);
		ItemList items = testReader.processCatalog(test1, true);
		assertNotNull(items);
		assertEquals(items.size(),7046);
	}

}
