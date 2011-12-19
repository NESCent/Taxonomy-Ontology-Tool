/**
 * VTOTool - a tool for merging and building ontologies from multiple taxonomic sources
 * 
 * Peter Midford
 * 
 */
package org.nescent.VTO;

import java.io.File;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;


public class VTOTool {
	
	private static final String usageStr = "usage: VTOTool [spec-file] [--help]";
	private static final String helpStr = "VTOTool builds a new VTO (combined vertebrate taxonomy ontology)\n"+
	                                      "args: spec-file xml file specifying taxonomy, attachments and merge files\n";


	static Logger logger = Logger.getLogger(VTOTool.class.getName());

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		BasicConfigurator.configure();
		File processFile = processArgs(args);
		if (processFile != null){
			Builder b = new Builder(processFile);
			try {
				b.build();
			} catch (Exception e) {
				logger.fatal("Error in processing; message is " + e.getLocalizedMessage());
				e.printStackTrace();
			}
		}
	}

	static File processArgs(String[] args) {	
		File result = null;
		if (args.length < 1){
			System.err.println(usageStr);
			return null;
		}
		if ("--usage".equals(args[0])){
			System.err.println(usageStr);
			return null;
		}
		if ("--help".equals(args[0])){
			System.err.println(helpStr);
			return null;
		}
		result = new File(args[0]);
		return result;
	}

}
