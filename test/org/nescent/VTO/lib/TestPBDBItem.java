package org.nescent.VTO.lib;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class TestPBDBItem {
	
	private final String validLine1 = "\"M. Carrano\"|\"M. Carrano\"|\"M. Carrano\"|9259|38613|Tyrannosaurus|\"original spelling\"|\"tyrant lizard\"|genus|38613|Tyrannosaurus|genus|\"H. F.\"|Osborn||||1905|259|1|Tyrannosaurinae|no|\"not a trace\"|\"Tyrannosaurus rex\"|||||\"2003-01-23 15:24:00\"|\"2011-07-30 01:43:18\"";
	private final String validLine2 = "\"M. Carrano\"|\"M. Carrano\"|\"M. Carrano\"|9259|54833|\"Tyrannosaurus rex\"|\"original spelling\"|\"tyrant lizard king\"|species|54833|\"Tyrannosaurus rex\"|species|\"H. F.\"|Osborn||||1905|262|1|Tyrannosaurus|no|\"body (3D)\"||\"CM 9380 (= AMNH 973)\"|\"partial skeleton\"|||\"2003-09-22 07:45:42\"|\"2010-03-18 14:32:56\"";
	private final String invalidLine1 = "\"M. Carrano\"|\"M. Carrano\"|\"M. Carrano\"|10350|67649|\"Albertosaurus megagracilis\"||species|\"subjective synonym of Tyrannosaurus rex\"|67649|\"Albertosaurus megagracilis\"|species|\"G. S.\"|Paul||||1988|333-334||\"Tyrannosaurus rex\"|no|||\"LACM 28345\"||||\"2005-09-09 18:02:17\"|\"2005-09-22 23:35:31\"";
	private final String invalidLine2 = "\"M. Carrano\"|\"M. Carrano\"||14898|68319|\"Tyrannosaurus turpanensis\"||species|\"nomen dubium belonging to Tyrannosaurus\"|68319|\"Tyrannosaurus turpanensis\"|species|R.|Zhai|J.|Zheng|\"Y. Tong\"|1978|||Tyrannosaurus|no|||||||\"2005-10-28 08:35:04\"|\"2005-10-29 01:35:04\"";
	
	private final String badLine1  = "\"M. Carrano\"|\"M. Carrano\"|\"M. Carrano\"|9259||Tyrannosaurus|\"original spelling\"|\"tyrant lizard\"|genus|38613|Tyrannosaurus|genus|\"H. F.\"|Osborn||||1905|259|1|Tyrannosaurinae|no|\"not a trace\"|\"Tyrannosaurus rex\"|||||\"2003-01-23 15:24:00\"|\"2011-07-30 01:43:18\"";
	private final String badLine2 = "\"M. Carrano\"|\"M. Carrano\"|\"M. Carrano\"|9259|foo|\"Tyrannosaurus rex\"|\"original spelling\"|\"tyrant lizard king\"|species|54833|\"Tyrannosaurus rex\"|species|\"H. F.\"|Osborn||||1905|262|1|Tyrannosaurus|no|\"body (3D)\"||\"CM 9380 (= AMNH 973)\"|\"partial skeleton\"|||\"2003-09-22 07:45:42\"|\"2010-03-18 14:32:56\"";
    private final String badLine3 ="\"M. Carrano\"|\"M. Carrano\"|\"M. Carrano\"|10350|67649|\"Albertosaurus megagracilis\"||species|\"subjective synonym of\"|67649|\"Albertosaurus megagracilis\"|species|\"G. S.\"|Paul||||1988|333-334||\"Tyrannosaurus rex\"|no|||\"LACM 28345\"||||\"2005-09-09 18:02:17\"|\"2005-09-22 23:35:31\"";


	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testPBDBItem() {
		PBDBItem element1 = PBDBItem.getValidInstance(validLine1);
		assertNotNull(element1);
		assertEquals(38613, element1.getId());
		assertEquals("Tyrannosaurus",element1.getName());
		assertTrue(element1.isValid());
		PBDBItem element2 = PBDBItem.getValidInstance(validLine2);
		assertNotNull(element2);
		assertEquals(54833, element2.getId());
		assertEquals("Tyrannosaurus rex",element2.getName());
		PBDBItem syn1 = PBDBItem.getInvalidInstance(invalidLine1);
		assertNotNull(syn1);
		assertEquals("Albertosaurus megagracilis",syn1.getName());
		assertFalse(syn1.isValid());
		assertEquals("Tyrannosaurus rex",syn1.getValidName());
		PBDBItem syn2 = PBDBItem.getInvalidInstance(invalidLine2);
		assertNotNull(syn2);
		assertEquals("Tyrannosaurus turpanensis",syn2.getName());
	}
	
	@Test(expected=RuntimeException.class)
	public void testProcessEmptyTaxonID(){
		 PBDBItem.getValidInstance(badLine1);
	}
	
	@Test(expected=RuntimeException.class)
	public void testProcessBadTaxonID(){
		PBDBItem.getValidInstance(badLine2);
	}

	@Test(expected=StringIndexOutOfBoundsException.class)
	public void testProcessBadSynonymName(){
		PBDBItem.getInvalidInstance(badLine3);
	}

	

}
