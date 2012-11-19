package org.nescent.VTO.lib;

import static org.junit.Assert.*;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;

import org.apache.log4j.BasicConfigurator;
import org.junit.Before;
import org.junit.Test;
import org.obo.datamodel.OBOClass;

public class TestOBOTerm {

	
	private static String testImportsPath;

	private final String baseTaxonomy = "src/Ontologies/exto.obo";
	private OBOStore testStore;
	
	

	@Before
	public void setUp() throws Exception {
		BasicConfigurator.configure();
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
        testImportsPath = headPath.substring(0,cutPoint+1) + baseTaxonomy;
		File test1 = new File (testImportsPath);
        testStore = new OBOStore(testImportsPath+"src/Ontologies/exto_out.obo","exto","test-taxonomy");
        OBOMerger testMerger = new OBOMerger();
        testMerger.setTarget(testStore);
        testMerger.setSource(test1);
        testMerger.attach("Chordata", "Chordata", "EXTO");
	}

	@Test
	public void testOBOTerm() {
		fail("Not yet implemented");
	}

	@Test
	public void testGetOldSynonym() {
		fail("Not yet implemented");
	}

	@Test
	public void testAddSynonym() {
		fail("Not yet implemented");
	}

	@Test
	public void testAsOBOClass() {
		Term testTerm = testStore.getTerm("EXTO:0000006");
		assertNotNull(testTerm);
		OBOClass testClass = testTerm.asOBOClass();
		assertNotNull(testClass);
	}

	@Test
	public void testGetLabel() {
		fail("Not yet implemented");
	}

	@Test
	public void testGetAncestors(){
		Term testTerm = testStore.getTerm("EXTO:0000006");
		assertNotNull(testTerm);
		List<Term> ancestorList = testTerm.getAncestors();
		assertFalse(ancestorList.isEmpty());
		for (Term a : ancestorList){
			System.out.println(a.getLabel() + "\t" + a.getID());
		}
	}
	
}
