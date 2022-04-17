package com.ab.core.tasks;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.ab.core.db.VerifyUserProfileDBHandler;

public class DeleteUselessOTPTask implements Runnable {
	private static final Logger logger = LogManager.getLogger(DeleteUselessOTPTask.class);
	
	public DeleteUselessOTPTask() {
	}

	@Override
	public void run() {
		try {
			int delCt = VerifyUserProfileDBHandler.getInstance().deleteUselessEntries();
			logger.info("Deleted Old Transactions Records size {}", delCt);
		}
		catch(Exception ex) {
			logger.error("Exception while deleting the DeleteUselessOTPTask ", ex);
		}
	}
}
