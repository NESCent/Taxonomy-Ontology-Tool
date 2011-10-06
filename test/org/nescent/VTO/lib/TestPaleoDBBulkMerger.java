package org.nescent.VTO.lib;

import static org.junit.Assert.*;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jmock.Mockery;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class TestPaleoDBBulkMerger {

	Mockery context = new Mockery();
	
	private PaleoDBBulkMerger testMerger;
	
	private final File testDumpDirectory = new File("src/SampleProcessFiles/Tyrannosaurus");
	
	final String fileSeparator = System.getProperty("file.separator");
	
	private final File testTaxonomic_units1 = new File(testDumpDirectory.getAbsoluteFile() + fileSeparator + "taxonomic_units.dat");
	private final File testSynonym_links1 = new File(testDumpDirectory.getAbsoluteFile() + fileSeparator + "synonym_links.dat");


	@Before
	public void setUp() throws Exception {
		testMerger = new PaleoDBBulkMerger();
		testMerger.setSource(testDumpDirectory);
		testMerger.setTarget(context.mock(TaxonStore.class));
	}

	@After
	public void tearDown() throws Exception {
	}

	
	@Test
	public void testBuildPDBDList() throws Exception{
		List<PBDBItem> items = testMerger.buildPBDBList(testTaxonomic_units1);
		assertEquals(17,items.size());
		
	}
	
	@Test
	public void testBuildSynonymLinks() throws Exception{
		Map<Integer,Integer> links = testMerger.buildSynonymLinks(testSynonym_links1);
		assertEquals(14,links.size());
	}
	
	@Test
	public void testBuildTree() throws Exception{
		List<PBDBItem> items = testMerger.buildPBDBList(testTaxonomic_units1);
		Map<String,Set<String>> testTree = testMerger.buildTree(items);
		assertEquals(2,testTree.size());
		assertTrue(testTree.containsKey("Tyrannosaurus"));
		assertTrue(testTree.containsKey("Tyrannosaurinae"));
		assertEquals(1,testTree.get("Tyrannosaurus").size());
		assertTrue(testTree.get("Tyrannosaurus").contains("Tyrannosaurus rex"));
		assertEquals(1,testTree.get("Tyrannosaurinae").size());
		assertTrue(testTree.get("Tyrannosaurinae").contains("Tyrannosaurus"));
	}
	
	@Test
	public void testMerge() {
		assertNotNull(testMerger);
		
		//expectations
		
		testMerger.merge("PBDB");
	}

	@Test
	public void testAttach() {
		
		//expectations
		
		
		testMerger.attach("", "", "PBDB");
	}


}
