package org.nescent.VTO.lib;

import static org.junit.Assert.*;

import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.Test;

public class TestColumnMerger {

	private ColumnMerger testMerger_comma;
	private ColumnMerger testMerger_tab;
	private TaxonStore testStore;
	
	static final Logger logger = Logger.getLogger(TestColumnMerger.class.getName());

	@Before
	public void setUp() throws Exception {
		testMerger_comma = new ColumnMerger(",");
		testMerger_tab = new ColumnMerger("\t");
		testStore = new OBOStore(null,null,null);
	}


	@Test
	public void testSetColumns() {
		fail("Not yet implemented");
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
