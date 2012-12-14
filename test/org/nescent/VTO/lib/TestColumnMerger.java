package org.nescent.VTO.lib;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestColumnMerger {

	private ColumnMerger testMergerCommaFormat;
	private ColumnMerger testMergerTabFormat;
	private TaxonStore testStore;
	
	final static String AMPHIBIASTR = "AmphibiaWeb.txt";
	final static List<ColumnType>amphibiaColumns = new ArrayList<ColumnType>();
	private final String baseTaxonomy = "src/Ontologies/exto.obo";
	private final File baseTaxonomyFile = new File(baseTaxonomy);

	private static String testImportsPath;
	
	final static String testURI = "http://www.fishbase.org/Summary/SpeciesSummary.php?id=*xref";
	private ColumnMerger testMerger;
	
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
        amphibiaColumns.add(new ColumnType("order"));
        amphibiaColumns.add(new ColumnType("family"));
        amphibiaColumns.add(new ColumnType("subfamily"));
        amphibiaColumns.add(new ColumnType("genus"));
        amphibiaColumns.add(new ColumnType("species"));
        
	}

	@Before
	public void setUp() throws Exception {
		BasicConfigurator.configure();
		testMerger = new ColumnMerger("\t");
	}

	
	//don't need to load the base taxonomy for every test
	private void initBaseTaxonomy(){
		Merger initMerger = new OBOMerger();
		initMerger.setSource(baseTaxonomyFile);
		initMerger.setTarget(testStore);
		initMerger.attach("Chordata", "Chordata", "EXTO");
	}


	@Test
	public void testSetColumns() {
		ColumnMerger dummyMerger = new ColumnMerger("\t");
		dummyMerger.setColumns(amphibiaColumns);
	}

	@Test
	public void testMergeTabFormat() {
		testMergerTabFormat = new ColumnMerger("\t");
		testMergerTabFormat.setTarget(testStore);
		File test1 = new File (testImportsPath + AMPHIBIASTR);
		testMergerTabFormat.setColumns(amphibiaColumns);
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
		testMergerTabFormat.setColumns(amphibiaColumns);
		testMergerTabFormat.attach("Amphibia","Amphibia","ATO");
	}


}
