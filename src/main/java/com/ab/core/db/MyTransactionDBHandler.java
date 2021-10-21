package com.ab.core.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.ab.core.exceptions.NotAllowedException;
import com.ab.core.pojo.MyTransaction;
import com.ab.core.pojo.TransactionsHolder;
import com.ab.core.pojo.UserProfile;

/*
CREATE TABLE TRANSACTIONS(ID BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
 		USERID BIGINT NOT NULL,
		DATE BIGINT NOT NULL,
		AMOUNT INT NOT NULL,
		ACCOUNTTYPE INT NOT NULL,
		TRANSACTIONTYPE INT NOT NULL,
		ISWIN INT NOT NULL,
		RESULT INT NOT NULL,
		OPENINGBALANCE BIGINT NOT NULL,
		CLOSINGBALANCE BIGINT NOT NULL,
		TRANACTIONID VARCHAR(5) NOT NULL,
		COMMENTS VARCHAR(100) NULL, PRIMARY KEY (ID)) ENGINE = INNODB;
		
CREATE INDEX TRANSACTIONS_Inx ON TRANSACTIONS(USERID);		
DROP INDEX TRANSACTIONS_Inx ON TRANSACTIONS;		
CREATE INDEX TRANSACTIONS_Inx ON TRANSACTIONS(USERID);
CREATE FULLTEXT INDEX TRANSACTIONS_Inx1 ON TRANSACTIONS(COMMENTS);		
DROP INDEX TRANSACTIONS_Inx1 ON TRANSACTIONS;		
CREATE FULLTEXT INDEX TRANSACTIONS_Inx1 ON TRANSACTIONS(COMMENTS);
*/

public class MyTransactionDBHandler {
	// Create an entry
	// get list by userId and account type with limit parameter
	private static String TABLE_NAME = "TRANSACTIONS";
	
	private static String ID = "ID";
	private static String USERID = "USERID";
	private static String DATE = "DATE";
	private static String AMOUNT = "AMOUNT";
	private static String ACCOUNT_TYPE = "ACCOUNTTYPE";
	private static String TRANSACTION_TYPE = "TRANSACTIONTYPE";
	private static String ISWIN = "ISWIN";
	private static String RESULT = "RESULT";
	private static String OB = "OPENINGBALANCE";
	private static String CB = "CLOSINGBALANCE";
	private static String COMMENTS = "COMMENTS";
	private static String TRANACTIONID = "TRANACTIONID"; 
	
	private static int MAX_ROWS = 5;
	
	private static final String CREATE_TRANSACTION_ENTRY = "INSERT INTO " + TABLE_NAME   
			+ "(" + USERID + "," + DATE + "," + AMOUNT + "," + ACCOUNT_TYPE + ","
			+ TRANSACTION_TYPE + "," + ISWIN + "," + RESULT + "," + OB + "," + CB + "," + TRANACTIONID + "," 
			+ COMMENTS + ") VALUES"
			+ "(?,?,?,?,?,?,?,?,?,?,?)";
	private static final String GET_TRANSACTIONS_BY_USER_ID_ACC_TYPE = "SELECT * FROM " + TABLE_NAME 
			+ " WHERE " + USERID + " = ? AND " + ACCOUNT_TYPE + " = ? ORDER BY " + ID + " DESC LIMIT ?, " + MAX_ROWS;
	
	private static final String GET_TRANSACTIONS_BY_USER_ID = "SELECT * FROM " + TABLE_NAME 
			+ " WHERE " + USERID + " = ? ORDER BY " + ID + " DESC LIMIT ?, " + MAX_ROWS;
		
	private static final String GET_TOTAL_COUNT = "SELECT COUNT(*) FROM " + TABLE_NAME + " WHERE "
			+ USERID + " = ?";
	private static final String GET_TOTAL_COUNT_BY_ACCTYPE = "SELECT COUNT(*) FROM " + TABLE_NAME + " WHERE "
			+ USERID + " = ? AND " + ACCOUNT_TYPE + " = ?";
	
