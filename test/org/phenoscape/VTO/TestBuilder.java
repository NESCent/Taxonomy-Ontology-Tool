package org.phenoscape.VTO;


import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestBuilder {

	final File testFile1 = new File("/Users/peter/Projects/OBOworkspace/VTOTool/taxonOptions.xml"); 
	Map<String,String> argMap2; 
	Map<String,String> argMap3; 
	Map<String,String> argMap4; 
	
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	@Before
	public void setUp() throws Exception {
		 
	}

	
	@Test
	public void testBuilder() {
		Builder b = new Builder(testFile1);
		assertNotNull(b);
	}

}
