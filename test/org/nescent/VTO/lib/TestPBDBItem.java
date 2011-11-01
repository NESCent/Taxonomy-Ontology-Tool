package org.nescent.VTO.lib;

import static org.junit.Assert.*;

import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class TestPBDBItem {
	
	private final String validHeaders = "authorizer|enterer|modifier|reference_no|taxon_no|taxon_name|spelling_reason|common_name|taxon_rank|original_taxon_no|original_taxon_name|original_taxon_rank|author1init|author1last|author2init|author2last|otherauthors|pubyr|pages|figures|parent_name|extant|preservation|type_taxon|type_specimen|type_body_part|part_details|comments|created|modified";
	private final String invalidHeaders = "authorizer|enterer|modifier|reference_no|taxon_no|taxon_name|common_name|taxon_rank|invalid_reason|original_taxon_no|original_taxon_name|original_taxon_rank|author1init|author1last|author2init|author2last|otherauthors|pubyr|pages|figures|parent_name|extant|preservation|type_taxon|type_specimen|type_body_part|part_details|comments|created|modified";
	
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
	public void testProcessHeaders(){
		Map<String,Integer>validMap = PBDBItem.processHeaders(validHeaders);
		assertNotNull(validMap);
		assertEquals(30,validMap.size());
		Map<String,Integer>invalidMap = PBDBItem.processHeaders(invalidHeaders);
		assertNotNull(invalidMap);
		assertEquals(30,validMap.size());
	}
	
	@Test
	public void testPBDBItem() {
		Map<String,Integer>validMap = PBDBItem.processHeaders(validHeaders);		
		PBDBItem element1 = PBDBItem.getValidInstance(validLine1,validMap);
		assertNotNull(element1);
		assertEquals(38613, element1.getId());
		assertEquals("Tyrannosaurus",element1.getName());
		assertTrue(element1.isValid());
		assertTrue(element1.isExtinct());
		PBDBItem element2 = PBDBItem.getValidInstance(validLine2,validMap);
		assertNotNull(element2);
		assertEquals(54833, element2.getId());
		assertEquals("Tyrannosaurus rex",element2.getName());
		assertTrue(element2.isExtinct());
		Map<String,Integer>invalidMap = PBDBItem.processHeaders(invalidHeaders);
		PBDBItem syn1 = PBDBItem.getValidInstance(invalidLine1,invalidMap);
		assertNotNull(syn1);
		assertEquals("Albertosaurus megagracilis",syn1.getName());
		assertFalse(syn1.isValid());
		assertTrue(syn1.isSynonym());
		assertEquals("Tyrannosaurus rex",syn1.getValidName());
		PBDBItem syn2 = PBDBItem.getValidInstance(invalidLine2,invalidMap);
		assertNotNull(syn2);
		assertFalse(syn2.isSynonym());
		assertEquals("Tyrannosaurus turpanensis",syn2.getName());
	}
	
	@Test(expected=RuntimeException.class)
	public void testProcessEmptyTaxonID(){
		Map<String,Integer>validMap = PBDBItem.processHeaders(validHeaders);		
		PBDBItem.getValidInstance(badLine1,validMap);
	}
	
	@Test(expected=RuntimeException.class)
	public void testProcessBadTaxonID(){
		Map<String,Integer>validMap = PBDBItem.processHeaders(validHeaders);		
		PBDBItem.getValidInstance(badLine2,validMap);
	}

	@Test(expected=StringIndexOutOfBoundsException.class)
	public void testProcessBadSynonymName(){
		Map<String,Integer>invalidMap = PBDBItem.processHeaders(invalidHeaders);		
		PBDBItem.getValidInstance(badLine3,invalidMap);
	}

	

}
