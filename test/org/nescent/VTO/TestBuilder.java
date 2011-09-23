package org.nescent.VTO;


import static org.junit.Assert.assertNotNull;

import java.io.File;

import org.junit.Test;

public class TestBuilder {

	final File testFile1 = new File("src/SampleProcessFiles/taxonOptions.xml"); 
	
	
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
