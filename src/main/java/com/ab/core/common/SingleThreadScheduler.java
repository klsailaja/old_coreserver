package com.ab.core.common;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class SingleThreadScheduler {
	
	private ExecutorService singleThreadExecutor = Executors.newSingleThreadExecutor();
	
	private static SingleThreadScheduler instance = null;
	
	private SingleThreadScheduler() {
	}
	
	public static SingleThreadScheduler getInstance() {
		if (instance == null) {
			instance = new SingleThreadScheduler();
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
