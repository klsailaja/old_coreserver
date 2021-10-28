package com.ab.core.helper;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.ab.core.constants.UserMoneyAccountType;
import com.ab.core.constants.UserMoneyOperType;
import com.ab.core.db.ConnectionPool;
import com.ab.core.db.UserMoneyDBHandler;
import com.ab.core.pojo.MoneyTransaction;
import com.ab.core.pojo.MyTransaction;
import com.ab.core.pojo.UserMoney;
import com.ab.core.pojo.UsersCompleteMoneyDetails;
import com.ab.core.tasks.AddTransactionsTask;



public class MoneyUpdater {
	
	private static MoneyUpdater instance = null;
	
	private static final Logger logger = LogManager.getLogger(MoneyUpdater.class);
	
	private Map<Long, UserMoney> userIdVsUserMoney = new HashMap<>();
	private Map<Long, List<MoneyTransaction>> userIdVsCurrentTransactions = new HashMap<>();
	private List<Long> loadUserIds = new ArrayList<>();
	private Map<Long, Long> userIdVsWinMoney = new HashMap<>();
	private Map<Long, Long> userIdVsReferMoney = new HashMap<>();
	
	private MoneyUpdater() {
	}
	
	public static MoneyUpdater getInstance() {
		if (instance == null) {
			instance = new MoneyUpdater();
		}
		return instance;
	}
	
	public void clearStates() {
		userIdVsUserMoney.clear();
		userIdVsCurrentTransactions.clear();
		loadUserIds.clear();
		userIdVsWinMoney.clear();
		userIdVsReferMoney.clear();
	}
	
	public synchronized List<Integer> performTransactions(UsersCompleteMoneyDetails usersMoneyDetails) throws SQLException {
		
		long startTime = System.currentTimeMillis();
		
		fetchUserMoneyObjectsFromDB(usersMoneyDetails);
		
		for (Long userId : loadUserIds) {
			List<MoneyTransaction> perUserTransactions = userIdVsCurrentTransactions.get(userId);
			if (perUserTransactions.size() == 0) {
				continue;
			}
			UserMoney userMoneyObject = userIdVsUserMoney.get(userId);
			if (userMoneyObject == null) {
				logger.info("Ignoring {} user id as user money not found", userId);
				continue;
			}
			
			long userOB = userMoneyObject.getAmount();
			long userBalance = userOB;
			long userCB = userOB;
			long userWinMoney = 0;
			long userReferMoney = 0;
			
			for (MoneyTransaction moneyTran : perUserTransactions) {
				
				UserMoneyAccountType userAccountType = moneyTran.getAccountType();
				if (moneyTran.getOperType() == UserMoneyOperType.ADD) {
					userBalance = userBalance + moneyTran.getAmount();
				} else {
					userBalance = userBalance - moneyTran.getAmount();
				}
				userCB = userBalance;
				userOB = userCB;
				moneyTran.getTransaction().setOpeningBalance(userOB);
				moneyTran.getTransaction().setClosingBalance(userCB);
				
				switch (userAccountType) {
					case WINNING_MONEY: {
						userWinMoney = userWinMoney + moneyTran.getAmount(); 
						break;
					}
					case REFERAL_MONEY: {
						userReferMoney = userReferMoney + moneyTran.getAmount();
						break;
					}
					case LOADED_MONEY: {
						break;
					}
				}
			}
	
			userIdVsWinMoney.put(userId, userWinMoney);
			userIdVsReferMoney.put(userId, userReferMoney);
			
		}
		
		List<Integer> moneyUpdateResults = bulkUpdate();
		
		UserMoneyDBHandler.getInstance().updateUsersMoneyEntriesInBatch(userIdVsReferMoney, 50, 
				UserMoneyDBHandler.UPDATE_REFERMONEY_BY_USER_ID, "REFER");
		UserMoneyDBHandler.getInstance().updateUsersMoneyEntriesInBatch(userIdVsWinMoney, 50, 
				UserMoneyDBHandler.UPDATE_WINMONEY_BY_USER_ID, "WIN");
		
		clearStates();
		
		logger.info("Total Time in MoneyUpdater performTransactions {}", (System.currentTimeMillis() - startTime));
		
		return moneyUpdateResults;
	}
	
