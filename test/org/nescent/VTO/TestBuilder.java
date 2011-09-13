package org.nescent.VTO;


import static org.junit.Assert.assertNotNull;

import java.io.File;

import org.junit.Test;

public class TestBuilder {

	final File testFile1 = new File("/Users/peter/Projects/OBOworkspace/VTOTool/taxonOptions.xml"); 
	
	
	@Test
	public void testBuilderConstructorWithValidFile() {
		Builder b = new Builder(testFile1);
		assertNotNull(b);
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testBuilderConstructorWithNullFile(){
		new Builder(null);
	}

}
