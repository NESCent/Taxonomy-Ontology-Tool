package runner;

import java.util.List;

import org.apache.log4j.BasicConfigurator;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;
import org.nescent.VTO.lib.TestCoLDBMerger;
import org.nescent.VTO.lib.TestCoLMerger;
import org.nescent.VTO.lib.TestColumnMerger;
import org.nescent.VTO.lib.TestColumnReader;
import org.nescent.VTO.lib.TestIOCMerger;
import org.nescent.VTO.lib.TestITISMerger;
import org.nescent.VTO.lib.TestItem;
import org.nescent.VTO.lib.TestItemList;
import org.nescent.VTO.lib.TestNCBIMerger;
import org.nescent.VTO.lib.TestOBOMerger;
import org.nescent.VTO.lib.TestOBOStore;
import org.nescent.VTO.lib.TestOBOSynonym;
import org.nescent.VTO.lib.TestOBOTerm;
import org.nescent.VTO.lib.TestOBOUtils;
import org.nescent.VTO.lib.TestPBDBItem;
import org.nescent.VTO.lib.TestPaleoDBBulkMerger;
import org.nescent.VTO.lib.TestUnderscoreJoinedNamesMerger;


public class RunAll {

	private int runTotal = 0;
	private int failureTotal = 0;

	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		BasicConfigurator.configure();

		
		//testOneClass(TestCoLDBMerger.class);
		testOneClass(TestCoLMerger.class);
		testOneClass(TestColumnMerger.class);
		testOneClass(TestColumnReader.class);
		testOneClass(TestIOCMerger.class);
		testOneClass(TestItem.class);
		testOneClass(TestItemList.class);
		testOneClass(TestITISMerger.class);
		testOneClass(TestNCBIMerger.class);
		testOneClass(TestOBOMerger.class);
		testOneClass(TestOBOStore.class);
		testOneClass(TestOBOSynonym.class);
		testOneClass(TestOBOTerm.class);
		testOneClass(TestOBOUtils.class);
		testOneClass(TestUnderscoreJoinedNamesMerger.class);
		testOneClass(TestPBDBItem.class);
		testOneClass(TestPaleoDBBulkMerger.class);
	}

	private static void testOneClass(Class<?> c){
		Result r;
		List<Failure> fList;
		
		r = org.junit.runner.JUnitCore.runClasses(c);
		System.out.println("For test class: " + c.getCanonicalName());
		System.out.println("Run count: " + r.getRunCount());
		System.out.println("Failure count: " + r.getFailureCount());
		fList = r.getFailures();
		for (Failure f : fList){
			System.err.println(f.getTestHeader());
			System.err.print("  ");
			System.err.println(f.getException());
			System.err.println("");
		}

	}

	
}
