package com.ab.core.helper;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class SingleThreadAddTransactions {
	
	private ExecutorService singleThreadExecutor = Executors.newSingleThreadExecutor();
	
	private static SingleThreadAddTransactions instance = null;
	
	private SingleThreadAddTransactions() {
	}
	
	public static SingleThreadAddTransactions getInstance() {
		if (instance == null) {
			instance = new SingleThreadAddTransactions();
		}
		return instance;
	}
	
	public void submit(Runnable run) {
		singleThreadExecutor.submit(run);
	}
	
	public void shutDown() {
		singleThreadExecutor.shutdown();
		try {
			singleThreadExecutor.awaitTermination(60 * 1000, TimeUnit.MILLISECONDS);
		} catch (InterruptedException ex) {
			ex.printStackTrace();
		}
	}
}
