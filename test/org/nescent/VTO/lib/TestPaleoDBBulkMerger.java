package org.nescent.VTO.lib;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collection;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestPaleoDBBulkMerger {

	
	private PaleoDBBulkMerger testMerger;
	private TaxonStore testStore;
	
	private final File testDumpDirectory = new File("src/SampleProcessFiles/Tyrannosaurus");
	private final String baseTaxonomy = "src/Ontologies/exto.obo";
	private final String testMergeOntology = "TestPaleoDBMerge.obo";
	private final String testAttachOntology = "TestPaleoDBAttach.obo";
	
	private final File baseTaxonomyFile = new File(baseTaxonomy);
	
	final String fileSeparator = System.getProperty("file.separator");
	
	private final File testValidTaxa1 = new File(testDumpDirectory.getAbsoluteFile() + fileSeparator + "valid_taxa.csv");
	private final File testInvalidTaxa1 = new File(testDumpDirectory.getAbsoluteFile() + fileSeparator + "invalid_taxa.csv");

	private static String testOntologiesPath;
	private static String testProcessFilesPath;
	private static String testOutputPath;
	
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
        testOntologiesPath = headPath.substring(0,cutPoint+1) + "src/Ontologies/";
        testProcessFilesPath = headPath.substring(0,cutPoint+1) + "src/SampleProcessFiles/";
        testOutputPath = headPath.substring(0,cutPoint+1) + "output/";
	}

	
	
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
		Map<String,PBDBItem> items = testMerger.buildPBDBList(testValidTaxa1);
		assertEquals(2,items.size());
		
	}
	
	
	@Test
	public void testBuildTree() throws Exception{
		Map<String,PBDBItem> items = testMerger.buildPBDBList(testValidTaxa1);
		Map<String,PBDBItem> invalidItems = testMerger.buildPBDBList(testInvalidTaxa1);
		Map<String,String> testTree = testMerger.buildTree(items,invalidItems);
		assertEquals(2,testTree.size());
		assertTrue(testTree.containsKey("Tyrannosaurus rex"));
		assertTrue(testTree.containsKey("Tyrannosaurus"));
		assertEquals("Tyrannosaurinae",testTree.get("Tyrannosaurus"));
		assertEquals("Tyrannosaurus",testTree.get("Tyrannosaurus rex"));
	}
	
	//don't need to load the base taxonomy for every test
	private void initBaseTaxonomy(){
		Merger initMerger = new OBOMerger();
		initMerger.setSource(baseTaxonomyFile);
		initMerger.setTarget(testStore);
		initMerger.attach("Chordata", "Chordata", "EXTO");
	}
	
	@Test
	public void testMerge() {
		testStore = new OBOStore(testOutputPath+testMergeOntology,null,"test-namespace");  //will need to flesh this out eventually
		testMerger.setTarget(testStore);
		initBaseTaxonomy();
		testStore.saveStore();
		final String tName = "Tyrannosaurus";
		final String trName = "Tyrannosaurus rex";

	}

	@Test
	public void testAttach() {
		testStore = new OBOStore(testOutputPath+testAttachOntology,null,"test-namespace");  //will need to flesh this out eventually
		testMerger.setTarget(testStore);
		initBaseTaxonomy();
		testStore.saveStore();		
		testMerger.attach("Tyrannosauridae", "", "PBDB");
	}


}
