package org.nescent.VTO.lib;

import static org.junit.Assert.*;

import java.io.File;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class TestPaleoDBBulkMerger {

	
	private PaleoDBBulkMerger testMerger;
	
	private final File testTaxonomic_units1 = new File("src/SampleProcessFiles/Tyrannosaurus/taxonomic_units.dat");

	private final String testLine1 = "67649||\"Albertosaurus megagracilis\"|invalid|\"junior synonym\"|||||\"2005-09-09 18:02:17\"|Tyrannosaurus|57107||Chromista|species|09/22/2005|";
	private final String testLine2 = "38613||Tyrannosaurus|valid||||||\"2003-01-23 15:24:00\"|Tyrannosaurinae|38613||Chromista|genus|07/30/2011|";
	@Before
	public void setUp() throws Exception {
		testMerger = new PaleoDBBulkMerger();
		testMerger.setSource(testTaxonomic_units1);
	}

	@After
	public void tearDown() throws Exception {
	}

	
	@Test
	public void testProcessLine(){
		PaleoDBBulkMerger.PBDBElement element1 = testMerger.processLine(testLine1);
		assertNotNull(element1);
		PaleoDBBulkMerger.PBDBElement element2 = testMerger.processLine(testLine2);
		assertNotNull(element2);
	}
	
	
	@Test
	public void testMerge() {	
		fail("Not yet implemented");
	}

	@Test
	public void testAttach() {
		testMerger.attach("", "", "");
	}

	@Test
	public void testSetColumns() {
		fail("Not yet implemented");
	}

	
}
