package com.ab.core.db;

import java.sql.Array;
import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.NClob;
import java.sql.PreparedStatement;
import java.sql.SQLClientInfoException;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.SQLXML;
import java.sql.Savepoint;
import java.sql.Statement;
import java.sql.Struct;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executor;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class MyConnectionPool {

	protected List<PoolConnection> _connections;
	protected String _url;
	protected String _user;
	protected String _password;

	/**
	 * Indicates whether the ConnectionPool should check the status of connections
	 * (closed, has warnings) before they are returned.
	 **/

	protected boolean _checkConnections = true;

	protected long _cleaningInterval = 30 * 1000; // 30 seconds
	protected long _maxIdleTime = 30 * 1000; // 30 seconds

	protected long _maxUseTime = -1; // disabled by default

	protected boolean _draining = false;

	protected PoolCleaner _cleaner;
	
	private static final Logger logger = LogManager.getLogger(MyConnectionPool.class);

	/*--------------------------------------------------+
	| Constructors                                      |
	+--------------------------------------------------*/

	public MyConnectionPool(String url, String user, String password) {
		_url = url;
		_user = user;
		_password = password;

		_connections = new ArrayList<PoolConnection>();
	}
	
	/**
	 * Sets the flag that determines whether the the status of connections (closed,
	 * has warnings) is checked before they are returned by getConnection(). With
	 * some jdbc-drivers, the extra checks can have a large performance penalty.
	 * Default value is 'true'.
	 **/
	public void setCheckConnections(boolean checkConnections) {
		_checkConnections = checkConnections;
	}

	/**
	 * Sets the interval for the pool cleaner to come into action. The pool cleaner
	 * checks the connection pool every so many milliseconds for connections that
	 * should be removed. The default interval is 30 seconds.
	 * 
	 * @param cleaningInterval The interval in milliseconds.
	 **/
	public void setCleaningInterval(long cleaningInterval) {
		_cleaningInterval = cleaningInterval;
	}

	/**
	 * Sets the maximum time that a connection is allowed to be idle. A connection
	 * that has been idle for a longer time will be removed by the pool cleaner the
	 * next time it check the pool. The default value is 30 seconds.
	 *
	 * @param maxIdleTime The maximum idle time in milliseconds.
	 **/
	public void setMaxIdleTime(long maxIdleTime) {
		_maxIdleTime = maxIdleTime;
	}

	/**
	 * Sets the maximum time that a connection is allowed to be used. A connection
	 * that has been used for a longer time will be forced to close itself, even if
	 * it is still in use. Normally, this time should only be reached in case an
	 * program "forgets" to close a connection. The maximum time is switched of by
	 * default.
	 *
	 * @param maxUseTime The maximum time a connection can be used in milliseconds,
	 *                   or a negative value if there is no maximum.
	 **/
	public void setMaxUseTime(long maxUseTime) {
		_maxUseTime = maxUseTime;
	}
	
	public Connection getConnectionNotFromPool() throws SQLException {
		Connection con = DriverManager.getConnection(_url, _user, _password);
		con.setAutoCommit(true);
		con.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
		return con;
	}

	/*--------------------------------------------------+
	| Methods                                           |
	+--------------------------------------------------*/
	public Connection getConnection() throws SQLException {

		if (_draining) {
			throw new SQLException("ConnectionPool was drained.");
		}

		// Try reusing an existing Connection
		synchronized (_connections) {
			if (_connections.size() > 50) {
				throw new SQLException("Max Connection Count Reached..");
			}
			PoolConnection pc = null;

			for (int i = 0; i < _connections.size(); i++) {
				pc = _connections.get(i);

				if (pc.lease()) {
					// PoolConnection is available

					if (!_checkConnections) {
						return pc;
					} else {
						// Check the status of the connection
						boolean isHealthy = true;

						try {
							if (pc.isClosed() && pc.getWarnings() != null) {
								// If something happend to the connection, we
								// don't want to use it anymore.
								isHealthy = false;
							}
						} catch (SQLException sqle) {
							// If we can't even ask for that information, we
							// certainly don't want to use it anymore.
							isHealthy = false;
						}

						if (isHealthy) {
							pc.setAutoCommit(true);
							return pc;
						} else {
							try {
								pc.expire();
							} catch (SQLException sqle) {
								// ignore
							}
							_connections.remove(i);
						}
					}
				}
			}
		}

		// Create a new Connection
		Connection con = DriverManager.getConnection(_url, _user, _password);
		con.setAutoCommit(true);
		con.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
		PoolConnection pc = new PoolConnection(con);
		pc.lease();

		// Add it to the pool
		synchronized (_connections) {
			_connections.add(pc);

			if (_cleaner == null) {
				// Put a new PoolCleaner to work
				_cleaner = new PoolCleaner(_cleaningInterval);
				_cleaner.start();
			}
		}
		return pc;
	}

	public void removeExpired() {

		PoolConnection pc;

		long maxIdleDeadline = System.currentTimeMillis() - _maxIdleTime;
		long maxUseDeadline = System.currentTimeMillis() - _maxUseTime;

		synchronized (_connections) {
			// Check all connections
			for (int i = _connections.size() - 1; i >= 0; i--) {
				pc = _connections.get(i);

				//logger.info("Rajasekhar {} and {} and {}", pc.hashCode(), pc.inUse(), (pc.getTimeClosed() < maxIdleDeadline));
				if (!pc.inUse() && pc.getTimeClosed() < maxIdleDeadline) {
					// Connection has been idle too long, close it.
					//logger.info("Rajasekhar closing here {}", pc.hashCode());
					_connections.remove(i);
					try {
						//logger.info("Rajasekhar expire called {}", pc.hashCode());
						pc.expire();
					} catch (SQLException ignore) {
						logger.info("Rajasekhar SQLException", ignore);
					}
				} else if (_maxUseTime >= 0 && // don't check if disabled
						pc.inUse() && pc.getTimeOpened() < maxUseDeadline) {
					// Connection has been used too long, close it.

					// Print the location where the connetion was acquired
					// as it probably forgot to close the connection (which
					// is a bug).
					System.err.println("Warning: forced closing of a connection that has been in use too long." + pc.hashCode());
					System.err.println("Connection was acquired in:");
					pc.printStackTrace();
					System.err.println();

					_connections.remove(i);
					try {
						pc.expire();
					} catch (SQLException ignore) {
						logger.error("ignore error ", ignore);
					}
				}
			}

			// Stop the PoolCleaner if the pool is empty.
			if (_connections.size() == 0 && _cleaner != null) {
				logger.info("No Active DB Connections. Stopping the thread");
				_cleaner.halt();
				_cleaner = null;
			}
		}
	}

	public int getPoolSize() {
		synchronized (_connections) {
			return _connections.size();
		}
	}

	/**
	 * Drains the pool. After the ConnectionPool has been drained it will not give
	 * out any more connections and all existing connections will be closed. This
	 * action cannot be reversed, so a ConnectionPool will become unusable once it
	 * has been drained.
	 **/
	public void drain() {
		_draining = true;

		if (_cleaner != null) {
			_cleaner.halt();
		}

		synchronized (_connections) {
			for (int i = _connections.size() - 1; i >= 0; i--) {
				PoolConnection pc = _connections.get(i);

				if (pc.inUse()) {
					System.err.println("Warning: forced closing of a connection still in use. " + pc.hashCode());
					System.err.println("Connection was acquired in:");
					pc.printStackTrace();
					System.err.println();
				}
				
				_connections.remove(i);
				try {
					pc.expire();
				} catch (SQLException ignore) {
				}
			}
		}
	}

	protected void finalize() {
		drain();
	}

	/*--------------------------------------------+
	| inner class PoolConnection                  |
	+--------------------------------------------*/

	/**
	 * Wrapper around java.sql.Connection
	 **/
	static class PoolConnection implements Connection {

		/*----------------------------------+
		| Variables                         |
		+----------------------------------*/

		protected Connection _conn;

		protected boolean _inUse;

		protected boolean _autoCommit;

		/** Time stamp for the last time the connection was opened. **/
		protected long _timeOpened;

		/** Time stamp for the last time the connection was closed. **/
		protected long _timeClosed;

		private Throwable _throwable;

		/*----------------------------------+
		| Constructors                      |
		+----------------------------------*/

		public PoolConnection(Connection conn) {
			_conn = conn;
			_inUse = false;
			_autoCommit = true;
		}

		/*----------------------------------+
		| PoolConnection specific methods   |
		+----------------------------------*/

		/**
		 * Tries to lease this connection. If the attempt was successful (the connection
		 * was available), a flag will be set marking this connection "in use", and this
		 * method will return 'true'. If the connection was already in use, this method
		 * will return 'false'.
		 **/
		public synchronized boolean lease() {
			if (_inUse) {
				return false;
			} else {
				_inUse = true;
				_timeOpened = System.currentTimeMillis();
				return true;
			}
		}

		/**
		 * Checks if the connection currently is used by someone.
		 **/
		public boolean inUse() {
			return _inUse;
		}

		/**
		 * Returns the time stamp of the last time this connection was opened/leased.
		 **/
		public synchronized long getTimeOpened() {
			return _timeOpened;
		}

		/**
		 * Returns the time stamp of the last time this connection was closed.
		 **/
		public synchronized long getTimeClosed() {
			return _timeClosed;
		}

		/**
		 * Expires this connection and closes the underlying connection to the database.
		 * Once expired, a connection can no longer be used.
		 **/
		public void expire() throws SQLException {
			_conn.close();
			_conn = null;
		}

		public void printStackTrace() {
			_throwable.printStackTrace(System.err);
		}

		/*----------------------------------+
		| Wrapping methods for Connection   |
		+----------------------------------*/

		public synchronized void close() throws SQLException {
			// Multiple calls to close?
			//logger.info("Rajasekhar close " + this.hashCode());
			if (_inUse) {
				//logger.info("Rajasekhar if " + this.hashCode());
				_timeClosed = System.currentTimeMillis();
				_inUse = false;

				if (_autoCommit == false) {
					// autoCommit has been set to false by this user,
					// restore the default "autoCommit = true"
					setAutoCommit(true);
				}
			}
		}

		public Statement createStatement() throws SQLException {
			_throwable = new Throwable();
			return _conn.createStatement();
		}

		public PreparedStatement prepareStatement(String sql) throws SQLException {
			_throwable = new Throwable();
			return _conn.prepareStatement(sql);
		}

		public CallableStatement prepareCall(String sql) throws SQLException {
			return _conn.prepareCall(sql);
		}

		public String nativeSQL(String sql) throws SQLException {
			return _conn.nativeSQL(sql);
		}

		public void setAutoCommit(boolean autoCommit) throws SQLException {
			_conn.setAutoCommit(autoCommit);
			_autoCommit = _conn.getAutoCommit();
		}

		public boolean getAutoCommit() throws SQLException {
			return _conn.getAutoCommit();
		}

		public void commit() throws SQLException {
			_conn.commit();
		}

		public void rollback() throws SQLException {
			_conn.rollback();
		}

		public boolean isClosed() throws SQLException {
			return _conn.isClosed();
		}

		public DatabaseMetaData getMetaData() throws SQLException {
			return _conn.getMetaData();
		}

		public void setReadOnly(boolean readOnly) throws SQLException {
			_conn.setReadOnly(readOnly);
		}

		public boolean isReadOnly() throws SQLException {
			return _conn.isReadOnly();
		}

		public void setCatalog(String catalog) throws SQLException {
			_conn.setCatalog(catalog);
		}

		public String getCatalog() throws SQLException {
			return _conn.getCatalog();
		}

		public void setTransactionIsolation(int level) throws SQLException {
			_conn.setTransactionIsolation(level);
		}

		public int getTransactionIsolation() throws SQLException {
			return _conn.getTransactionIsolation();
		}

		public SQLWarning getWarnings() throws SQLException {
			return _conn.getWarnings();
		}

		public void clearWarnings() throws SQLException {
			_conn.clearWarnings();
		}

		public Statement createStatement(int resultSetType, int resultSetConcurrency) throws SQLException {
			return _conn.createStatement(resultSetType, resultSetConcurrency);
		}

		public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency)
				throws SQLException {
			return _conn.prepareStatement(sql, resultSetType, resultSetConcurrency);
		}

		public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency)
				throws SQLException {
			return _conn.prepareCall(sql, resultSetType, resultSetConcurrency);
		}

		public Map<String, Class<?>> getTypeMap() throws SQLException {
			return _conn.getTypeMap();
		}

		public void setTypeMap(Map<String, Class<?>> map) throws SQLException {
			_conn.setTypeMap(map);
		}

		/*
		 * The following methods are new methods from java.sql.Connection that were
		 * added in JDK1.4. These additions are incompatible with older JDK versions.
		 */

		public void setHoldability(int holdability) throws SQLException {
			_conn.setHoldability(holdability);
		}

		public int getHoldability() throws SQLException {
			return _conn.getHoldability();
		}

		public Savepoint setSavepoint() throws SQLException {
			return _conn.setSavepoint();
		}

		public Savepoint setSavepoint(String name) throws SQLException {
			return _conn.setSavepoint(name);
		}

		public void rollback(Savepoint savepoint) throws SQLException {
			_conn.rollback(savepoint);
		}

		public void releaseSavepoint(Savepoint savepoint) throws SQLException {
			_conn.releaseSavepoint(savepoint);
		}

		public Statement createStatement(int resultSetType, int resultSetConcurrency, int resultSetHoldability)
				throws SQLException {
			return _conn.createStatement(resultSetType, resultSetConcurrency, resultSetHoldability);
		}

		public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency,
				int resultSetHoldability) throws SQLException {
			return _conn.prepareStatement(sql, resultSetType, resultSetConcurrency, resultSetHoldability);
		}

		public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency,
				int resultSetHoldability) throws SQLException {
			return _conn.prepareCall(sql, resultSetType, resultSetConcurrency, resultSetHoldability);
		}

		public PreparedStatement prepareStatement(String sql, int autoGenerateKeys) throws SQLException {
			return _conn.prepareStatement(sql, autoGenerateKeys);
		}

		public PreparedStatement prepareStatement(String sql, int[] columnIndexes) throws SQLException {
			return _conn.prepareStatement(sql, columnIndexes);
		}

		public PreparedStatement prepareStatement(String sql, String[] columnNames) throws SQLException {
			return _conn.prepareStatement(sql, columnNames);
		}

		public Clob createClob() throws SQLException {
			return null;
		}

		public Blob createBlob() throws SQLException {
			return null;
		}

		public NClob createNClob() throws SQLException {
			return null;
		}

		public SQLXML createSQLXML() throws SQLException {
			return null;
		}

		public boolean isValid(int timeout) throws SQLException {
			return false;
		}

		public void setClientInfo(String name, String value) throws SQLClientInfoException {
		}

		public void setClientInfo(Properties properties) throws SQLClientInfoException {
		}

		public String getClientInfo(String name) throws SQLException {
			return null;
		}

		public Properties getClientInfo() throws SQLException {
			return null;
		}

		public Array createArrayOf(String typeName, Object[] elements) throws SQLException {
			return null;
		}

		public Struct createStruct(String typeName, Object[] attributes) throws SQLException {
			return null;
		}

		public <T> T unwrap(Class<T> iface) throws SQLException {
			return null;
		}

		public boolean isWrapperFor(Class<?> iface) throws SQLException {
			return false;
		}

		@Override
		public void abort(Executor arg0) throws SQLException {
		}

		@Override
		public int getNetworkTimeout() throws SQLException {
			return 0;
		}

		@Override
		public String getSchema() throws SQLException {
			return null;
		}

		@Override
		public void setNetworkTimeout(Executor arg0, int arg1) throws SQLException {
		}

		@Override
		public void setSchema(String arg0) throws SQLException {
		}
	}

	
	/*--------------------------------------------+
	| inner class PoolCleaner                     |
	+--------------------------------------------*/

	class PoolCleaner extends Thread {

		protected long _cleaningInterval;
		protected boolean _mustStop;

		public PoolCleaner(long cleaningInterval) {
			if (cleaningInterval < 0) {
				throw new IllegalArgumentException("cleaningInterval must be >= 0");
			}
			_mustStop = false;
			_cleaningInterval = cleaningInterval;

			setDaemon(true);
		}

		public void run() {
			while (!_mustStop) {
				try {
					sleep(_cleaningInterval);
					logger.info("DB Connections cleanup task. Active Conn count {}", getPoolSize());
				} catch (InterruptedException ignore) {
				}
				if (_mustStop) {
					break;
				}
				removeExpired();
			}
		}

		public void halt() {
			_mustStop = true;
			synchronized (this) {
				this.interrupt();
			}
		}
	}
}
