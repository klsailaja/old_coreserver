package com.ab.core.handlers;

import java.sql.SQLException;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.ab.core.constants.QuizConstants;
import com.ab.core.db.UserAccumulatedResultsDBHandler;
import com.ab.core.db.UserMoneyDBHandler;
import com.ab.core.exceptions.NotAllowedException;
import com.ab.core.helper.MoneyUpdater;
import com.ab.core.helper.SingleThreadMoneyUpdater;
import com.ab.core.helper.WinnersMoneyUpdateStatus;
import com.ab.core.pojo.MoneyTransaction;
import com.ab.core.pojo.UserMoney;
import com.ab.core.pojo.UsersCompleteMoneyDetails;
import com.ab.core.pojo.WithdrawMoney;
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
	
	public synchronized List<Integer> performUserMoneyOperation(UsersCompleteMoneyDetails usersMoneyDetails) 
			throws NotAllowedException, SQLException {
		
		logger.info("{} This is in performUserMoneyOperation with serverId: {} and request id: {} , money transactions size {}",
				usersMoneyDetails.getLogTag(), usersMoneyDetails.getServerId(), usersMoneyDetails.getRequestId(), 
				usersMoneyDetails.getUsersMoneyTransactionList().size());
		
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
				throw new NotAllowedException("Not allowed to play. \n Please raise withdraw request as current balance exceeds max limit.");
			}
			long[] userAccumulatedResults = UserAccumulatedResultsDBHandler.getInstance().getAccumulatedResults(userId);
			userMoney.setWinAmount(userAccumulatedResults[0]);
			userMoney.setReferAmount(userAccumulatedResults[1]);
			userMoney.setAddedAmount(userAccumulatedResults[2]);
			userMoney.setWithdrawnAmount(userAccumulatedResults[3]);
			
			long profit = userMoney.getAddedAmount() - (userMoney.getWinAmount() + userMoney.getReferAmount());
			if (!QuizConstants.MONEY_MODE) {
				long profitCoins = profit;
				profit = (profitCoins * 10)/100;
			}
			
			if (profit > 50000) {
				if (usersMoneyDetails.getkycDocsStatus() == 0) {
					throw new NotAllowedException("Not allowed to play. Please complete the KYC Docs Upload process");
				}
			}
		}
		
		UserMoneyUpdateProcessorTask moneyProcessorTask = new UserMoneyUpdateProcessorTask(usersMoneyDetails);
		
		if (usersMoneyDetails.getRequestId() == 0) {
			moneyProcessorTask.run();
			List<Integer> syncCallResults = moneyProcessorTask.getMoneyUpdateResults();
			if (syncCallResults == null) {
				throw new NotAllowedException("Server Busy. Try after 2 minutes");
			} else {
				return syncCallResults;
			}
		} else {
			WinnersMoneyUpdateStatus.getInstance().createEntry(usersMoneyDetails);
		}
		
		SingleThreadMoneyUpdater.getInstance().submit(moneyProcessorTask);
		return null;
	}
	
	public boolean performWitdrawOperation(WithdrawMoney wdMoney) throws SQLException {
		try {
			return MoneyUpdater.getInstance().performWitdrawOperation(wdMoney);
		} catch (Exception ex) {
			throw new NotAllowedException("Server Busy. Try after 2 minutes");
		}
	}

	public boolean addMoney(UsersCompleteMoneyDetails completeDetails) {
		return false;
	}
}
