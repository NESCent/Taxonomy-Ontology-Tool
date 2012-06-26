package org.nescent.VTO.lib;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import org.apache.log4j.Logger;


/**
 * This class merges names from the Catalogue of Life, using JDBC to connect directly to a (presumibly local) MySQL
 * installation of the CoL database.
 * 
 * This version supports attaching.
 * 
 * @author pmidford@nescent.org
 *
 */
public class CoLDBMerger implements Merger {

	static final String DEFAULTPROPERTIESFILESTR = "Connection.properties";

	private File source;
	private TaxonStore target;
	
	private SynonymSource preserveSynonyms;

	static final Logger logger = Logger.getLogger(CoLDBMerger.class.getName());

	/* Metadata about this merger */
	
	/**
	 * @return true because this merger supports attaching
	 */
	@Override
	public boolean canAttach() {
		return true;
	}
	
	/**
	 * @return false because CoL id's aren't permanent
	 */
	@Override
	public boolean canPreserveID(){
		return false;
	}


	@Override 
	public void setSource(File sourceFile){
		source = sourceFile;
	}

	@Override
	public void setTarget(TaxonStore targetStore){
		target = targetStore;
	}
	
	@Override
	public void setPreserveID(boolean v){
		throw new RuntimeException("This merger can't preserve IDs because CoL ids are not stable");
	}
	
	@Override
	public void setPreserveSynonyms(SynonymSource s){
		preserveSynonyms = s;
	}
	
