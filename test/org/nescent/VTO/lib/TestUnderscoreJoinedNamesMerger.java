package org.nescent.VTO.lib;

import static org.junit.Assert.*;

import java.io.File;

import org.junit.Before;
import org.junit.Test;

public class TestUnderscoreJoinedNamesMerger {
	
	private UnderscoreJoinedNamesMerger testMerger;
	private File testFile;
	private TaxonStore testStore;
	
	private final static String commaStr = ",";

	@Before
	public void setUp() throws Exception {
		testMerger = new UnderscoreJoinedNamesMerger(commaStr);
		testStore = new OBOStore(null,null,null);
	}

	@Test
	public void testUnderscoreJoinedNamesMerger() {
		fail("Not yet implemented");
	}

	@Test
	public void testSetColumns() {
		fail("Not yet implemented");
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
