package com.ab.core.tasks;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.ab.core.constants.QuizConstants;
import com.ab.core.db.UserProfileDBHandler;

public class LoggedInUsersCountTask implements Runnable {
	
	private static final Logger logger = LogManager.getLogger(LoggedInUsersCountTask.class);
	private long loggedUsersCount;
	private int serverId;
	
	public LoggedInUsersCountTask(int serverId) {
		this.serverId = serverId;
	}
	
	public void run() {
		try {
			long serverRangeStart = (serverId - 1) * QuizConstants.MAX_USERS_PER_SERVER + 1;
			long serverRangeEnd = (serverId) * QuizConstants.MAX_USERS_PER_SERVER;
			loggedUsersCount = UserProfileDBHandler.getInstance().getLoggedInUsersCount(serverRangeStart, serverRangeEnd);
		} catch (Exception ex) {
			logger.error(QuizConstants.ERROR_PREFIX_START);
			logger.error("Exception in LoggedInUsersCountTask", ex);
			logger.error(QuizConstants.ERROR_PREFIX_END);
		}
	}
	
	public long getUsersCount() {
		if (loggedUsersCount < 50) {
			int userAnswerMin = 100;
			int userAnswerMax = 150;
			int userAnswerFinal = userAnswerMin + (int) (Math.random() * (userAnswerMax - userAnswerMin));
			return userAnswerFinal;
		}
		return loggedUsersCount;
	}
}
