package org.nescent.VTO.lib;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
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

	static final Logger logger = Logger.getLogger(CoLDBMerger.class.getName());

	
	/**
	 * @return true because this merger supports attaching
	 */
	@Override
	public boolean canAttach() {
		return true;
	}

	/**
	 * 
	 * @param source specifies a properties file that specifies the host,db,user,password (will default if not specified)
	 * @param target
	 * @param prefix
	 * @throws SQLException 
	 */
	@Override
	public void merge(File source, TaxonStore target, String prefix) {
		String connectionSpec;
		Connection connection;
		if (source != null)
			connectionSpec = source.getAbsolutePath();
		else
			connectionSpec = DEFAULTPROPERTIESFILESTR;
		try {
			connection = openKBFromConnections(connectionSpec);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
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
	
	final String SYNONYMQUERY = "SELECT ";
	public Set<String> lookupSynonyms(Connection c, Integer taxon){
		Set <String> result = new HashSet<String>();
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
	public void attach(File source, TaxonStore target, String parent, String cladeRoot, String prefix) {
		
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