	private List<Integer> bulkUpdate() throws SQLException {
		
		ConnectionPool cp = null;
		Connection dbConn = null;
		
		PreparedStatement ps = null;
		
		try {
			cp = ConnectionPool.getInstance();
			dbConn = cp.getDBConnection();
			
			ps = dbConn.prepareStatement(UserMoneyDBHandler.UPDATE_BALANCE_AMOUNT_BY_USER_ID);
			int index = 0;
			
			List<Integer> resultsList = new ArrayList<>();
			List<MoneyTransaction> allTransactions = new ArrayList<>();
			
			for (Long userId : loadUserIds) {
				List<MoneyTransaction> perUserTransactions = userIdVsCurrentTransactions.get(userId);
				if (perUserTransactions.size() == 0) {
					continue;
				}
				
				allTransactions.addAll(perUserTransactions);
				
				for (MoneyTransaction moneyTransaction : perUserTransactions) {
					++index;
					long amount = moneyTransaction.getAmount();
					if (moneyTransaction.getOperType() == UserMoneyOperType.SUBTRACT) {
						amount = -1 * amount;
					}
					
					ps.setLong(1,  amount);
					ps.setLong(2, moneyTransaction.getUserProfileId());
					ps.addBatch();
					
					if (index == 50) {
						int[] results = ps.executeBatch();
						dbConn.setAutoCommit(false);
						dbConn.commit();
						dbConn.setAutoCommit(true);
						for (int result : results) {
							resultsList.add(result);
						}
						index = 0;
					}
				}
			}
			
			if (index > 0) {
				int [] results = ps.executeBatch();
				dbConn.setAutoCommit(false);
				dbConn.commit();
				dbConn.setAutoCommit(true);
				for (int result : results) {
					resultsList.add(result);
				}
			}
			
			if (dbConn != null) {
				dbConn.close();
			}
			
			logger.info("Total records size and results size {} : {}", allTransactions.size(), resultsList.size());
			
			int size = allTransactions.size();
			int operResult = 0;
			for (int counter = 0; counter < size; counter ++) {
				operResult = 0;
				MoneyTransaction transaction = allTransactions.get(counter);
				if (resultsList.get(counter) > 0) {
					operResult = 1;
				}
				transaction.getTransaction().setOperResult(operResult);
			}
			
			List<MyTransaction> transactionsList = new ArrayList<>();
			for (MoneyTransaction moneyTran : allTransactions) {
				transactionsList.add(moneyTran.getTransaction());
			}
			
			SingleThreadAddTransactions.getInstance().submit(new AddTransactionsTask(transactionsList));
			
			return resultsList;
			
		} catch(SQLException ex) {
			logger.error("Error in bulk update in Money Updater", ex);
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
	
	private void fetchUserMoneyObjectsFromDB(UsersCompleteMoneyDetails usersMoneyDetails) throws SQLException {
		
		// Find out the user ids to do money transactions
		
		for (MoneyTransaction moneyTransaction : usersMoneyDetails.getUsersMoneyTransactionList()) {
			Long userId = moneyTransaction.getUserProfileId();
			if (!loadUserIds.contains(userId)) {
				loadUserIds.add(userId);
			}
			
			List<MoneyTransaction> userCurrentTrans = userIdVsCurrentTransactions.get(userId);
			if (userCurrentTrans == null) {
				userCurrentTrans = new ArrayList<>();
			}
			userCurrentTrans.add(moneyTransaction);
			userIdVsCurrentTransactions.put(userId, userCurrentTrans);
		}
		
		logger.info("Total user ids to load from DB size {}", loadUserIds.size());
		int size = loadUserIds.size();
		if (size <= 0) {
			return;
		}
		
		logger.info("UserMoney objects missing for {}", userIdVsUserMoney.size());
		
		ConnectionPool cp = ConnectionPool.getInstance();
		Connection dbConn = cp.getDBConnection();
		
		int index = 0;
		while (index < size) {
			int remainingSize = size - index;
			PreparedStatement ps = null;
			ResultSet rs = null;
			
			if (remainingSize >= 50) {
				String sql = UserMoneyDBHandler.GET_FIFTY_ENTRY_SET;
				ps = dbConn.prepareStatement(sql);
				for (int counter = 1; counter <= 50; counter ++) {
					ps.setLong(counter, loadUserIds.get(index++));
				}
				logger.info("Greater than 50");
			} else if (remainingSize >= 20) {
				String sql = UserMoneyDBHandler.GET_TWENTY_ENTRY_SET;
				ps = dbConn.prepareStatement(sql);
				for (int counter = 1; counter <= 20; counter ++) {
					ps.setLong(counter, loadUserIds.get(index++));
				}
				logger.info("Greater than 20");
			} else {
				String sql = UserMoneyDBHandler.GET_MONEY_ENTRY_BY_USER_ID;
				ps = dbConn.prepareStatement(sql);
				ps.setLong(1, loadUserIds.get(index++));
			}
			
			try {
				rs = ps.executeQuery();
				if (rs != null) {
					while (rs.next()) {
						
						UserMoney userMoney = new UserMoney();
						
						userMoney.setId(rs.getLong(UserMoneyDBHandler.ID));
						userMoney.setAmount(rs.getLong(UserMoneyDBHandler.BALANCE));
						userMoney.setAmtLocked(rs.getLong(UserMoneyDBHandler.BALANCE_LOCKED));
				
						userIdVsUserMoney.put(userMoney.getId(), userMoney);
					}
				}
			} catch (SQLException ex) {
				logger.error("SQLException executing prepared statement", ex);
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
		}
		
		logger.info("After size {}", userIdVsUserMoney.size());
	}
}
