package com.ab.core.helper;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.ab.core.common.LazyScheduler;
import com.ab.core.constants.MoneyCreditStatus;
import com.ab.core.constants.QuizConstants;
import com.ab.core.constants.UserMoneyAccountType;
import com.ab.core.constants.UserMoneyOperType;
import com.ab.core.constants.WithdrawReqState;
import com.ab.core.db.ConnectionPool;
import com.ab.core.db.UserAccumulatedResultsDBHandler;
import com.ab.core.db.UserMoneyDBHandler;
import com.ab.core.exceptions.NotAllowedException;
import com.ab.core.pojo.GameSlotMoneyStatus;
import com.ab.core.pojo.MoneyTransaction;
import com.ab.core.pojo.MyTransaction;
import com.ab.core.pojo.UserMoney;
import com.ab.core.pojo.UsersCompleteMoneyDetails;
import com.ab.core.pojo.WithdrawMoney;
import com.ab.core.tasks.AddTransactionsTask;
import com.ab.core.tasks.UserAccumulatedUpdateTask;

public class MoneyUpdater {
	
	private static MoneyUpdater instance = null;
	
	private static final Logger logger = LogManager.getLogger(MoneyUpdater.class);
	
	private Map<Long, UserMoney> userIdVsUserMoney = new HashMap<>();
	private Map<Long, List<MoneyTransaction>> userIdVsCurrentTransactions = new HashMap<>();
	private List<Long> loadUserIds = new ArrayList<>();
	
