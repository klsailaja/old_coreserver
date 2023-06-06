package com.ab.core.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.ab.core.pojo.UserAccumulatedResults;

/*
CREATE TABLE USERACCUMULATEDMONEY (ID BIGINT UNSIGNED NOT NULL AUTO_INCREMENT, 
		USERID BIGINT, 
		YEARINDEX INT,
		WINMONEY BIGINT,
		REFERMONEY BIGINT,
		ADDEDMONEY BIGINT,
		WITHDRAWNMONEY BIGINT,
		PRIMARY KEY (ID)) ENGINE = INNODB;
		
CREATE INDEX USERACCUMULATEDMONEY_Inx ON USERACCUMULATEDMONEY(USERID);		
DROP INDEX USERACCUMULATEDMONEY_Inx ON USERACCUMULATEDMONEY;		
CREATE INDEX USERACCUMULATEDMONEY_Inx ON USERACCUMULATEDMONEY(USERID);
CREATE INDEX USERACCUMULATEDMONEY_Inx1 ON USERACCUMULATEDMONEY(YEARINDEX);		
DROP INDEX USERACCUMULATEDMONEY_Inx1 ON USERACCUMULATEDMONEY;		
CREATE INDEX USERACCUMULATEDMONEY_Inx1 ON USERACCUMULATEDMONEY(YEARINDEX);
*/

public class UserAccumulatedResultsDBHandler {
	private static final Logger logger = LogManager.getLogger(UserAccumulatedResultsDBHandler.class);
	
	public static String TABLE_NAME = "USERACCUMULATEDMONEY";
	
	public static String ID = "ID";
	public static String USERID = "USERID";
	public static String YEAR_INDEX = "YEARINDEX";
	public static String WINMONEY = "WINMONEY";
	public static String REFERMONEY = "REFERMONEY";
	public static String ADDEDMONEY = "ADDEDMONEY";
	public static String WITHDRAWNMONEY = "WITHDRAWNMONEY";
	
	
	public static final String UPDATE_WINMONEY_BY_USER_ID = "UPDATE " + TABLE_NAME + " SET "
			+ WINMONEY + " = " + WINMONEY + " + ? WHERE " + USERID + " = ? AND " + YEAR_INDEX + " = ?";
	
	public static final String UPDATE_REFERMONEY_BY_USER_ID = "UPDATE " + TABLE_NAME + " SET "
			+ REFERMONEY + " = " + REFERMONEY + " + ? WHERE " + USERID + " = ? AND " + YEAR_INDEX + " = ?";
	
	public static final String UPDATE_ADDEDMONEY_BY_USER_ID = "UPDATE " + TABLE_NAME + " SET "
			+ ADDEDMONEY + " = " + ADDEDMONEY + " + ? WHERE " + USERID + " = ? AND " + YEAR_INDEX + " = ?";
	
	public static final String UPDATE_WITHDRAWNMONEY_BY_USER_ID = "UPDATE " + TABLE_NAME + " SET "
			+ WITHDRAWNMONEY + " = " + WITHDRAWNMONEY + " + ? WHERE " + USERID + " = ? AND " + YEAR_INDEX + " = ?";
	
	public static final String GET_BY_USER_ID = "SELECT " + WINMONEY + "," + REFERMONEY + "," 
			+ ADDEDMONEY + "," + WITHDRAWNMONEY 
			+ " FROM " + TABLE_NAME + " WHERE " + USERID + " = ? AND " + YEAR_INDEX + " = ?";
	
	// create a record
	// update the records in bulk mode
	// Get by year index
	
	private static UserAccumulatedResultsDBHandler instance = null;
	
	private static final String CREATE_MONEY_ENTRY = "INSERT INTO " + TABLE_NAME 
			+ "(" + USERID + "," + YEAR_INDEX + ","
			+ WINMONEY + "," + REFERMONEY + ","
			+ ADDEDMONEY + "," + WITHDRAWNMONEY
			+ ") VALUES" + "(?,?,?,?,?,?)";
	
	private UserAccumulatedResultsDBHandler() {
	}
	
	public static UserAccumulatedResultsDBHandler getInstance() {
		if (instance == null) {
			logger.debug("In UserAccumulatedResultsDBHandler getInstance() method instance created");
			instance = new UserAccumulatedResultsDBHandler();
		}
		return instance;
	}
	
	public void createInitialEntries(UserAccumulatedResults obj, int maxRows) throws SQLException {
		
		int[] yearIndices = getYearIndices(maxRows);
		boolean overallResult = true;
		for (int i = 0; i < maxRows; i++) {
			obj.setYearIndex(yearIndices[i]);
			boolean createResult = createEntry(obj);
			overallResult = overallResult & createResult;
		}
		logger.info("UserAccumulatedResults Object creation for {} and the result is {}", obj.getUid(), overallResult);
	}
	
