package org.nescent.VTO.lib;

import java.io.File;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class TestIOCMerger {

	private IOCMerger testMerger;
	private File testFile;
	private TaxonStore testStore;
	
	@Before
	public void setUp() throws Exception {
		testMerger = new IOCMerger();
		testFile = new File("src/SampleProcessFiles/ioc-names-2.4.xml");
		testStore = new OBOStore(null, null, null);   //TODO put something here
		testMerger.setSource(testFile);
		testMerger.setTarget(testStore);
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testMerge() {
		testMerger.merge("IOC");
	}

}
