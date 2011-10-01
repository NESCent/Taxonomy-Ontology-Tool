package org.nescent.VTO.lib;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class TestPBDBItem {
	
	private final String testLine1 = "67649||\"Albertosaurus megagracilis\"|invalid|\"junior synonym\"|||||\"2005-09-09 18:02:17\"|Tyrannosaurus|57107||Chromista|species|09/22/2005|";
	private final String testLine2 = "38613||Tyrannosaurus|valid||||||\"2003-01-23 15:24:00\"|Tyrannosaurinae|38613||Chromista|genus|07/30/2011|";

	private final String badLine1 = "||\"Albertosaurus megagracilis\"|invalid|\"junior synonym\"|||||\"2005-09-09 18:02:17\"|Tyrannosaurus|57107||Chromista|species|09/22/2005|";
	private final String badLine2 = "foo||\"Albertosaurus megagracilis\"|invalid|\"junior synonym\"|||||\"2005-09-09 18:02:17\"|Tyrannosaurus|57107||Chromista|species|09/22/2005|";


	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testPBDBItem() {
		PBDBItem element1 = new PBDBItem(testLine1);
		assertNotNull(element1);
		assertEquals(67649, element1.getId());
		assertEquals("Albertosaurus megagracilis",element1.getName());
		PBDBItem element2 = new PBDBItem(testLine2);
		assertNotNull(element2);
	}
	
	@Test(expected=RuntimeException.class)
	public void testProcessEmptyTaxonID(){
		new PBDBItem(badLine1);
	}
	
	@Test(expected=RuntimeException.class)
	public void testProcessBadTaxonID(){
		new PBDBItem(badLine2);
	}

	

}
