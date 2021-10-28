package com.ab.core.tasks;

import java.sql.SQLException;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.ab.core.helper.MoneyUpdater;
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
			results = MoneyUpdater.getInstance().performTransactions(usersCompleteDetails);
		} catch (SQLException e) {
			logger.error("Error executing the UserMoneyUpdateProcessorTask", e);
		}
	}
	
	public List<Integer> getMoneyUpdateResults() {
		return results;
	}
}
