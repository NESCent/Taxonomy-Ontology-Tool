package org.phenoscape.VTO.lib;

import static org.junit.Assert.*;

import java.io.File;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class TestIOCMerger {

	IOCMerger testMerger;
	File testFile;
	@Before
	public void setUp() throws Exception {
		testMerger = new IOCMerger();
		testFile = new File("/Users/peter/Projects/VTO/Data/ioc-names-2.4.xml");
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testAttach() {
		testMerger.attach(testFile, null, null, null, "IOC");
	}

}
