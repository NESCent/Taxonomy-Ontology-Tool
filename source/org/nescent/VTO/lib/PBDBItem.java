package org.nescent.VTO.lib;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

class PBDBItem{
	
	static final Pattern pipePattern = Pattern.compile("\\|");
	
	//columns for itis format
//	static final int IDCOLUMN = 0;
//	static final int NAMECOLUMN = 2;
//	static final int STATUSCOLUMN = 3;
//	static final int STATUSDETAILCOLUMN = 4;
//	static final int ENTRYDATECOLUMN = 9;
//	static final int PARENTNAMECOLUMN = 10;
//	static final int KINGDOMNAMECOLUMN = 13;
//	static final int RANKNAMECOLUMN = 14;
//	static final int MODDATECOLUMN = 15;
//	
	//static final Pattern commaPattern = Pattern.compile("\\,");
	
	//columns for full fields format for valid taxa
	static final int authorizer	= 0;
	static final int enterer = 1;
	static final int modifier = 2;	
	static final int reference_no = 3;	
	static final int TAXON_NO = 4;
	static final int TAXON_NAME	= 5;
	static final int spelling_reason = 6;
	static final int common_name = 7;	
	static final int TAXON_RANK	= 8;
	static final int original_taxon_no = 9;	
	static final int original_taxon_name = 10;	
	static final int original_taxon_rank = 11;
	static final int author1init = 12;	
	static final int author1last = 13;
	static final int author2init = 14;
	static final int author2last = 15;
	static final int otherauthors = 16;	
	static final int pubyr = 17;
	static final int pages = 18;
	static final int figures = 19;	
	static final int PARENT_NAME = 20;
	static final int extant = 21;
	static final int preservation = 22;	
	static final int type_taxon = 23;
	static final int type_specimen = 24;	
	static final int type_body_part = 25;
	static final int part_details = 26;
	static final int comments = 27;	
	static final int created = 28;	
	static final int modified = 29;
	
	//columns for full fields format for invalid taxa
	static final int IV_common_name = 6;
	static final int IV_taxon_rank = 7;
	static final int INVALID_REASON	= 8;
	static final int IV_original_taxon_no = 9;	
	static final int IV_original_taxon_name = 10;	
	static final int IV_original_taxon_rank = 11;	
	static final int IV_author1init = 12;	
	static final int IV_author1last = 13;	
	static final int IV_author2init = 14;	
	static final int IV_author2last = 15;	
	static final int IV_otherauthors = 16;	
	static final int IV_pubyr = 17;	
	static final int IV_pages = 18;	
	static final int IV_figures = 19;	
	static final int IV_PARENT_NAME = 20;	
	static final int IV_extant = 21;	
	static final int IV_preservation = 22;	
	static final int IV_type_taxon = 23;	
	static final int IV_type_specimen = 24;	
	static final int IV_type_body_part = 25;	
	static final int IV_part_details = 26;	
	static final int IV_comments = 27;	
	static final int IV_created = 28;	
	static final int IV_modified = 29;
	
	static final String INVALIDBELONGSTO = "belongs to";
	static final String INVALIDSUBGROUP = "invalid subgroup";
	static final String INVALIDNOMENDUBIUM = "nomen dubium";
	static final String INVALIDNOMENNUDUM = "nomen nudum";
	static final String INVALIDNOMENOBLITUM = "nomen oblitum";
	static final String INVALIDNOMENVANUM = "nomen vanum";
	static final String SYNONYMOBJECTIVE = "objective synonym of";
	static final String INVALIDREPLACEDBY = "replaced by";
	static final String SYNONYMSUBJECTIVE = "subjective synonym of";
	
	final private String name;
	final private int id;
	final private String parentName;
	private String rankName;
	final private TaxonomicStatus status;
	private String validName;     //Used for synonyms
	
	
	static final Logger logger = Logger.getLogger(PBDBItem.class.getName());
	
	enum TaxonomicStatus{
		VALID,
		INVALID_SUBGROUP,
		MISPLACED,
		NOMEN_DUBIUM,
		NOMEN_NUDUM,
		NOMEN_OBLITUM,
		NOMEN_VANUM,
		OBJECTIVE_SYNONYM,
		REPLACED_BY,
		SUBJECTIVE_SYNONYM,
		UNRECOGNIZED
	}

	private PBDBItem(int idVal,String nameVal,TaxonomicStatus statusVal,String parentVal){
		id = idVal;
		name = nameVal;
		status = statusVal;
		parentName = parentVal;
	}
	
