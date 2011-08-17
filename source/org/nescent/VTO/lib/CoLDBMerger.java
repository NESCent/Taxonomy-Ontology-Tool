package org.nescent.VTO.lib;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

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
	 * @param source
	 * @param target
	 * @param prefix
	 */
	@Override
	public void merge(File source, TaxonStore target, String prefix) {
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
