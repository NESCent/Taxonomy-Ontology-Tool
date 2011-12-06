package org.nescent.VTO.lib;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
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
	static final int ORIGINAL_TAXON_NAME = 10;	
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
	static final int EXTANT = 21;
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
	
	static final String INVALIDBELONGSTO = "belongs to";
	static final String INVALIDSUBGROUP = "invalid subgroup of";
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
	final private String rankName;
	private TaxonomicStatus status;
	private String validName;     //Used for synonyms
	private String originalName;
	private boolean isExtinct;
	
	
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
	
	static final Set<TaxonomicStatus> synonymStatus = new HashSet<TaxonomicStatus>();
	static {
		synonymStatus.add(TaxonomicStatus.SUBJECTIVE_SYNONYM);
		synonymStatus.add(TaxonomicStatus.OBJECTIVE_SYNONYM);
		synonymStatus.add(TaxonomicStatus.REPLACED_BY);     //TODO This inclusion may need review
	}

	private PBDBItem(int idVal,String nameVal,String rankVal,String parentVal){
		id = idVal;
		name = nameVal;
		rankName = rankVal;
		parentName = parentVal;
	}
	
	static PBDBItem getValidInstance(String line, Map<String,Integer> columns){
		final String[] digest = pipePattern.split(line);
		if (digest.length != 30){
			throw new RuntimeException("Line had wrong number of elements: " + digest.length);
		}
		final int taxon_id = columns.get("taxon_no");
		final int taxon_name = columns.get("taxon_name");
		final int parent_name = columns.get("parent_name");
		final int taxon_rank = columns.get("taxon_rank");
		final int extant = columns.get("extant");
		int id;
		try {
			id =Integer.parseInt(digest[taxon_id]);
		}
		catch (NumberFormatException e){
			throw new RuntimeException("Misformatted taxon ID in line " + line);
		}
		if (digest[taxon_name].isEmpty()){
			throw new RuntimeException("Misformatted taxon name in line " + line);
		} 
		if (digest[parent_name].isEmpty()){
			throw new RuntimeException("Misformatted parent name in line " + line);
		}		
		if (digest[taxon_rank].isEmpty()){
			throw new RuntimeException("Misformatted rank name in line " + line);
		}
		if (!("yes".equalsIgnoreCase(digest[extant]) || "no".equalsIgnoreCase(digest[extant]) || digest[extant].isEmpty())){
			throw new RuntimeException("Misformated extinction indication in line " + line);
		}
		final PBDBItem result = new PBDBItem(id,stripQuotes(digest[taxon_name]),digest[taxon_rank],stripQuotes(digest[PARENT_NAME]));
		result.setStatus(digest,columns);
		if ("yes".equalsIgnoreCase(digest[extant]))
			result.isExtinct = false;
		else
			result.isExtinct = true;
		result.setOriginalName(digest,columns);
		return result;
	}
	
	
	public static Map<String,Integer>processHeaders(String headerrow){
		Map<String,Integer> result = new HashMap<String,Integer>();
		final String[] digest = pipePattern.split(headerrow);
		if (digest.length < 5){
			throw new RuntimeException("Header row could not be parsed.");
		}
		for(int count = 0;count< digest.length; count++){
			result.put(digest[count].toLowerCase(), count);
		}
		if (!result.containsKey("taxon_no")){
			throw new RuntimeException("File has no taxon number (taxon_no) field");
		}
		if (!result.containsKey("taxon_name")){
			throw new RuntimeException("File has no taxon name (taxon_name) field");
		}
		if (!result.containsKey("taxon_rank")){
			throw new RuntimeException("File has no taxon rank (taxon_rank) field");
		}
		return result;
	}
	
	
	private void setStatus(String[] digest, Map<String,Integer> columns){
		this.validName = null;
		if (!columns.containsKey("invalid_reason")){
			this.status = TaxonomicStatus.VALID;
			this.validName = this.name;  //more sensible than null in this case
		}
		else {
			String invalidReason = stripQuotes(digest[columns.get("invalid_reason")]);
			if (invalidReason.startsWith(INVALIDBELONGSTO)){
				this.status= TaxonomicStatus.MISPLACED;
			}
			else if (invalidReason.startsWith(INVALIDSUBGROUP)){
				this.status = TaxonomicStatus.INVALID_SUBGROUP;
				this.validName = invalidReason.substring(INVALIDSUBGROUP.length()+1);	
			}
			else if (invalidReason.startsWith(INVALIDNOMENDUBIUM)){
				this.status = TaxonomicStatus.NOMEN_DUBIUM;
			}
			else if (invalidReason.startsWith(INVALIDNOMENNUDUM)){
				this.status = TaxonomicStatus.NOMEN_NUDUM;
			}
			else if (invalidReason.startsWith(INVALIDNOMENOBLITUM)){
				this.status = TaxonomicStatus.NOMEN_OBLITUM;
			}
			else if (invalidReason.startsWith(INVALIDNOMENVANUM)){
				this.status = TaxonomicStatus.NOMEN_VANUM;
			}
			else if (invalidReason.startsWith(SYNONYMOBJECTIVE)){
				this.status = TaxonomicStatus.OBJECTIVE_SYNONYM;
				this.validName= invalidReason.substring(SYNONYMOBJECTIVE.length()+1);
			}
			else if (invalidReason.startsWith(INVALIDREPLACEDBY)){
				this.status = TaxonomicStatus.REPLACED_BY;
				this.validName = invalidReason.substring(INVALIDREPLACEDBY.length()+1);
			}
			else if (invalidReason.startsWith(SYNONYMSUBJECTIVE)){
				this.status = TaxonomicStatus.SUBJECTIVE_SYNONYM;
				this.validName = invalidReason.substring(SYNONYMSUBJECTIVE.length()+1);
			}
			else
				this.status = TaxonomicStatus.UNRECOGNIZED;
			if (this.status == TaxonomicStatus.UNRECOGNIZED){
				//logger.error("Unrecognized taxonomic status " + digest[STATUSCOLUMN] + ") or status detail (" + digest[STATUSDETAILCOLUMN] + ") in line " + line);
			}
		}
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
	
	boolean isSynonym(){
		return (synonymStatus.contains(status));
	}
	
	boolean isInvalidSubgroup(){
		return (status == TaxonomicStatus.INVALID_SUBGROUP);
	}
	
	boolean isExtinct(){
		return isExtinct;
	}
	
	void setOriginalName(String[] digest, Map<String,Integer> columns){
		originalName = null;
		String oName = stripQuotes(digest[columns.get("original_taxon_name")]);
		if (!oName.equals(validName))
			originalName = oName;
	}
	
	String getRankName(){
		return rankName;
	}
}