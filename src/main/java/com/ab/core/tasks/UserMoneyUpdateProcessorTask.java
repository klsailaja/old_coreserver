package com.ab.core.tasks;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.ab.core.constants.QuizConstants;
import com.ab.core.helper.MoneyUpdater;
import com.ab.core.pojo.GameSlotMoneyStatus;
import com.ab.core.pojo.MoneyTransaction;
import com.ab.core.pojo.UsersCompleteMoneyDetails;

public class UserMoneyUpdateProcessorTask implements Runnable {
	private static final Logger logger = LogManager.getLogger(UserMoneyUpdateProcessorTask.class);
	private UsersCompleteMoneyDetails usersCompleteDetails;
	private List<Integer> results;
	
	public UserMoneyUpdateProcessorTask(UsersCompleteMoneyDetails usersCompleteDetails) {
		this.usersCompleteDetails = usersCompleteDetails;
	}

	@Override
	public void run() {
		try {
			List<MoneyTransaction> transactionsList = usersCompleteDetails.getUsersMoneyTransactionList();
			
			List<Long> paymentInProgressUidList = new ArrayList<>();
			for (MoneyTransaction trans : transactionsList) {
				paymentInProgressUidList.add(trans.getUserProfileId());
			}
			//PaymentProgressCheck.getInstance().loadUserIds(paymentInProgressUidList);
			
			GameSlotMoneyStatus response 
				= MoneyUpdater.getInstance().performTransactions(usersCompleteDetails);
			results = response.getDbResultsIds();
			
			//PaymentProgressCheck.getInstance().clearAll();
			
		} catch (SQLException e) {
			logger.error(QuizConstants.ERROR_PREFIX_START);
			logger.error("Error executing the UserMoneyUpdateProcessorTask", e);
			logger.error(QuizConstants.ERROR_PREFIX_END);
		} catch (Exception ex) {
			results = null;
		}
	}
	
	public List<Integer> getMoneyUpdateResults() {
		return results;
	}
}
