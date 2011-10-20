package org.nescent.VTO.lib;

import static org.junit.Assert.*;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class TestPaleoDBBulkMerger {

	Mockery context = new Mockery();
	
	private PaleoDBBulkMerger testMerger;
	
	private final File testDumpDirectory = new File("src/SampleProcessFiles/Tyrannosaurus");
	
	final String fileSeparator = System.getProperty("file.separator");
	
	private final File testValidTaxa1 = new File(testDumpDirectory.getAbsoluteFile() + fileSeparator + "valid_taxa.csv");
	private final File testInvalidTaxa1 = new File(testDumpDirectory.getAbsoluteFile() + fileSeparator + "invalid_taxa.csv");


	@Before
	public void setUp() throws Exception {
		testMerger = new PaleoDBBulkMerger();
		testMerger.setSource(testDumpDirectory);
	}

	@After
	public void tearDown() throws Exception {
	}

	
	@Test
	public void testBuildPDBDList() throws Exception{
		List<PBDBItem> items = testMerger.buildPBDBList(testValidTaxa1);
		assertEquals(2,items.size());
		
	}
	
	@Test
	public void testBuildSynonymLinks() throws Exception{
		Map<String,String> links = testMerger.buildSynonymLinks(testInvalidTaxa1);
		assertEquals(10,links.size());
	}
	
	@Test
	public void testBuildTree() throws Exception{
		List<PBDBItem> items = testMerger.buildPBDBList(testValidTaxa1);
		Map<String,String> testTree = testMerger.buildTree(items);
		assertEquals(2,testTree.size());
		assertTrue(testTree.containsKey("Tyrannosaurus rex"));
		assertTrue(testTree.containsKey("Tyrannosaurus"));
		assertEquals("Tyrannosaurinae",testTree.get("Tyrannosaurus"));
		assertEquals("Tyrannosaurus",testTree.get("Tyrannosaurus rex"));
	}
	
	@Test
	public void testMerge() {

		final TaxonStore testStore = context.mock(TaxonStore.class);
		testMerger.setTarget(testStore);
		
		final String tName = "Tyrannosaurus";
		final String trName = "Tyrannosaurus rex";

		//expectations

		context.checking(new Expectations() {{
			allowing(testStore).getTerms(); will(returnValue(any(Collection.class)));
			//oneOf(testStore).addTerm(tName);
			//oneOf(testStore).addTerm(trName);
			
		}});
		
		testMerger.merge("PBDB");
	}

	@Test
	public void testAttach() {
		final TaxonStore testStore = context.mock(TaxonStore.class);
		testMerger.setTarget(testStore);

		final String tName = "Tyrannosaurus";
		final String trName = "Tyrannosaurus rex";
		final String tnName = "Tyrannosaurinae";


		//expectations
		context.checking(new Expectations() {{
			allowing(testStore).getTerms(); //will(returnValue(any(Collection.class)));
			allowing(testStore).addTerm(tName);
			allowing(testStore).addTerm(trName);
			allowing(testStore).addTerm(tnName);
			
		}});
		
		
		testMerger.attach("", "", "PBDB");
	}


}
