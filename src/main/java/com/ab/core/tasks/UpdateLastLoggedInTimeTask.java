package com.ab.core.tasks;

import java.sql.SQLException;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.ab.core.db.UserProfileDBHandler;

public class UpdateLastLoggedInTimeTask implements Runnable {
	private List<Long> userIds;
	private static final Logger logger = LogManager.getLogger(UpdateLastLoggedInTimeTask.class);
	
	public UpdateLastLoggedInTimeTask(List<Long> userIds) {
		this.userIds = userIds;
	}
	
	@Override
	public void run() {
		try {
			UserProfileDBHandler.getInstance().updateLastLoggedTimeInBulkMode(userIds, 100);
		} catch (SQLException e) {
			logger.error("Exception in UpdateLastLoggedInTimeTask", e);
		}
	}
}
