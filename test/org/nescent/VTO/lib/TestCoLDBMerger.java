package org.nescent.VTO.lib;

import static org.junit.Assert.*;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class TestCoLDBMerger {
	
	static final String TESTGENUS1 = "Homo";
	static final String TESTGENUS2 = "Ictalurus";
	static final String TESTTAXON1 = "Homo sapiens";
	static final String TESTTAXON2 = "Danio rerio";
	static final String TESTTAXON3 = "Ictalurus punctatus";
	static final String TESTTAXON4 = "Aphelocoma coerulescens";

	static final String CONNECTIONFILESPEC = "testConnection.properties";
	
	CoLDBMerger testMerger;
	private TaxonStore testStore;
	


	@Before
	public void setUp() throws Exception {
		testMerger = new CoLDBMerger();
		testStore = new OBOStore(null, null, null);
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testOpenKBFromConnections() throws SQLException {
		Connection c = testMerger.openKBFromConnections(CONNECTIONFILESPEC);
		assertNotNull(c);
	}

	@Test
	public void testCanAttach() {
		assertTrue(testMerger.canAttach());
	}

	@Test
	public void testMerge() {
		File propFile = new File(CONNECTIONFILESPEC);
		testMerger.setSource(propFile);
		testMerger.merge("");
	}
	
	@Test
	public void testLookupMonomial() throws SQLException{
		Connection c = testMerger.openKBFromConnections(CONNECTIONFILESPEC);
		Set<Integer> testTaxa1 = testMerger.lookupMonomial(c, TESTGENUS1);
		assertFalse(testTaxa1.isEmpty());
		for(Integer taxon : testTaxa1)
			System.out.println(TESTGENUS1 + " " + taxon.toString());
		Set<Integer> testTaxa2 = testMerger.lookupMonomial(c, TESTGENUS2);
		assertFalse(testTaxa2.isEmpty());
		for(Integer taxon : testTaxa2)
			System.out.println(TESTGENUS2 + " " + taxon.toString());
	}

	@Test
	public void testLookupBinomial() throws SQLException{
		Connection c = testMerger.openKBFromConnections(CONNECTIONFILESPEC);
		String[] components = TESTTAXON1.split(" ");
		Set<Integer> testTaxa1 = testMerger.lookupBinomial(c, components[0],components[1]);
		assertFalse(testTaxa1.isEmpty());
		for(Integer taxon : testTaxa1)
			System.out.println(TESTTAXON1 + " " + taxon.toString());
		components = TESTTAXON2.split(" ");
		Set<Integer> testTaxa2 = testMerger.lookupBinomial(c, components[0],components[1]);
		assertFalse(testTaxa2.isEmpty());
		for(Integer taxon : testTaxa2)
			System.out.println(TESTTAXON2 + " " + taxon.toString());
		components = TESTTAXON3.split(" ");
		Set<Integer> testTaxa3 = testMerger.lookupBinomial(c, components[0],components[1]);
		assertFalse(testTaxa3.isEmpty());
		for(Integer taxon : testTaxa3)
			System.out.println(TESTTAXON3 + " " + taxon.toString());
		components = TESTTAXON4.split(" ");
		Set<Integer> testTaxa4 = testMerger.lookupBinomial(c, components[0],components[1]);
		assertFalse(testTaxa4.isEmpty());
		for(Integer taxon : testTaxa4)
			System.out.println(TESTTAXON4 + " " + taxon.toString());
	}

	
	@Test
	public void testLookupSynonyms() throws SQLException{
		Connection c = testMerger.openKBFromConnections(CONNECTIONFILESPEC);
		String[] components = TESTTAXON2.split(" ");
		Set<Integer> testTaxa2 = testMerger.lookupBinomial(c, components[0],components[1]);
		for(Integer targetTaxon : testTaxa2){
			Set<String> synSet = testMerger.lookupSynonyms(c, targetTaxon);
			assertFalse(synSet.isEmpty());
			System.out.println("Synonyms of " + TESTTAXON2 + " are: ");
			for(String syn : synSet){
				System.out.println("   " + syn);
			}
		}
		components = TESTTAXON3.split(" ");
		Set<Integer> testTaxa3 = testMerger.lookupBinomial(c, components[0],components[1]);
		for(Integer targetTaxon : testTaxa3){
			Set<String> synSet = testMerger.lookupSynonyms(c, targetTaxon);
			assertFalse(synSet.isEmpty());
			System.out.println("Synonyms of " + TESTTAXON3 + " are: ");
			for(String syn : synSet){
				System.out.println("   " + syn);
			}
		}
	}
	
	@Test
	public void testAttach() {
		fail("Not yet implemented");
	}


}