	private ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
	private WriteLock writeLock = lock.writeLock();
	
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
	}
	
	public GameSlotMoneyStatus performTransactions(UsersCompleteMoneyDetails usersMoneyDetails) 
			throws SQLException, Exception {
		
		boolean lockAcquired = false;
		try {
			lockAcquired = writeLock.tryLock(20, TimeUnit.SECONDS);  
		} catch(InterruptedException ex) {
			lockAcquired = false;
		}
		
		if (!lockAcquired) {
			throw new Exception("Server Busy. Try after 2 minutes");
		}
		
		try {
				
			long startTime = System.currentTimeMillis();
			
			GameSlotMoneyStatus response = new GameSlotMoneyStatus();
			response.setServerId(usersMoneyDetails.getServerId());
			response.setRequestId(usersMoneyDetails.getRequestId());
			response.setOperationType(usersMoneyDetails.getOperationType());
			
			Map<Long, Long> userIdVsWinMoney = new HashMap<>();
			Map<Long, Long> userIdVsReferMoney = new HashMap<>();
	
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
				long userLoadedMoney = 0;
				
				for (MoneyTransaction moneyTran : perUserTransactions) {
					long transactionAmount = moneyTran.getAmount();
					UserMoneyAccountType userAccountType = moneyTran.getAccountType();
					if (moneyTran.getOperType() == UserMoneyOperType.ADD) {
						userBalance = userBalance + transactionAmount;
					} else {
						userBalance = userBalance - transactionAmount;
					}
					userCB = userBalance;
					moneyTran.getTransaction().setOpeningBalance(userOB);
					moneyTran.getTransaction().setClosingBalance(userCB);
					userOB = userCB;
					
					if (userAccountType == UserMoneyAccountType.WINNING_MONEY) {
						if (moneyTran.getOperType() == UserMoneyOperType.ADD) {
							userWinMoney = userWinMoney + transactionAmount;
						} else {
							userWinMoney = userWinMoney - transactionAmount;
						}
					} else if (userAccountType == UserMoneyAccountType.REFERAL_MONEY) {
						if (moneyTran.getOperType() == UserMoneyOperType.ADD) {
							userReferMoney = userReferMoney + transactionAmount;
						} else {
							userReferMoney = userReferMoney - transactionAmount;
						}
					} else if (userAccountType == UserMoneyAccountType.LOADED_MONEY) {
						if (moneyTran.getOperType() == UserMoneyOperType.ADD) {
							userLoadedMoney = userLoadedMoney + transactionAmount;
						} 
						UserAccumulatedResultsDBHandler.getInstance().
						updateAddedMoneyOrWDMoneyQuery("AddedMoney", 
								UserAccumulatedResultsDBHandler.UPDATE_ADDEDMONEY_BY_USER_ID, userId, userLoadedMoney);
					}
				}
		
				if (userWinMoney > 0) {
					userIdVsWinMoney.put(userId, userWinMoney);
				}
				
				if (userReferMoney > 0) {
					userIdVsReferMoney.put(userId, userReferMoney);
				}
			}
			
			List<Integer> moneyUpdateResults = bulkUpdate(response);
			
			logger.info("userIdVsWinMoney : {}", userIdVsWinMoney);
			logger.info("userIdVsReferMoney : {}", userIdVsReferMoney);
			
			UserAccumulatedUpdateTask run = new UserAccumulatedUpdateTask(userIdVsWinMoney, userIdVsReferMoney);
			SingleThreadMoneyUpdater.getInstance().submit(run);
			
			int totalWinTransactionsSize = moneyUpdateResults.size();
			int failedOperationsSize = 0;
			int successOperationsSize = 0;
			for (int index : moneyUpdateResults) {
				if (index > 0) {
					successOperationsSize++;
				} else if (index == 0) {
					failedOperationsSize++;
				}
			}
			
			clearStates();
			
			int moneyCreditState = MoneyCreditStatus.IN_PROGRESS.getId();
			if (successOperationsSize == totalWinTransactionsSize) {
				moneyCreditState = MoneyCreditStatus.ALL_SUCCESS.getId();
			} else if (failedOperationsSize == totalWinTransactionsSize) {
				moneyCreditState = MoneyCreditStatus.ALL_FAIL.getId();
			} else if ((successOperationsSize + failedOperationsSize) == totalWinTransactionsSize) {
				moneyCreditState = MoneyCreditStatus.PARTIAL_RESULTS.getId();
			}
			response.setMoneyCreditedStatus(moneyCreditState);
			response.setDbResultsIds(moneyUpdateResults);
			
			WinnersMoneyUpdateStatus.getInstance().setStatusToComplete(response.getRequestId(), response.getServerId(),
					response.getUniqueIds(), response.getDbResultsIds(), response.getMoneyCreditedStatus());
					
			
			logger.info("Total Time in MoneyUpdater performTransactions {}", (System.currentTimeMillis() - startTime));
			
			return response;
		
		} catch(Exception ex) {
			logger.error(QuizConstants.ERROR_PREFIX_START);
			logger.error("Error executing MoneyUpdater performTransactions", ex);
			logger.error(QuizConstants.ERROR_PREFIX_END);
			
		} finally {
			writeLock.unlock();
		}
		return null;
	}
	
	private List<Integer> bulkUpdate(GameSlotMoneyStatus mResponse) throws SQLException {
		
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
			List<Integer> requestIds = new ArrayList<>();
			
			for (Long userId : loadUserIds) {
				List<MoneyTransaction> perUserTransactions = userIdVsCurrentTransactions.get(userId);
				if (perUserTransactions.size() == 0) {
					continue;
				}
				
				allTransactions.addAll(perUserTransactions);
				
				for (MoneyTransaction moneyTransaction : perUserTransactions) {
					
					requestIds.add(moneyTransaction.getUniqueId());
					
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
				if (operResult == 1) {
					transaction.getTransaction().setExtraDetails("");
				}
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
		
		logger.info("UserMoney present in cache {}", userIdVsUserMoney.size());
		
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
	
	public boolean performWitdrawOperation(WithdrawMoney wdMoney) throws SQLException, Exception {
		
		boolean lockAcquired = false;
		try {
			lockAcquired = writeLock.tryLock(20, TimeUnit.SECONDS);  
		} catch(InterruptedException ex) {
			lockAcquired = false;
		}
		
		if (!lockAcquired) {
			throw new Exception("Server Busy. Try after 2 minutes");
		}
		
		String wdSql = UserMoneyDBHandler.WITHDRAW_BALANCE_AMOUNT_BY_USER_ID;

		long userProfileId = wdMoney.getUid();
		
		long balance = wdMoney.getWdAmt();
		long lockBalance = wdMoney.getWdAmt();
		
		UserMoney userMoney = getUserMoney(userProfileId);
		long userOB = userMoney.getAmount();
		long userCB = userOB; 
		
		if (wdMoney.getWdType() == WithdrawReqState.OPEN.getId()) {
			userCB = userOB - balance;
			balance = -1 * balance;
			lockBalance = wdMoney.getWdAmt();
		} else if (wdMoney.getWdType() == WithdrawReqState.CANCELLED.getId()) {
			balance = wdMoney.getWdAmt();
			lockBalance = -1 * lockBalance;
			userCB = userOB + balance;
		} else if (wdMoney.getWdType() == WithdrawReqState.CLOSED.getId()) {
			balance = 0;
			lockBalance = -1 * lockBalance;
		}
		
		ConnectionPool cp = null;
		Connection dbConn = null;
		PreparedStatement ps = null;
		
		
		
		int withDrawResult = 0;
		
		try {
			cp = ConnectionPool.getInstance();
			dbConn = cp.getDBConnection();
			ps = dbConn.prepareStatement(wdSql);
			
			ps.setLong(1, balance);
			ps.setLong(2, lockBalance);
			ps.setLong(3, userProfileId);
			
			int createResult = ps.executeUpdate();
			if (createResult > 0) {
				withDrawResult = 1;
			}
			logger.debug("WithdrawMoney operation result {}", withDrawResult);
			wdMoney.getTransaction().setOperResult(withDrawResult);
			wdMoney.getTransaction().setOpeningBalance(userOB);
			wdMoney.getTransaction().setClosingBalance(userCB);
			
			List<MyTransaction> wdRelatedTransactions = new ArrayList<>();
			wdRelatedTransactions.add(wdMoney.getTransaction());
			
			UserAccumulatedResultsDBHandler.getInstance().
			updateAddedMoneyOrWDMoneyQuery("WDAccumulatedMoney", 
					UserAccumulatedResultsDBHandler.UPDATE_WITHDRAWNMONEY_BY_USER_ID, userProfileId, balance);
			
			LazyScheduler.getInstance().submit(new AddTransactionsTask(wdRelatedTransactions));
		} catch(SQLException ex) {
			logger.error("Error while executing withdraw lock money statement", ex);
			throw ex;
		} finally {
			if (ps != null) {
				ps.close();
			}
			if (dbConn != null) {
				dbConn.close();
			}
		}
		return true;
	}
	
	private UserMoney getUserMoney(long id) throws SQLException, NotAllowedException {
		UserMoney userMoneyDb = UserMoneyDBHandler.getInstance().getUserMoneyById(id);
		return userMoneyDb;
	}
}
