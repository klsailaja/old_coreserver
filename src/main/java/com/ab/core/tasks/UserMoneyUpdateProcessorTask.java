package com.ab.core.tasks;

import java.sql.SQLException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.ab.core.helper.MoneyUpdater;
import com.ab.core.pojo.UsersCompleteMoneyDetails;

public class UserMoneyUpdateProcessorTask implements Runnable {
	private static final Logger logger = LogManager.getLogger(UserMoneyUpdateProcessorTask.class);
	private UsersCompleteMoneyDetails usersCompleteDetails;
	
	public UserMoneyUpdateProcessorTask(UsersCompleteMoneyDetails usersCompleteDetails) {
		this.usersCompleteDetails = usersCompleteDetails;
	}

	@Override
	public void run() {
		try {
			MoneyUpdater.getInstance().performTransactions(usersCompleteDetails);
		} catch (SQLException e) {
			logger.error("Error executing the UserMoneyUpdateProcessorTask", e);
		}
	}
}
