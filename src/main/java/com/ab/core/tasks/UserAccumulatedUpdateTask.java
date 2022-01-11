package com.ab.core.tasks;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.ab.core.db.UserAccumulatedResultsDBHandler;

public class UserAccumulatedUpdateTask implements Runnable {
	
	private Map<Long, Long> userIdVsWinMoney = new HashMap<>();
	private Map<Long, Long> userIdVsReferMoney = new HashMap<>();
	private static final Logger logger = LogManager.getLogger(UserAccumulatedUpdateTask.class);
	
	public UserAccumulatedUpdateTask(Map<Long, Long> userIdVsWinMoney, Map<Long, Long> userIdVsReferMoney) {
		this.userIdVsWinMoney = userIdVsWinMoney;
		this.userIdVsReferMoney = userIdVsReferMoney;
	}

	@Override
	public void run() {
		try {
			UserAccumulatedResultsDBHandler.getInstance().updateUsersMoneyEntriesInBatch(userIdVsWinMoney, 50, 
					UserAccumulatedResultsDBHandler.UPDATE_WINMONEY_BY_USER_ID, "WIN");
			UserAccumulatedResultsDBHandler.getInstance().updateUsersMoneyEntriesInBatch(userIdVsReferMoney, 50, 
					UserAccumulatedResultsDBHandler.UPDATE_REFERMONEY_BY_USER_ID, "REFER");

		} catch (SQLException e) {
			logger.error("SQLException in updating the WIN and REFER money", e);
		}
	}
}
