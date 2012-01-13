package org.nescent.VTO.lib;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;


//target class may be should be @deprecated?
public class TestITISMerger {
	
	private ITISMerger testMerger;
	private TaxonStore testStore;

	@Before
	public void setUp() throws Exception {
		testMerger = new ITISMerger();
		testStore = new OBOStore(null,null,null);
	}

	@Test
	public void testMerge() {
		fail("Not yet implemented");
	}

	@Test
	public void testCanAttach() {
		fail("Not yet implemented");
	}

	@Test
	public void testAttach() {
		fail("Not yet implemented");
	}

}
