package com.ab.core.tasks;

import java.sql.SQLException;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.ab.core.db.MyTransactionDBHandler;
import com.ab.core.pojo.MyTransaction;

public class AddTransactionsTask implements Runnable {
	
	private List<MyTransaction> transactionsList;
	private static final Logger logger = LogManager.getLogger(AddTransactionsTask.class);
	
	public AddTransactionsTask(List<MyTransaction> transactionsList) {
		this.transactionsList = transactionsList;
	}
	
	@Override
	public void run() {
		if (transactionsList.size() == 0) {
			return;
		}
		try {
			MyTransactionDBHandler.getInstance().createTransactionsInBatch(transactionsList, 10);
		} catch (SQLException e) {
			logger.error("SQLException in adding the Transactions List", e);
		}
	}
}
