package com.ab.core.db;

import java.sql.Connection;
import java.sql.SQLException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ConnectionPool {
	
	// JDBC Driver Name & Database URL
    static final String JDBC_DRIVER = "com.mysql.jdbc.Driver";  
    static final String JDBC_DB_URL = "jdbc:mysql://localhost:3306/core_quiz_db?useUnicode=yes&characterEncoding=UTF-8";
    
    // JDBC Database Credentials
    static final String JDBC_USER = "rajasekhar";
    static final String JDBC_PASS = "rajasekhar";
    
    private static ConnectionPool instance;
    private MyConnectionPool myconnectionPool;
    private static final Logger logger = LogManager.getLogger(ConnectionPool.class);
    
    public static ConnectionPool getInstance() throws SQLException {
    	if (instance == null) {
    		logger.info("Initializing Connection Pool");
    		instance = new ConnectionPool();
    		instance.myconnectionPool = new MyConnectionPool(JDBC_DB_URL, JDBC_USER, JDBC_PASS);
    		instance.myconnectionPool.setCheckConnections(true);
    		instance.myconnectionPool.setMaxUseTime(2 * 60 * 1000);
    	}
    	return instance;
    }
    
    public Connection getDBConnection() throws SQLException {
    	return myconnectionPool.getConnection();
    }
    
    public Connection getConnectionNotFromPool() throws SQLException {
    	return myconnectionPool.getConnectionNotFromPool();
    }
    
    /*public GenericObjectPool<PoolableConnection> getConnectionPool() {

        return gPool;
    }
 
    // This Method Is Used To Print The Connection Pool Status
    private void printDbStatus() {
        System.out.println("Max.: " + getConnectionPool().getMaxActive() + "; Active: " + getConnectionPool().getNumActive() + "; Idle: " + getConnectionPool().getNumIdle());
    }
    
    private void setUp() {
    	if (dataSource == null) {
    		BasicDataSource ds = new BasicDataSource();
    		
    		//ds.setDriverClassName(JDBC_DRIVER);
            ds.setUrl(JDBC_DB_URL);
            ds.setUsername(JDBC_USER);
            ds.setPassword(JDBC_PASS);
 
            ds.setInitialSize(8);
            ds.setMaxOpenPreparedStatements(50);
            ds.setDefaultAutoCommit(true);
            ds.setDefaultTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
            
            dataSource = ds;
    	}
    }
    
    private static GenericObjectPool<PoolableConnection> gPool = null;
    
    public DataSource setUpPool() throws Exception{
        //Class.forName(JDBC_DRIVER);
 
        // Creates an Instance of GenericObjectPool That Holds Our Pool of Connections Object!
        
 
        // Creates a ConnectionFactory Object Which Will Be Use by the Pool to Create the Connection Object!
        ConnectionFactory cf = new DriverManagerConnectionFactory(JDBC_DB_URL, JDBC_USER, JDBC_PASS);
 
        // Creates a PoolableConnectionFactory That Will Wraps the Connection Object Created by the ConnectionFactory to Add Object Pooling Functionality!
        PooledObjectFactory<PoolableConnection> pcf = new PoolableConnectionFactory(cf, null);
        
        gPool = new GenericObjectPool<PoolableConnection>(pcf);
        gPool.setMaxTotal(4);
        gPool.setMaxWaitMillis(5 * 1000);
        return new PoolingDataSource<PoolableConnection>(gPool);
    }
 
    public void shutDown() throws SQLException {
    	dataSource.close();
    }*/
}