	/**
	 * 
	 * @param source specifies a properties file that specifies the host,db,user,password (will default if not specified)
	 * @param target
	 * @param prefix
	 * @throws SQLException 
	 */
	@Override
	public void merge(String prefix) {
		String connectionSpec;
		Connection connection = null;
		if (source != null)
			connectionSpec = source.getAbsolutePath();
		else
			connectionSpec = DEFAULTPROPERTIESFILESTR;
		try {
			connection = openKBFromConnections(connectionSpec);
			final Collection<Term> terms = target.getTerms();
			for (Term term : terms){
				String[] nameSplit = term.getLabel().split(" ");
				if (nameSplit.length == 2) {   //any need to lookup synonyms for trinomials?
					logger.info("Processing: " + term.getLabel());
					Set<Integer> colTaxonSet = lookupBinomial(connection,nameSplit[0],nameSplit[1]);
					for(Integer colTaxon : colTaxonSet){
						Set<String> synSet = lookupSynonyms(connection,colTaxon);
						for(String syn : synSet){
							SynonymI newSyn = target.makeSynonym(syn);
							term.addSynonym(newSyn);
						}
					}
				}
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}	
		finally {
			try {
				if (connection != null)
					connection.close();
			} catch (SQLException e) {
				logger.error("SQL error while trying to close the connection to CoL database");
				e.printStackTrace();
			}
		}
		
	}

	final String MONOMIALQUERY = "SELECT t.id FROM taxon AS t " +
	                             "JOIN taxon_name_element AS tne ON (tne.taxon_id = t.id)" + 
			                     "JOIN scientific_name_element AS sne ON (tne.scientific_name_element_id = sne.id) " +
	                             "WHERE sne.name_element = ?";
	public Set<Integer> lookupMonomial(Connection c, String name) throws SQLException{
		final Set<Integer> result = new HashSet<Integer>();
		final PreparedStatement monomialStatement = c.prepareStatement(MONOMIALQUERY);
		monomialStatement.setString(1, name);
		ResultSet s = monomialStatement.executeQuery();
		while(s.next()){
			result.add(s.getInt(1));
		}
		return result;
	}
	
	final String BINOMIALQUERY = "SELECT t.id FROM taxon AS t "+
								 "JOIN taxon_name_element AS tne ON (tne.taxon_id = t.id) " + 
								 "JOIN scientific_name_element AS sne ON (tne.scientific_name_element_id = sne.id) " +
	                             "WHERE sne.name_element = ? AND tne.parent_id = ? ";
	public Set<Integer> lookupBinomial(Connection c, String genus, String species) throws SQLException{
		final Set<Integer> result = new HashSet<Integer>();
		final Set<Integer> genusSet = lookupMonomial(c, genus);
		final PreparedStatement binomialStatement = c.prepareStatement(BINOMIALQUERY);
		binomialStatement.setString(1, species);
		for (Integer genusID : genusSet){
			binomialStatement.setInt(2, genusID);
			ResultSet s = binomialStatement.executeQuery();
			while(s.next()){
				result.add(s.getInt(1));
			}
		}
		return result;
	}
	
	static final String SYNONYMIDQUERYSTRING = "select s.id from synonym as s where s.taxon_id = ?";

	static final private String SYNONYMBINOMIALQUERY = "SELECT gne.name_element,sne.name_element,status.name_status FROM synonym AS syn " +
	"JOIN synonym_name_element AS genus_name_element ON (genus_name_element.synonym_id = syn.id AND genus_name_element.taxonomic_rank_id=20) " +
	"JOIN scientific_name_element AS gne ON (genus_name_element.scientific_name_element_id = gne.id) " +
	"join synonym_name_element as species_name_element on (species_name_element.synonym_id = syn.id AND species_name_element.taxonomic_rank_id=83) " +
	"join scientific_name_element as sne on (species_name_element.scientific_name_element_id = sne.id) " +
	"join scientific_name_status as status on (status.id = syn.scientific_name_status_id) " +
	"WHERE syn.id=?" ;
	

	public Set<String> lookupSynonyms(Connection c, Integer taxon) throws SQLException{
		Set <String> result = new HashSet<String>();
		PreparedStatement synonymIDStatement = c.prepareStatement(SYNONYMIDQUERYSTRING);
		PreparedStatement binomialStatement = c.prepareStatement(SYNONYMBINOMIALQUERY);
		synonymIDStatement.setInt(1, taxon);
		ResultSet synonymSet = synonymIDStatement.executeQuery();
		while(synonymSet.next()){
			int synonymID = synonymSet.getInt(1);
			binomialStatement.setInt(1,synonymID);
			ResultSet binomialSet = binomialStatement.executeQuery();
			String genusString = "";
			String speciesString = "";
			String synStatus = "";  //TODO use this for filtering
			if (binomialSet.next()){
				genusString = binomialSet.getString(1);
				speciesString = binomialSet.getString(2);
				synStatus = binomialSet.getString(3);
			}
			StringBuilder synBuilder = new StringBuilder(30);
			synBuilder.append(genusString);
			synBuilder.append(' ');
			synBuilder.append(speciesString);
			result.add(synBuilder.toString());
		}
		return result;
	}
	
	/**
	 * 
	 * @param source
	 * @param target
	 * @param parent
	 * @param cladeRoot
	 * @param prefix
	 */
	@Override
	public void attach(String parent, String cladeRoot, String prefix) {
		String connectionSpec;
		if (source != null)
			connectionSpec = source.getAbsolutePath();
		else
			connectionSpec = DEFAULTPROPERTIESFILESTR;
		Term parentTerm = null;
		if (!"".equals(parent)){
			parentTerm = target.getTermbyName(parent);
			if (parentTerm == null){   //parent is unknown
				if (!target.isEmpty()){
					logger.error("Can not attach CoL Database specified by " + connectionSpec + " specified parent: " + parent + " is unknown to " + target);
					return;
				}
				else { // attachment will be added first to provide a root for an otherwise empty target
					parentTerm = target.addTerm(parent,prefix);
					logger.info("Assigning " + parent + " as root");
				}
			}
		}
		Connection connection = null;
		try {
			connection = openKBFromConnections(connectionSpec);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}	
		finally {
			try {
				if (connection != null)
					connection.close();
			} catch (SQLException e) {
				logger.error("SQL error while trying to close the connection to CoL database");
				e.printStackTrace();
			}
		}

	}

	/**
	 * This opens a JDBC connection to a MySQL database that (presumably) contains a CoL dumb.
	 * @param connectionsSpec specifies a properties file with host, db, user, and pw fields to open the db.
	 * @return an sql connection to the database specified by the properties file
	 * @throws SQLException
	 */
	public Connection openKBFromConnections(String connectionsSpec) throws SQLException {
		final Properties properties = new Properties();
		try {
			properties.load(this.getClass().getResourceAsStream(connectionsSpec));
		} catch (Exception e1) {
			throw new RuntimeException("Failed to open connection properties file; path = " + connectionsSpec);
		} 
		try {
			Class.forName("com.mysql.jdbc.Driver");
		} catch(ClassNotFoundException e){
			logger.error("Couldn't load MySQL Driver");
			e.printStackTrace();
		}
		final String host = properties.getProperty("host");
		final String db = properties.getProperty("db");
		final String user = properties.getProperty("user");
		final String password = properties.getProperty("pw");
		return DriverManager.getConnection(String.format("jdbc:mysql://%s/%s",host,db),user,password);

	}

}
