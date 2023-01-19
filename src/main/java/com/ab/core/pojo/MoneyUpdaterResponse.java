package com.ab.core.pojo;

import java.util.List;

public class MoneyUpdaterResponse {
	private String trackKey;
	private int operationType;
	private int overallStatus;
	private List<Integer> transactionsIdSet;
	private List<Integer> dbResults;
	
	public String getTrackKey() {
		return trackKey;
	}
	public void setTrackKey(String trackKey) {
		this.trackKey = trackKey;
	}
	public int getOperationType() {
		return operationType;
	}
	public void setOperationType(int operationType) {
		this.operationType = operationType;
	}
	public int getOverallStatus() {
		return overallStatus;
	}
	public void setOverallStatus(int overallStatus) {
		this.overallStatus = overallStatus;
	}
	public List<Integer> getTransactionsIdSet() {
		return transactionsIdSet;
	}
	public void setTransactionsIdSet(List<Integer> transactionsIdSet) {
		this.transactionsIdSet = transactionsIdSet;
	}
	public List<Integer> getDbResults() {
		return dbResults;
	}
	public void setDbResults(List<Integer> dbResults) {
		this.dbResults = dbResults;
	}
}
