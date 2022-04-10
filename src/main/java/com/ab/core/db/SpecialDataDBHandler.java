package com.ab.core.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.ab.core.constants.QuizConstants;

/*
CREATE TABLE SPECIALCODEINFO(ID INT UNSIGNED NOT NULL AUTO_INCREMENT,
		COUNT INT NOT NULL,
 		MAXID BIGINT, PRIMARY KEY (ID)) ENGINE = INNODB;
INSERT INTO SPECIALCODEINFO (COUNT, MAXID) VALUES(0, 0);		
*/

public class SpecialDataDBHandler {
	
	private static String TABLE_NAME = "SPECIALCODEINFO";
	
	private static String ID = "ID";
	private static String COUNT = "COUNT";
	private static String MAXID = "MAXID";
	
	private static final String GET_INFO_ENTRY_BY_ID = "SELECT * FROM " + TABLE_NAME 
			+ " WHERE " + ID + " = 1";
	private static final String UPDATE_INFO_ENTRY_BY_ID = "UPDATE " + TABLE_NAME + " SET " 
			+ COUNT + " = " + COUNT + " + ? WHERE " + ID + " = 1";
	private static final String UPDATE_MAXID_ENTRY_BY_ID = "UPDATE " + TABLE_NAME + " SET " 
			+ MAXID + " = ? WHERE " + ID + " = 1";
	private static final Logger logger = LogManager.getLogger(SpecialDataDBHandler.class);
	
	private static SpecialDataDBHandler instance = null;
	
	// get api
	// increment
	
	private SpecialDataDBHandler() {
	}
	
	public static SpecialDataDBHandler getInstance() {
		if (instance == null) {
			logger.debug("In SpecialDataDBHandler getInstance() method instance created");
			instance = new SpecialDataDBHandler();
		}
		return instance;
	}
	
	public long getCurrentMaxId() throws SQLException {
		
		ConnectionPool cp = ConnectionPool.getInstance();
		Connection dbConn = cp.getDBConnection();
		
		PreparedStatement ps = dbConn.prepareStatement(GET_INFO_ENTRY_BY_ID);
		ResultSet rs = null;
		
		try {
			rs = ps.executeQuery();
			if (rs != null) {
				if (rs.next()) {
					return rs.getLong(MAXID);
				}
			}
		} catch (SQLException ex) {
			logger.error(QuizConstants.ERROR_PREFIX_START);
			logger.error("Exception while getting the max id");
			logger.error("SQLException in getCurrentMaxId()", ex);
			logger.error(QuizConstants.ERROR_PREFIX_END);
			throw ex;
		} finally {
			if (rs != null) {
				rs.close();
			}
			if (ps != null) {
				ps.close();
			}
			if (dbConn != null) {
				dbConn.close();
			}
		}
		return -1;
	}
	
	public int getSpecialCodeUsedCount() throws SQLException {
		
		ConnectionPool cp = ConnectionPool.getInstance();
		Connection dbConn = cp.getDBConnection();
		
		PreparedStatement ps = dbConn.prepareStatement(GET_INFO_ENTRY_BY_ID);
		ResultSet rs = null;
		
		try {
			rs = ps.executeQuery();
			if (rs != null) {
				if (rs.next()) {
					return rs.getInt(COUNT);
				}
			}
		} catch (SQLException ex) {
			logger.error(QuizConstants.ERROR_PREFIX_START);
			logger.error("Exception while getting the special code used count");
			logger.error("SQLException in getSpecialCodeUsedCount()", ex);
			logger.error(QuizConstants.ERROR_PREFIX_END);
			throw ex;
		} finally {
			if (rs != null) {
				rs.close();
			}
			if (ps != null) {
				ps.close();
			}
			if (dbConn != null) {
				dbConn.close();
			}
		}
		return -1;
	}
	
	public boolean updateMaxCount(long maxCount) throws SQLException {
		logger.debug("In updateMaxCount {}", maxCount);
		ConnectionPool cp = ConnectionPool.getInstance();
		Connection dbConn = cp.getDBConnection();
		PreparedStatement ps = dbConn.prepareStatement(UPDATE_MAXID_ENTRY_BY_ID);
		ps.setLong(1, maxCount);
		
		try {
			int resultCount = ps.executeUpdate();
			if (resultCount > 0) {
				return true;
			}
		}
		catch(SQLException ex) {
			logger.error(QuizConstants.ERROR_PREFIX_START);
			logger.error("Exception in updateMaxCount");
			logger.error("SQLException in ", ex);
			logger.error(QuizConstants.ERROR_PREFIX_END);
			throw ex;
		} finally {
			if (ps != null) {
				ps.close();
			}
			if (dbConn != null) {
				dbConn.close();
			}
		}
		return false;
	}
	
	public boolean incrementSpecialCodeCount() throws SQLException {
		
		logger.debug("In incrementSpecialCodeCount");
		ConnectionPool cp = ConnectionPool.getInstance();
		Connection dbConn = cp.getDBConnection();
		PreparedStatement ps = dbConn.prepareStatement(UPDATE_INFO_ENTRY_BY_ID);
		ps.setInt(1, 1);
		
		try {
			int resultCount = ps.executeUpdate();
			if (resultCount > 0) {
				return true;
			}
		}
		catch(SQLException ex) {
			logger.error(QuizConstants.ERROR_PREFIX_START);
			logger.error("Exception while incrementCount");
			logger.error("SQLException in ", ex);
			logger.error(QuizConstants.ERROR_PREFIX_END);
			throw ex;
		} finally {
			if (ps != null) {
				ps.close();
			}
			if (dbConn != null) {
				dbConn.close();
			}
		}
		return false;
	}
}
