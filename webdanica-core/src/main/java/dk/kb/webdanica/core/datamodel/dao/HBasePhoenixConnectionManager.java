package dk.kb.webdanica.core.datamodel.dao;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;

import org.apache.phoenix.jdbc.PhoenixConnection;
import org.slf4j.Logger;

import dk.kb.webdanica.core.WebdanicaSettings;
import dk.kb.webdanica.core.utils.SettingsUtilities;

import org.apache.phoenix.jdbc.PhoenixDriver;
import org.slf4j.LoggerFactory;

public class HBasePhoenixConnectionManager {
	
	private static final Logger logger = LoggerFactory.getLogger(HBasePhoenixConnectionManager.class);

	protected HBasePhoenixConnectionManager() {
	}

	protected static Object driver;
	protected static PhoenixDriver driverP;
	protected static String connectionString;

	public static synchronized void register() {
		if (driver == null) {
			try {
				driver = Class.forName( "org.apache.phoenix.jdbc.PhoenixDriver" ).newInstance();
			}
			catch (ClassNotFoundException e) {
				System.out.println( "Error: could not find jdbc driver." );
				e.printStackTrace();
			}
			catch (InstantiationException e) {
				System.out.println( "Error: could not instantiate jdbc driver." );
				e.printStackTrace();
			}
			catch (IllegalAccessException e) {
				System.out.println( "Error: could not access jdbc driver." );
				e.printStackTrace();
			}
			if (driver instanceof org.apache.phoenix.jdbc.PhoenixDriver) {
				driverP = (org.apache.phoenix.jdbc.PhoenixDriver) driver;
				logger.info("Now created instance of '" +  driverP.getClass().getName());
			}
		}
		
		String defaultConnectionString = "jdbc:phoenix:localhost:2181:/hbase";
		connectionString = SettingsUtilities.getStringSetting(WebdanicaSettings.DATABASE_CONNECTION, defaultConnectionString);
	}

	protected static Map<Thread, PhoenixConnection> threadConnectionMap =
			new TreeMap<>(Comparator.comparingLong(Thread::getId));

	public static synchronized PhoenixConnection getThreadLocalConnection() throws SQLException {
		PhoenixConnection conn = threadConnectionMap.get(Thread.currentThread());
		if (conn != null && conn.isClosed()) {
			threadConnectionMap.remove(Thread.currentThread());
			conn = null;
		}
		if (conn == null) {
			Properties connprops = new Properties();
			
			conn = (PhoenixConnection) DriverManager.getConnection(connectionString, connprops );
			threadConnectionMap.put(Thread.currentThread(), conn);
		}
		return conn;
	}

	public static void closeThreadLocalConnection() throws SQLException {
		Connection conn = threadConnectionMap.remove(Thread.currentThread());
		if (conn != null) {
			conn.close();
			conn = null;
		}
	}
	
	public static void closeAllConnections() throws SQLException {
		logger.info("Closing down all " + threadConnectionMap.size() + " connections");
		for (Connection conn: threadConnectionMap.values()) {
			if (conn != null) {
				conn.close();
			}
		}
		threadConnectionMap.clear();
		logger.info("Clearing the connectionmap");
	}

	public static void deregister() {
		// Now deregister JDBC drivers in this context's ClassLoader:
	    // Get the webapp's ClassLoader
	    ClassLoader cl = Thread.currentThread().getContextClassLoader();
	    // Loop through all drivers
	    Enumeration<Driver> drivers = DriverManager.getDrivers();
	    while (drivers.hasMoreElements()) {
	        Driver driver = drivers.nextElement();
	        
	        if (driver.getClass().getClassLoader() == cl) {
	            // This driver was registered by the webapp's ClassLoader, so deregister it:
	            try {
	                DriverManager.deregisterDriver(driver);
	            } catch (SQLException ex) {
	            	logger.warn("Exception thrown during deregistering of driver '" + driver.getClass().getName() + "': " + ex);
	            }
	        } else {
	            // driver was not registered by the webapp's ClassLoader and may be in use elsewhere
	        	logger.warn("jdbc-driver '" + driver.getClass().getName() + "' not registered by this app, so we don't touch it");       }
	    }
	    if (driverP != null) {
	    	try {
	            driverP.close();
            } catch (SQLException e) {
            	logger.warn( "Exception while trying to close the Phoenix JDBC driver", e);
            }
	    	driverP = null;
	    }
	    driver = null;
	}

}
