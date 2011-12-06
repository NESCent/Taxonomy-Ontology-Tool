package runner;

import java.util.List;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;
import org.nescent.VTO.lib.TestPBDBItem;


public class RunAll {

	private int runTotal = 0;
	private int failureTotal = 0;

	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Result r;
		List<Failure> fList;
		
		r = org.junit.runner.JUnitCore.runClasses(TestPBDBItem.class);
		System.err.println("Run count: " + r.getRunCount());
		System.err.println("Failure count: " + r.getFailureCount());
		fList = r.getFailures();
		for (Failure f : fList){
			System.err.println(f.getTestHeader());
			System.err.print("  ");
			System.err.println(f.getException());
			System.err.println("");
		}

		
		// TODO Auto-generated method stub

	}
	
}
