package com.ab.core.common;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class LazyScheduler {
	
	private ScheduledThreadPoolExecutor scheduler = (ScheduledThreadPoolExecutor) Executors.newScheduledThreadPool(4);
	private static LazyScheduler instance = null;
	
	private LazyScheduler() {
	}
	
	public static LazyScheduler getInstance() {
		if (instance == null) {
			instance = new LazyScheduler();
			((ScheduledThreadPoolExecutor)instance.scheduler).setRemoveOnCancelPolicy(true);
		}
		return instance;
	}
	
	public void submit(Runnable run) {
		scheduler.schedule(run, 100, TimeUnit.MILLISECONDS);
	}
	
	public void submit(Runnable run, long delay, TimeUnit timeUnit) {
		scheduler.schedule(run, delay, timeUnit);
	}
	
	public ScheduledFuture<?> submitRepeatedTask(Runnable run, long initialDelay, long delay, TimeUnit unit) {
		return scheduler.scheduleAtFixedRate(run, initialDelay, delay, unit);
	}
	
	public void shutDown() {
		scheduler.shutdown();
		try {
			scheduler.awaitTermination(60 * 1000, TimeUnit.MILLISECONDS);
		} catch (InterruptedException ex) {
			ex.printStackTrace();
		}
	}
}