	static PBDBItem getValidInstance(String line){
		final String[] digest = pipePattern.split(line);
		if (digest.length != 30){
			throw new RuntimeException("Line had wrong number of elements: " + digest.length);
		}
		int id;
		try {
			id =Integer.parseInt(digest[TAXON_NO]);
		}
		catch (NumberFormatException e){
			throw new RuntimeException("Misformatted taxon ID in line " + line);
		}
		if (digest[TAXON_NAME].isEmpty()){
			throw new RuntimeException("Misformatted taxon name in line " + line);
		} 
		if (digest[PARENT_NAME].isEmpty()){
			throw new RuntimeException("Misformatted parent name in line " + line);
		}		
		if (digest[TAXON_RANK].isEmpty()){
			throw new RuntimeException("Misformatted rank name in line " + line);
		}
		final PBDBItem result = new PBDBItem(id,stripQuotes(digest[TAXON_NAME]),TaxonomicStatus.VALID,stripQuotes(digest[PARENT_NAME]));
		result.rankName = digest[TAXON_RANK];
		return result;
	}
	
	
	static PBDBItem getInvalidInstance(String line){
		final String[] digest = pipePattern.split(line);
		if (digest.length != 30){
			throw new RuntimeException("Line had wrong number of elements: " + digest.length);
		}
		int id;
		try {
			id =Integer.parseInt(digest[TAXON_NO]);
		}
		catch (NumberFormatException e){
			throw new RuntimeException("Misformatted taxon ID in line " + line);
		}
		if (digest[TAXON_NAME].isEmpty()){
			throw new RuntimeException("Misformatted taxon name in line " + line);
		} 
		if (digest[TAXON_RANK].isEmpty()){
			throw new RuntimeException("Misformatted rank name in line " + line);
		}
		String invalidReason = stripQuotes(digest[INVALID_REASON]);
		TaxonomicStatus tstatus;
		String vname = null;
		if (invalidReason.startsWith(INVALIDBELONGSTO)){
			tstatus = TaxonomicStatus.MISPLACED;
		}
		else if (invalidReason.startsWith(INVALIDSUBGROUP)){
			tstatus = TaxonomicStatus.INVALID_SUBGROUP;
		}
		else if (invalidReason.startsWith(INVALIDNOMENDUBIUM)){
			tstatus = TaxonomicStatus.NOMEN_DUBIUM;
		}
		else if (invalidReason.startsWith(INVALIDNOMENNUDUM)){
			tstatus = TaxonomicStatus.NOMEN_NUDUM;
		}
		else if (invalidReason.startsWith(INVALIDNOMENOBLITUM)){
			tstatus = TaxonomicStatus.NOMEN_OBLITUM;
		}
		else if (invalidReason.startsWith(INVALIDNOMENVANUM)){
			tstatus = TaxonomicStatus.NOMEN_VANUM;
		}
		else if (invalidReason.startsWith(SYNONYMOBJECTIVE)){
			tstatus = TaxonomicStatus.OBJECTIVE_SYNONYM;
			vname = invalidReason.substring(SYNONYMOBJECTIVE.length()+1);
		}
		else if (invalidReason.startsWith(INVALIDREPLACEDBY)){
			tstatus = TaxonomicStatus.REPLACED_BY;
		}
		else if (invalidReason.startsWith(SYNONYMSUBJECTIVE)){
			tstatus = TaxonomicStatus.SUBJECTIVE_SYNONYM;
			vname = invalidReason.substring(SYNONYMSUBJECTIVE.length()+1);
		}
		else
			tstatus = TaxonomicStatus.UNRECOGNIZED;
		if (tstatus == TaxonomicStatus.UNRECOGNIZED){
			//logger.error("Unrecognized taxonomic status " + digest[STATUSCOLUMN] + ") or status detail (" + digest[STATUSDETAILCOLUMN] + ") in line " + line);
		}
		final PBDBItem result = new PBDBItem(id,stripQuotes(digest[TAXON_NAME]),tstatus,"");
		result.rankName = digest[TAXON_RANK];
		result.validName = vname;
		return result;
	}
	
	
	private static String stripQuotes(String s){
		if (s.charAt(0) == '"' && s.charAt(s.length()-1) == '"')
			return s.substring(1,s.length()-1);
		else
			return s;
	}
	
	
	String getName(){
		return name;
	}
	
	int getId(){
		return id;
	}
	
	String getParentName(){
		return parentName;
	}
	
	String getValidName(){
		return validName;
	}
	
	
	boolean isValid(){
		return (status == TaxonomicStatus.VALID);
	}
	
}