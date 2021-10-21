package com.ab.core.helper;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.ab.core.common.LazyScheduler;
import com.ab.core.constants.UserMoneyAccountType;
import com.ab.core.constants.UserMoneyOperType;
import com.ab.core.db.ConnectionPool;
import com.ab.core.db.MyTransactionDBHandler;
import com.ab.core.db.UserMoneyDBHandler;
import com.ab.core.pojo.MoneyTransaction;
import com.ab.core.pojo.MyTransaction;
import com.ab.core.pojo.UserMoney;

public class InMemUserMoneyManager implements Runnable {
	
	private Map<Long, UserMoney> postUserIdVsMoney = new HashMap<>();
	private Map<Long, List<MoneyTransaction>> userIdVsPendingTransactions = new HashMap<>();
	
	private static final Logger logger = LogManager.getLogger(InMemUserMoneyManager.class);
	
	private static InMemUserMoneyManager instance = null;
	private static int autoCommitInterval = 30;
	
	
	private InMemUserMoneyManager() {
	}
	
	public static InMemUserMoneyManager getInstance() {
		if (instance == null) {
			logger.debug("In InMemUserMoneyManager getInstance() method called");
			instance = new InMemUserMoneyManager();
			
			LazyScheduler.getInstance().submitRepeatedTask(instance, autoCommitInterval * 1000, 
					autoCommitInterval * 1000, TimeUnit.MILLISECONDS);
		}
		return instance;
	}
	
	public boolean hasInMemRecords(long userId) {
		return userIdVsPendingTransactions.containsKey(userId);
	}
	
	private void clearStates() {
		postUserIdVsMoney.clear();
		userIdVsPendingTransactions.clear();
	}
	
	private void updateUserMoneyRecords() throws SQLException {
		
		List<MoneyTransaction> fullPendingTransactions = new ArrayList<>();
		Collection<List<MoneyTransaction>> allUsersTransactions = userIdVsPendingTransactions.values();
		
		for (List<MoneyTransaction> userWiseTrans : allUsersTransactions) {
			fullPendingTransactions.addAll(userWiseTrans);
		}
		
		/*List<MoneyTransaction> loadedTransactions = new ArrayList<>();
		List<MoneyTransaction> winningTransactions = new ArrayList<>();
		List<MoneyTransaction> referralTransactions = new ArrayList<>();
		
		for (MoneyTransaction moneyTransaction : fullPendingTransactions) {
			
			UserMoneyAccountType userAccountType = moneyTransaction.getAccountType();
			switch(userAccountType) {
				case LOADED_MONEY: {
					loadedTransactions.add(moneyTransaction);
					break;
				}
				case WINNING_MONEY: {
					winningTransactions.add(moneyTransaction);
					break;
				}
				case REFERAL_MONEY: {
					referralTransactions.add(moneyTransaction);
					break;
				}
			}
		}*/
		try {
			if (fullPendingTransactions.size() > 0) {
				logger.info("All money related transactions size {}", fullPendingTransactions.size());
				bulkUpdate(fullPendingTransactions, UserMoneyDBHandler.UPDATE_BALANCE_AMOUNT_BY_USER_ID);	
			}
			/*if (winningTransactions.size() > 0) {
				logger.info("Winning money related transactions size {}", winningTransactions.size());
				bulkUpdate(winningTransactions, UserMoneyDBHandler.UPDATE_BALANCE_AMOUNT_BY_USER_ID);
			}
			if (referralTransactions.size() > 0) {
				logger.info("Referal money related transactions size {}", referralTransactions.size());
				bulkUpdate(referralTransactions, UserMoneyDBHandler.UPDATE_BALANCE_AMOUNT_BY_USER_ID);
			}*/
		} catch(SQLException ex) {
			logger.error("Error processing buld update for user money objects", ex);
			throw ex;
		}
		clearStates();
	}
	
