package org.phenoscape.VTO.lib;

import static org.junit.Assert.*;

import java.util.Collection;

import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.phenoscape.VTO.lib.CoLMerger.NamePair;

public class TestCoLMerger {
	
	static final String TESTTAXON1 = "Homo sapiens";
	static final String TESTTAXON2 = "Danio rerio";
	static final String TESTTAXON3 = "Ictalurus punctatus";
	static final String TESTTAXON4 = "Aphelocoma coerulescens";
	
	CoLMerger testMerger;
	


	@Before
	public void setUp() throws Exception {
		testMerger = new CoLMerger();
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testCanAttach() {
		assertFalse(testMerger.canAttach());
		fail("Not yet implemented");
	}

	@Test
	public void testQueryOneName(){
		DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
		f.setNamespaceAware(true);
		Collection<NamePair> taxon1Synonyms = testMerger.queryOneName(TESTTAXON1, f);
		System.out.println("There are " + taxon1Synonyms.size() + " synonyms of " + TESTTAXON1);
		Collection<NamePair> taxon2Synonyms = testMerger.queryOneName(TESTTAXON2, f);
		System.out.println("There are " + taxon2Synonyms.size() + " synonyms of " + TESTTAXON2);
		Collection<NamePair> taxon3Synonyms = testMerger.queryOneName(TESTTAXON3, f);
		System.out.println("There are " + taxon3Synonyms.size() + " synonyms of " + TESTTAXON3);
		Collection<NamePair> taxon4Synonyms = testMerger.queryOneName(TESTTAXON4, f);
		System.out.println("There are " + taxon4Synonyms.size() + " synonyms of " + TESTTAXON4);
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
