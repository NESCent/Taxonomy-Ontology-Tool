package org.nescent.VTO.lib;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.Collection;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class TestPaleoDBMerger {

	private PaleoDBMerger testMerger;
	
	private final File testFile1 = new File("src/SampleProcessFiles/PaleoDBNameFile.txt");
	private final File testBadNames1 = new File("src/SampleProcessFiles/PaleoDBBadNameFile1.txt");
	private final File testBadNames2 = new File("src/SampleProcessFiles/PaleoDBBadNameFile2.txt");
	private final File testBadNames3 = new File("src/SampleProcessFiles/PaleoDBBadNameFile3.txt");
	private final String PaleoDBPrefix = "PlDB";
	
	@Before
	public void setUp() throws Exception {
		testMerger = new PaleoDBMerger();
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testMerge() {
		testMerger.setSource(testFile1);
		testMerger.merge(PaleoDBPrefix);
	}

	@Test
	public void testGetNames() {
		Collection<String> nameList;
		try {
			nameList = testMerger.getNames(testFile1);
			assertNotNull(nameList);
			assertEquals(nameList.size(),3);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	@Test(expected=ParseException.class) 
	public void testBadGetNames1() throws IOException,ParseException{
		Collection<String> nameList = testMerger.getNames(testBadNames1);		
	}
	
	@Test(expected=ParseException.class) 
	public void testBadGetNames2() throws IOException,ParseException{
		Collection<String> nameList = testMerger.getNames(testBadNames2);		
	}
	
	@Test(expected=ParseException.class) 
	public void testBadGetNames3() throws IOException,ParseException{
		Collection<String> nameList = testMerger.getNames(testBadNames3);		
	}

}