	private void bulkUpdate(List<MoneyTransaction> list, String sqlQry) throws SQLException {
		
		ConnectionPool cp = null;
		Connection dbConn = null;
		
		PreparedStatement ps = null;
		
		try {
			cp = ConnectionPool.getInstance();
			dbConn = cp.getDBConnection();
			
			ps = dbConn.prepareStatement(sqlQry);
			int index = 0;
			
			List<Integer> resultsList = new ArrayList<>();
			
			for (MoneyTransaction moneyTransaction : list) {
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
			if (index > 0) {
				int [] results = ps.executeBatch();
				dbConn.setAutoCommit(false);
				dbConn.commit();
				dbConn.setAutoCommit(true);
				for (int result : results) {
					resultsList.add(result);
				}
			}
			logger.info("Total records size and results size {} : {}", list.size(), resultsList.size());
			
			if (dbConn != null) {
				dbConn.close();
			}
			
			int size = list.size();
			int operResult = 0;
			for (int counter = 0; counter < size; counter ++) {
				operResult = 0;
				MoneyTransaction transaction = list.get(counter);
				if (resultsList.get(counter) > 0) {
					operResult = 1;
				}
				transaction.getTransaction().setOperResult(operResult);
			}
			
			List<MyTransaction> transactionsList = new ArrayList<>();
			for (MoneyTransaction moneyTran : list) {
				transactionsList.add(moneyTran.getTransaction());
			}
			
			MyTransactionDBHandler.getInstance().createTransactionsInBatch(transactionsList, 200);
			
		} catch(SQLException ex) {
			logger.error("Error in bulk update", ex);
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
	
	public void update(List<MoneyTransaction> moneyTransactions, Map<Long, UserMoney> committedObjects) {
		
		synchronized (LockObject.getInstance().getLockObject()) {
			
			if ((moneyTransactions != null) && (moneyTransactions.size() == 0)) {
				return;
			}

			List<Long> userIds = new ArrayList<>();
			Map<Long, List<MoneyTransaction>> userIdVsCurrentTransactions = new HashMap<>();

			for (MoneyTransaction transaction : moneyTransactions) {

				long userId = transaction.getUserProfileId();
				if (!userIds.contains(userId)) {
					userIds.add(userId);
				}

				List<MoneyTransaction> userPendingTrans = userIdVsPendingTransactions.get(userId);
				if (userPendingTrans == null) {
					userPendingTrans = new ArrayList<>();
				}
				userPendingTrans.add(transaction);
				userIdVsPendingTransactions.put(userId, userPendingTrans);

				List<MoneyTransaction> userCurrentTrans = userIdVsCurrentTransactions.get(userId);
				if (userCurrentTrans == null) {
					userCurrentTrans = new ArrayList<>();
				}
				userCurrentTrans.add(transaction);
				userIdVsCurrentTransactions.put(userId, userCurrentTrans);
			}

			for (Long userId : userIds) {

				UserMoney userMoney = null;
				if (committedObjects != null) {
					userMoney = committedObjects.get(userId);
				}
				try {
					if (userMoney == null) {
						userMoney = getUserMoneyById(userId);
					}
				} catch (SQLException ex) {
					logger.error("User Money object not found for {}", userId);
					logger.error("User Money object not found in In Mem trasactions", ex);
					continue;
				}

				long userLoadedAmount = userMoney.getAmount();

				List<MoneyTransaction> userCuurentTrans = userIdVsCurrentTransactions.get(userId);

				for (MoneyTransaction moneyTran : userCuurentTrans) {

					UserMoneyAccountType userAccountType = moneyTran.getAccountType();
					switch (userAccountType) {
					case LOADED_MONEY: {
						if (moneyTran.getOperType() == UserMoneyOperType.ADD) {
							userLoadedAmount = userLoadedAmount + moneyTran.getAmount();
						} else {
							userLoadedAmount = userLoadedAmount - moneyTran.getAmount();
						}
						break;
					}
					case WINNING_MONEY: {
						if (moneyTran.getOperType() == UserMoneyOperType.ADD) {
							userLoadedAmount = userLoadedAmount + moneyTran.getAmount();
						} else {
							userLoadedAmount = userLoadedAmount - moneyTran.getAmount();
						}
						break;
					}
					case REFERAL_MONEY: {
						if (moneyTran.getOperType() == UserMoneyOperType.ADD) {
							userLoadedAmount = userLoadedAmount + moneyTran.getAmount();
						} else {
							userLoadedAmount = userLoadedAmount - moneyTran.getAmount();
						}
						break;
					}
					}
				}

				userMoney.setAmount(userLoadedAmount);
				postUserIdVsMoney.put(userId, userMoney);
			}
		}
	}
	
	public UserMoney getUserMoneyById(long userId) throws SQLException {
		synchronized (LockObject.getInstance().getLockObject()) {
			UserMoney userMoney = postUserIdVsMoney.get(userId);
			if (userMoney != null) {
				return userMoney;
			}
			return UserMoneyDBHandler.getInstance().getUserMoneyById(userId);
		}
	}
	
	public void commitNow() throws SQLException {
		synchronized (LockObject.getInstance().getLockObject()) {
			updateUserMoneyRecords();
		}
	}
	
	public void run() {
		//logger.info("This is in In Mem User Money Module in periodic run");
		try {
			commitNow();
		} catch (Exception ex) {
			logger.error("Exception in periodic user money objects commit", ex);
		}
	}
	
	public Map<Long, UserMoney> getInMemUserMoneyObjects() {
		return postUserIdVsMoney; 
	}
}
