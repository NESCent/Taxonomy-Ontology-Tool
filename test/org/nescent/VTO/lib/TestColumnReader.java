package org.nescent.VTO.lib;

import static org.junit.Assert.*;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestColumnReader {
	
	private ColumnReader testReader;

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
	}


	@Before
	public void setUp() throws Exception {
		testReader = new ColumnReader(":");
	}

	@Test
	public void testColumnReader() {
		File test1 = new File (testImportsPath + "AmphibiaWeb.txt");
		ItemList items = testReader.processCatalog(test1, true);
		assertNotNull(items);
		assertEquals(items.size(),7046);
	}

	@Test
	public void testSetColumns() {
		fail("Not yet implemented");
	}

	@Test
	public void testProcessCatalog() {
		fail("Not yet implemented");
	}

}
