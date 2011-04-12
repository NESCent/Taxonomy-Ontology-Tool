package org.phenoscape.VTO.lib;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.semanticweb.owlapi.model.OWLNamedIndividual;

public class testOWLUtils {

	OWLUtils testUtils;
	
	@Before
	public void setUp() throws Exception {
		testUtils = new OWLUtils();
	}

	@After
	public void tearDown() throws Exception {
	}


	@Test
	public void testIsEmpty() {
		assertTrue(testUtils.isEmpty());
	}

	@Test
	public void testMakeTerm(){
		OWLNamedIndividual testIndividual = testUtils.makeTerm("0000001", "Chordata");

	}
	
}
