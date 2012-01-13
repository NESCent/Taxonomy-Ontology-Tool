package org.nescent.VTO.lib;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

import java.util.Collection;

import javax.xml.parsers.DocumentBuilderFactory;

import junit.framework.Assert;

import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.nescent.VTO.lib.CoLMerger.NamePair;

public class TestCoLMerger {
	
	static final String TESTTAXON1 = "Homo sapiens";
	static final String TESTTAXON2 = "Danio rerio";
	static final String TESTTAXON3 = "Ictalurus punctatus";
	static final String TESTTAXON4 = "Aphelocoma coerulescens";
	
	private CoLMerger testMerger;
	private TaxonStore testStore;
	
	static final Logger logger = Logger.getLogger(TestColumnMerger.class.getName());


	@Before
	public void setUp() throws Exception {
		testMerger = new CoLMerger();
		testStore = new OBOStore(null,null,null);
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testCanAttach() {
		assertFalse(testMerger.canAttach());
	}

	@Test
	public void testQueryOneName(){
		DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
		f.setNamespaceAware(true);
		Collection<NamePair> taxon1Synonyms = testMerger.queryOneName(TESTTAXON1, f);
		Assert.assertEquals(0,taxon1Synonyms.size());
		Collection<NamePair> taxon2Synonyms = testMerger.queryOneName(TESTTAXON2, f);
		Assert.assertEquals(9,taxon2Synonyms.size());
		Collection<NamePair> taxon3Synonyms = testMerger.queryOneName(TESTTAXON3, f);
		Assert.assertEquals(1,taxon3Synonyms.size());
		Collection<NamePair> taxon4Synonyms = testMerger.queryOneName(TESTTAXON4, f);
		Assert.assertEquals(0, taxon4Synonyms.size());
		System.out.println("There are " + taxon4Synonyms.size() + " synonyms of " + TESTTAXON4);
	}

	
	@Test
	public void testMerge() {
		testMerger.setTarget(testStore);
		testMerger.merge("CoL");
	}

	@Test
	public void testAttach() {
		testMerger.setTarget(testStore);
		testMerger.merge("CoL");
	}

}
