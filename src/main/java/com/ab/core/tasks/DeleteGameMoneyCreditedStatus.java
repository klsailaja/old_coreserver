package com.ab.core.tasks;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.ab.core.helper.WinnersMoneyUpdateStatus;

public class DeleteGameMoneyCreditedStatus implements Runnable {
	
	private static final Logger logger = LogManager.getLogger(DeleteGameMoneyCreditedStatus.class);
	
	public DeleteGameMoneyCreditedStatus() {
	}
	
	@Override
	public void run() {
		try {
			WinnersMoneyUpdateStatus.getInstance().cleanupOldEntries();
		}
		catch(Exception ex) {
			logger.error("Exception while deleting the old winners money update records", ex);
		}
	}
}