	private static final String LATEST_WIN_RECORDS = "SELECT " + USERID + "," + AMOUNT + "," + DATE + " FROM " + TABLE_NAME 
			+ " WHERE " + ISWIN + " = 1 AND " + ACCOUNT_TYPE + " = 2 ORDER BY " + ID + " DESC LIMIT 0,120";
	private static final String LATEST_BOSS_WIN_RECORDS = "SELECT " + USERID + "," + AMOUNT + "," + DATE + " FROM " + TABLE_NAME 
			+ " WHERE " + USERID + " = ? AND " + ACCOUNT_TYPE + " = 2 AND " + ISWIN + " = 1 ORDER BY " + ID + " DESC LIMIT 0,10";
	private static final String REMOVE_OLD_RECORDS = "DELETE FROM " + TABLE_NAME 
			+ " WHERE (" + DATE + " < ? AND ID <> 0)";
	
	private static final Logger logger = LogManager.getLogger(MyTransactionDBHandler.class);
	private static MyTransactionDBHandler instance = null;
	
	
	
	private MyTransactionDBHandler() {
	}
	
	public static MyTransactionDBHandler getInstance() {
		if (instance == null) {
			logger.debug("In MyTransactionDBHandler getInstance() method instance created");
			instance = new MyTransactionDBHandler();
		}
		return instance;
	}
	
