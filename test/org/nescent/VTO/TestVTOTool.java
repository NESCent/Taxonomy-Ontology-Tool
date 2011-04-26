package org.nescent.VTO;


import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestVTOTool {

	final String[] testArgs1 = {};
	final String[] testArgs2 = {"test"};
	final String[] testArgs3 = {"--usage"};
	final String[] testArgs4 = {"--help"};
	final String[] testArgs5 = {"/Users/peter/Projects/OBOworkspace/VTOTool/taxonOptions.xml"};
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	@Before
	public void setUp() throws Exception {
	}
	
	@Test
	public void testprocessArgs(){
		File fileResult;
		fileResult = VTOTool.processArgs(testArgs1);
		assertNull(fileResult);
		fileResult = VTOTool.processArgs(testArgs2);
		assertFalse(fileResult==null);
		fileResult = VTOTool.processArgs(testArgs3);
		assertNull(fileResult);
		fileResult=VTOTool.processArgs(testArgs4);
		assertNull(fileResult);
		fileResult=VTOTool.processArgs(testArgs5);
		assertFalse(fileResult==null);
		assertTrue(fileResult.exists());
	}
}
