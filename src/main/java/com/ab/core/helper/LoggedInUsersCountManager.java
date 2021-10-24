package com.ab.core.helper;

import java.util.HashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.ab.core.tasks.LoggedInUsersCountTask;

public class LoggedInUsersCountManager {
	
	private static LoggedInUsersCountManager instance = null;
	private static final Logger logger = LogManager.getLogger(LoggedInUsersCountManager.class);
	private HashMap<Integer,LoggedInUsersCountTask> serverIdVsTask = new HashMap<>();
	
	private LoggedInUsersCountManager() {
	}
	
	public static LoggedInUsersCountManager getInstance() {
		if (instance == null) {
			logger.debug("In LoggedInUsersCountManager getInstance() method instance created");
			instance = new LoggedInUsersCountManager();
		}
		return instance;
	}
	
	public LoggedInUsersCountTask createIfDoesNotExist(int serverId) {
		LoggedInUsersCountTask serverTask = serverIdVsTask.get(serverId);
		if (serverTask == null) {
			serverTask = new LoggedInUsersCountTask(serverId);
			serverIdVsTask.put(serverId, serverTask);
		}
		return serverTask;
	}
}