	public boolean createEntry(UserAccumulatedResults obj) throws SQLException {
		
		logger.debug("In createEntry with {}", obj.getUid());
		
		ConnectionPool cp = null;
		Connection dbConn = null;
		PreparedStatement ps = null;
		
		try {
			cp = ConnectionPool.getInstance();
			dbConn = cp.getDBConnection();
			ps = dbConn.prepareStatement(CREATE_MONEY_ENTRY);
			
			ps.setLong(1, obj.getUid());
			ps.setInt(2, obj.getYearIndex());
			ps.setLong(3, obj.getWinAmount());
			ps.setLong(4, obj.getReferAmount());
			ps.setLong(5, obj.getAddedAmount());
			ps.setLong(6, obj.getWithdrawnAmount());
			
			int result = ps.executeUpdate();
			logger.info("createEntry with uid {} result is {}", obj.getUid(), (result > 0));
			return (result > 0);
			
		} catch(SQLException ex) {
			logger.error("******************************");
			logger.error("Error creating UserAccumulatedResults for id {} ", obj.getUid());
			logger.error("The Exception is", ex);
			logger.error("******************************");
			throw ex;
		} finally {
			if (ps != null) {
				ps.close();
			}
			if (dbConn != null) {
				dbConn.close();
			}
		}
	}
	
	public List<UserAccumulatedResults> getAllUserEntries(long uid, int maxRows) {
		
		int[] yearIndices = getYearIndices(maxRows);
		List<UserAccumulatedResults> list = new ArrayList<>();
		for (int i = 0; i < maxRows; i++) {
			UserAccumulatedResults obj = new UserAccumulatedResults();
			obj.setUid(uid);
			obj.setWinAmount(0);
			obj.setReferAmount(0);
			obj.setAddedAmount(0);
			obj.setWithdrawnAmount(0);
			obj.setYearIndex(yearIndices[i]);
			list.add(obj);
		}
		return list;
	}
	
	public void testCreateAccInBatch(List<UserAccumulatedResults> userAccList, int batchSize) throws SQLException {
		
		System.out.println("In testCreateAccInBatch with size " +  userAccList.size() + " batch size " + batchSize);
		
		ConnectionPool cp = null;
		Connection dbConn = null;
		PreparedStatement ps = null;
		
		int totalFailureCount = 0;
		int totalSuccessCount = 0;
		
		try {
			cp = ConnectionPool.getInstance();
			dbConn = cp.getDBConnection();
			dbConn.setAutoCommit(false);
			
			ps = dbConn.prepareStatement(CREATE_MONEY_ENTRY);
			
			int index = 0;
			for (UserAccumulatedResults obj : userAccList) {
				
				ps.setLong(1, obj.getUid());
				ps.setInt(2, obj.getYearIndex());
				ps.setLong(3, obj.getWinAmount());
				ps.setLong(4, obj.getReferAmount());
				ps.setLong(5, obj.getAddedAmount());
				ps.setLong(6, obj.getWithdrawnAmount());
			
				ps.addBatch();
				index++;
				
				if (index % batchSize == 0) {
					int results[] = ps.executeBatch();
					dbConn.setAutoCommit(false);
					dbConn.commit();
					for (int result : results) {
						if (result == 1) {
							++totalSuccessCount;
						} else {
							++totalFailureCount;
						}
					}
				}
			}
			if (index > 0) {
				int results[] = ps.executeBatch();
				dbConn.setAutoCommit(false);
				dbConn.commit();
				for (int result : results) {
					if (result == 1) {
						++totalSuccessCount;
					} else {
						++totalFailureCount;
					}
				}
			}
			logger.info("End of testCreateAccInBatch with success row count {} : failure row count {}", totalSuccessCount, totalFailureCount);
		} catch(SQLException ex) {
			logger.error("******************************");
			logger.error("Error in creating user accumulated list in bulk mode", ex);
			logger.error("******************************");
			throw ex;
		} finally {
			if (ps != null) {
				ps.close();
			}
			if (dbConn != null) {
				dbConn.close();
			}
		}
	}
	
	public int[] getYearIndices(int maxRows) {
		Calendar cldr = Calendar.getInstance();
        int month = cldr.get(Calendar.MONTH);
        int year = cldr.get(Calendar.YEAR);
        int yearIndex = -1;
        
        if (month <= Calendar.MARCH) {
        	yearIndex = year - 1;
        } else if (month >= Calendar.APRIL) {
        	yearIndex = year;
        }
        
        int[] yearIndices = new int[maxRows];
        for (int index = 0; index < maxRows; index ++) {
        	yearIndices[index] = yearIndex++;
        }
        return yearIndices;
	}
	