	public int deleteRecords(long timePeriod) throws SQLException {
		logger.info("In deleteRecords method");
		
		ConnectionPool cp = null;
		Connection dbConn = null;
		PreparedStatement ps = null;
		
		try {
			cp = ConnectionPool.getInstance();
			dbConn = cp.getDBConnection();
			ps = dbConn.prepareStatement(REMOVE_OLD_RECORDS);
			
			ps.setLong(1, timePeriod);
			
			int result = ps.executeUpdate();
			logger.debug("In deleteRecords create op result : {}", result);
			
			return result;
		} catch (SQLException ex) {
			logger.error("Error in deleteRecords ", ex);
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
	
	public void createTransactionsInBatch(List<MyTransaction> transactionsList, int batch) throws SQLException {
		
		ConnectionPool cp = null;
		Connection dbConn = null;
		PreparedStatement ps = null;
		
		try {
			cp = ConnectionPool.getInstance();
			dbConn = cp.getDBConnection();
			
			dbConn.setAutoCommit(false);
			
			ps = dbConn.prepareStatement(CREATE_TRANSACTION_ENTRY);
			
			int index = 0;
			int totalFailureCount = 0;
			int totalSuccessCount = 0;

			
			for (MyTransaction myTransaction : transactionsList) {
				
				ps.setLong(1, myTransaction.getUserId());
				ps.setLong(2, myTransaction.getDate());
				ps.setInt(3, myTransaction.getAmount());
				ps.setInt(4, myTransaction.getAccountType());
				ps.setInt(5, myTransaction.getTransactionType());
				ps.setInt(6, myTransaction.getIsWin());
				ps.setInt(7, myTransaction.getOperResult());
				ps.setLong(8, myTransaction.getOpeningBalance());
				ps.setLong(9, myTransaction.getClosingBalance());
				ps.setString(10, myTransaction.getTransactionId());
				String comment = myTransaction.getComment();
				if (myTransaction.getOperResult() == 0) {
					comment = comment + "." + "Backend issue while update. Will be resolved in 1-2 days.";
				}
				ps.setString(11, comment);
				index++;
				
				ps.addBatch();
				
				if (index == batch) {
					int[] results = ps.executeBatch();
					dbConn.setAutoCommit(false);
					dbConn.commit();
					for (int result : results) {
						if (result == 1) {
							++totalSuccessCount;
						} else {
							++totalFailureCount;
						}
					}
					index = 0;
				}
			}
			
			if (index > 0) {
				int [] results = ps.executeBatch();
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
			logger.info("createTransactionsInBatch returned with success row count {} : failure row count {}", 
					totalSuccessCount, totalFailureCount);
		} catch(SQLException ex) {
			logger.error("******************************");
			logger.error("Error processing transactions list in bulk mode", ex);
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
	
	public boolean createTransaction(MyTransaction myTransaction) throws SQLException {
		
		ConnectionPool cp = null;
		Connection dbConn = null;
		PreparedStatement ps = null;
		
		try {
			cp = ConnectionPool.getInstance();
			dbConn = cp.getDBConnection();
			ps = dbConn.prepareStatement(CREATE_TRANSACTION_ENTRY);
			
			ps.setLong(1, myTransaction.getUserId());
			ps.setLong(2, myTransaction.getDate());
			ps.setInt(3, myTransaction.getAmount());
			ps.setInt(4, myTransaction.getAccountType());
			ps.setInt(5, myTransaction.getTransactionType());
			ps.setInt(6, myTransaction.getIsWin());
			ps.setInt(7, myTransaction.getOperResult());
			ps.setLong(8, myTransaction.getOpeningBalance());
			ps.setLong(9, myTransaction.getClosingBalance());
			ps.setString(10, myTransaction.getTransactionId());
			String comment = myTransaction.getComment();
			if (myTransaction.getOperResult() == 0) {
				comment = comment + "." + "Backend issue while update. Will be resolved in 1-2 days.";
			}
			ps.setString(11, comment);
			
			int createResult = ps.executeUpdate();
			boolean recordCreationState = (createResult > 0);
			logger.debug("Transaction creation status {}", recordCreationState);
			return recordCreationState;
		} catch(SQLException ex) {
			logger.error("******************************");
			logger.error("Error creating transaction entry", ex);
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
	
	public List<String> getRecentWinRecords(long userProfileId, boolean isBoss, String bossUserName) throws SQLException {
		
		ConnectionPool cp = ConnectionPool.getInstance();
		Connection dbConn = cp.getDBConnection();
		ResultSet rs = null;
		
		String sql = LATEST_WIN_RECORDS;
		if (isBoss) {
			sql = LATEST_BOSS_WIN_RECORDS;
		}
		
		PreparedStatement ps = dbConn.prepareStatement(sql);
		if (isBoss) {
			ps.setLong(1, userProfileId);
		}
		
		List<String> winMessages = new ArrayList<>();
		String msg1 = "$NAME won Rs.$AMT recently";
		String msg2 = "Your network friend $NAME won Rs.$AMT recently";
		try {
			rs = ps.executeQuery();
			if (rs != null) {
				while (rs.next()) {
					long userId = rs.getLong(USERID);
					int amt = rs.getInt(AMOUNT);
					//long dateTime = rs.getLong(DATE);
					
					String userName = null;
					String str = msg1;
					if (isBoss) {
						userName = bossUserName;
						str = msg2;
					} else {
						UserProfile userProfile = UserProfileDBHandler.getInstance().getProfileById(userId);
						userName = userProfile.getName();
					}
					
					str = str.replace("$NAME", userName);
					str = str.replace("$AMT", String.valueOf(amt));
					
					winMessages.add(str);
				}
			}
		} catch(SQLException ex) {
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
		return winMessages;
	}
	
	public TransactionsHolder getTransactions(long userProfileId, int startRowNumber, int accType) 
			throws SQLException, NotAllowedException {
		
		logger.info("In getTransactions() with {} {} {}", userProfileId, accType, startRowNumber);
		
		UserProfile userProfile = UserProfileDBHandler.getInstance().getProfileById(userProfileId);
		if (userProfile.getId() == 0) {
			throw new NotAllowedException("User not found with id " + userProfileId);
		}
		
		String totalSql = GET_TOTAL_COUNT;
		String sql = GET_TRANSACTIONS_BY_USER_ID;
		
		if (accType != -1) {
			totalSql = GET_TOTAL_COUNT_BY_ACCTYPE;
			sql = GET_TRANSACTIONS_BY_USER_ID_ACC_TYPE;
		}
		
		ConnectionPool cp = ConnectionPool.getInstance();
		Connection dbConn = cp.getDBConnection();
		
		PreparedStatement totalPs = dbConn.prepareStatement(totalSql);
		PreparedStatement ps = dbConn.prepareStatement(sql);
		
		totalPs.setLong(1, userProfileId);
		ps.setLong(1, userProfileId);
		
		if (accType != -1) {
			totalPs.setInt(2, accType);
			
			ps.setInt(2, accType);
			ps.setInt(3, startRowNumber);
		} else {
			ps.setInt(2, startRowNumber);
		}
		
		TransactionsHolder transactionsDetails = new TransactionsHolder();
		List<MyTransaction> myTransactions = new ArrayList<>();
		ResultSet totalRs = null;
		ResultSet rs = null;
		
		try {
			totalRs = totalPs.executeQuery();
			if (totalRs != null) {
				if (totalRs.next()) {
					
					int total = totalRs.getInt("COUNT(*)");
					transactionsDetails.setTotal(total);
					
					int lowerRange = startRowNumber + 1;
					int higherRange = startRowNumber + MAX_ROWS;
					
					if (higherRange < total) {
						transactionsDetails.setNextEnabled(true);
					} else {
						transactionsDetails.setNextEnabled(false);
					}
					if ((lowerRange - MAX_ROWS) > 0) {
						transactionsDetails.setPrevEnabled(true);
					} else {
						transactionsDetails.setPrevEnabled(false);
					}
					
				}
			}
			
			rs = ps.executeQuery();
			if (rs != null) {
				while (rs.next()) {
					
					MyTransaction userTrans = new MyTransaction();
					
					userTrans.setsNo(++startRowNumber);
					
					userTrans.setId(rs.getLong(ID));
					userTrans.setUserId(rs.getLong(USERID));
					userTrans.setDate(rs.getLong(DATE));
					userTrans.setAmount(rs.getInt(AMOUNT));
					userTrans.setAccountType(rs.getInt(ACCOUNT_TYPE));
					userTrans.setTransactionType(rs.getInt(TRANSACTION_TYPE));
					userTrans.setOperResult(rs.getInt(RESULT));
					userTrans.setOpeningBalance(rs.getLong(OB));
					userTrans.setClosingBalance(rs.getLong(CB));
					userTrans.setComment(rs.getString(COMMENTS));
					userTrans.setTransactionId(rs.getString(TRANACTIONID));
					
					myTransactions.add(userTrans);
				}
			}
		} catch (SQLException ex) {
			throw ex;
		} finally {
			if (totalRs != null) {
				totalRs.close();
			}
			if (rs != null) {
				rs.close();
			}
			if (totalPs != null) {
				totalPs.close();
			}
			if (ps != null) {
				ps.close();
			}
			if (dbConn != null) {
				dbConn.close();
			}
		}
		transactionsDetails.setTransactionsList(myTransactions);
		logger.info("In getTransaction list. Returned with sie {}", myTransactions.size());
		return transactionsDetails;
	}
	
	public static void main(String[] args) throws SQLException {
		
		MyTransactionDBHandler instance = MyTransactionDBHandler.getInstance();
		
		long total = 800000;
		List<MyTransaction> transactionList = new ArrayList<>();
		
		for (int i = 1; i <= total; i++) {
			MyTransaction trans = new MyTransaction();
			
			trans.setId(i);
			trans.setUserId(1000);
			trans.setDate(System.currentTimeMillis());
			trans.setAmount(100);
			trans.setAccountType(1);
			trans.setTransactionType(1);
			trans.setOperResult(1);
			trans.setOpeningBalance(1000);
			trans.setClosingBalance(1100);
			trans.setComment("Testing");
			
			transactionList.add(trans);
		}
		System.out.println("Transactions size " + transactionList.size());
		instance.createTransactionsInBatch(transactionList, 200);
	}
}
