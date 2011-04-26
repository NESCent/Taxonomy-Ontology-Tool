package org.nescent.VTO.lib;

import static org.junit.Assert.*;

import java.io.File;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class TestIOCMerger {

	CoLMerger testMerger;
	File testFile;
	@Before
	public void setUp() throws Exception {
		testMerger = new CoLMerger();
		testFile = new File("/Users/peter/Projects/VTO/Data/ioc-names-2.4.xml");
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testMerge() {
		testMerger.merge(testFile, null, "CoL");
	}

}
