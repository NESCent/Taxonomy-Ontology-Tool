package org.nescent.VTO.lib;

import static org.junit.Assert.*;

import java.io.File;

import org.junit.Before;
import org.junit.Test;

public class TestOBOMerger {
	
	private OBOMerger testMerger;
	private File testFile;
	private TaxonStore testStore;

	@Before
	public void setUp() throws Exception {
		testMerger = new OBOMerger();
		testStore = new OBOStore(null,null,null);
	}

	@Test
	public void testCanAttach() {
		fail("Not yet implemented");
	}

	@Test
	public void testMerge() {
		fail("Not yet implemented");
	}

	@Test
	public void testAttach() {
		fail("Not yet implemented");
	}

}
