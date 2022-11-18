package com.ab.core.handlers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PaymentProgressCheck {
	private static PaymentProgressCheck instance;
	private Map<Long, Integer> status = new HashMap<>();
	
	private PaymentProgressCheck() {
		
	}
	
	public static PaymentProgressCheck getInstance() {
		if (instance == null) {
			instance = new PaymentProgressCheck();
		}
		return instance;
	}
	
	public synchronized void loadUserIds(List<Long> uids) {
		for (long uid : uids) {
			status.put(uid, 0);
		}
	}
	
	public synchronized void clearAll() {
		status.clear();
	}
	
	public synchronized boolean isWinMoneyPaymentInProgress(long uid) {
		return status.containsKey(uid);
	}

}
