package com.ab.core.tasks;

import java.util.Calendar;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.ab.core.db.MyTransactionDBHandler;

public class DeleteOldRecords implements Runnable {
	
	private static final Logger logger = LogManager.getLogger(DeleteOldRecords.class);
	
	public DeleteOldRecords() {
	}

	@Override
	public void run() {
		try {
			int lastFewDays = -30;
			
			Calendar calendar = Calendar.getInstance();
			calendar.add(Calendar.DATE, lastFewDays);
			long time = calendar.getTimeInMillis();
			
			logger.info("Time is {}", time);
			int delCt = MyTransactionDBHandler.getInstance().deleteRecords(time);
			logger.info("Deleted Old Transactions Records size {}", delCt);
		}
		catch(Exception ex) {
			logger.error("Exception while deleting the old records", ex);
		}
	}
	
	public static void main(String[] args) {
		DeleteOldRecords test = new DeleteOldRecords();
		test.run();
	}
}
