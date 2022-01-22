package com.ab.core.handlers;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.ab.core.common.LazyScheduler;
import com.ab.core.constants.QuizConstants;
import com.ab.core.constants.WithdrawReqState;
import com.ab.core.db.ConnectionPool;
import com.ab.core.db.UserMoneyDBHandler;
import com.ab.core.exceptions.NotAllowedException;
import com.ab.core.helper.SingleThreadMoneyUpdater;
import com.ab.core.helper.WinnersMoneyUpdateStatus;
import com.ab.core.pojo.MoneyTransaction;
import com.ab.core.pojo.MyTransaction;
import com.ab.core.pojo.UserMoney;
import com.ab.core.pojo.UsersCompleteMoneyDetails;
import com.ab.core.pojo.WithdrawMoney;
import com.ab.core.tasks.AddTransactionsTask;
import com.ab.core.tasks.UserMoneyUpdateProcessorTask;

public class UserMoneyHandler {
	
	private static final Logger logger = LogManager.getLogger(UserMoneyHandler.class);
	private static UserMoneyHandler instance = null;
	
	private UserMoneyHandler() {
	}
	
	public static UserMoneyHandler getInstance() {
		if (instance == null) {
			logger.debug("In UserMoneyHandler getInstance() method instance created");
			instance = new UserMoneyHandler();
		}
		return instance;
	}
	
	public UserMoney getUserMoney(long id) throws SQLException, NotAllowedException {
		UserMoney userMoneyDb = UserMoneyDBHandler.getInstance().getUserMoneyById(id);
		return userMoneyDb;
	}
	
	public List<Integer> performUserMoneyOperation(UsersCompleteMoneyDetails usersMoneyDetails) 
			throws NotAllowedException, SQLException {
		
		String trackKey = usersMoneyDetails.getTrackStatusKey();
		logger.info("This is in performUserMoneyOperation with trackKey: {} , money transactions size {}",
				trackKey, usersMoneyDetails.getUsersMoneyTransactionList().size());
		
		boolean checkMoney = usersMoneyDetails.isCheckMoney();
		
		if (checkMoney) {
			MoneyTransaction moneyTransaction = usersMoneyDetails.getUsersMoneyTransactionList().get(0); 
			long userId = moneyTransaction.getUserProfileId();
			UserMoney userMoney = getUserMoney(userId);
			if (userMoney.getId() == 0) {
				throw new NotAllowedException("User Money details not found");
			}
			if (moneyTransaction.getAmount() > userMoney.getAmount()) {
				throw new NotAllowedException("No Enough Cash. Please add money");
			}
			if (userMoney.getAmount() > QuizConstants.MAX_BALANCE_ALLOWED) {
				throw new NotAllowedException("Please raise withdraw request. Amount exceeds max limit.");
			}
		}
		
		UserMoneyUpdateProcessorTask moneyProcessorTask = new UserMoneyUpdateProcessorTask(usersMoneyDetails);
		
		if (trackKey == null) {
			moneyProcessorTask.run();
			return moneyProcessorTask.getMoneyUpdateResults();
		} else {
			WinnersMoneyUpdateStatus.getInstance().createEntry(usersMoneyDetails.getTrackStatusKey());
		}
		
		SingleThreadMoneyUpdater.getInstance().submit(moneyProcessorTask);
		return null;
	}
	
	public boolean performWitdrawOperation(WithdrawMoney wdMoney) throws SQLException {
		
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
}