	public void updateAddedMoneyOrWDMoneyQuery(String sqlQryType, String sqlQry, long uid, long amt) throws SQLException {
		logger.info("This is in updateAddedMoneyOrWDMoneyQuery method");
		logger.info("Query to execute is uid : {} amt {} and query", uid, amt, sqlQry);
		
		int[] yearIndices = getYearIndices(3);
		
		ConnectionPool cp = null;
		Connection dbConn = null;
		PreparedStatement ps = null;
		
		try {
			cp = ConnectionPool.getInstance();
			dbConn = cp.getDBConnection();
			dbConn.setAutoCommit(true);
			
			ps = dbConn.prepareStatement(sqlQry);
			ps.setLong(1, amt);
			ps.setLong(2, uid);
			ps.setInt(3, yearIndices[0]);
			int result = ps.executeUpdate();
			if (result == 0) {
				logger.error("******************************");
				logger.error("The query sql is {}", sqlQry);
				logger.error("Exception while updating the user accumulated results obj for id {} amt {}", uid, amt);
				logger.error("******************************");
				throw new SQLException("Could not update"); 
			}
		} catch (SQLException ex) {
			logger.error("******************************");
			logger.error("The query sql is {}", sqlQry);
			logger.error("Exception while updating the user accumulated results obj for id {} amt {}", uid, amt);
			logger.error("******************************");
			throw ex;
		} finally {
			if (ps != null) {
				ps.close();
			}
			if (dbConn != null) {
				dbConn.close();
			}
		}
		
	}
	
	public void updateUsersMoneyEntriesInBatch(Map<Long, Long> userIdVsMoney, int batchSize, String sqlQry, String recordType) 
			throws SQLException {

		if (userIdVsMoney.size() == 0) {
			return;
		}
	
		logger.info("This is in updateUsersMoneyEntriesInBatch with records size {} and type {}", userIdVsMoney.size(),
				recordType);
		
		int[] yearIndices = getYearIndices(3);
		
		ConnectionPool cp = null;
		Connection dbConn = null;
		PreparedStatement ps = null;
		
		int totalFailureCount = 0;
		int totalSuccessCount = 0;
		
		try {
			cp = ConnectionPool.getInstance();
			dbConn = cp.getDBConnection();
			dbConn.setAutoCommit(false);
			
			ps = dbConn.prepareStatement(sqlQry);
			
			int index = 0;
			for (Map.Entry<Long, Long> entry : userIdVsMoney.entrySet()) {
				long userId = entry.getKey();
				long winAmt = entry.getValue();
			
				ps.setLong(1, (long)winAmt);
				ps.setLong(2, userId);
				ps.setInt(3, yearIndices[0]);
				
				ps.addBatch();
				index++;
				
				if (index % batchSize == 0) {
					int results[] = ps.executeBatch();
					dbConn.setAutoCommit(false);
					dbConn.commit();
					for (int result : results) {
						if (result == 1) {
							++totalSuccessCount;
						} else {
							++totalFailureCount;
						}
					}
				}
				if (index > 0) {
					int results[] = ps.executeBatch();
					dbConn.setAutoCommit(false);
					dbConn.commit();
					for (int result : results) {
						if (result == 1) {
							++totalSuccessCount;
						} else {
							++totalFailureCount;
						}
					}
				}
			}
			logger.info("End of updateUsersMoneyEntriesInBatch with success row count {} : failure row count {}", totalSuccessCount, totalFailureCount);
		} catch(SQLException ex) {
			logger.error("******************************");
			logger.error("Error in updateUsersMoneyEntriesInBatch in bulk mode", ex);
			logger.error("******************************");
			throw ex;
		} finally {
			if (ps != null) {
				ps.close();
			}
			if (dbConn != null) {
				dbConn.close();
			}
		}
	}
	
	public long[] getAccumulatedResults(long uid) throws SQLException {
		
		int[] yearIndices = getYearIndices(3);
		
		long[] results = new long[4];
		results[0] = -1;
		results[1] = -1;
		
		ConnectionPool cp = ConnectionPool.getInstance();
		Connection dbConn = cp.getDBConnection();
		
		PreparedStatement ps = dbConn.prepareStatement(GET_BY_USER_ID);
		ps.setLong(1, uid);
		ps.setInt(2, yearIndices[0]);
		
		ResultSet rs = null;
		
		try {
			rs = ps.executeQuery();
			if (rs != null) {
				if (rs.next()) {
					results[0] = rs.getLong(WINMONEY);
					results[1] = rs.getLong(REFERMONEY);
					results[2] = rs.getLong(ADDEDMONEY);
					results[3] = rs.getLong(WITHDRAWNMONEY);
				}
			}
		} catch (SQLException ex) {
			logger.error("******************************");
			logger.error("Exception while getting the user accumulated results obj for id {}", uid);
			logger.error("SQLException in getAccumulatedResults()", ex);
			logger.error("******************************");
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
		return results;
	}
}
