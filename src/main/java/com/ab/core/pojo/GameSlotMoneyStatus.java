package com.ab.core.pojo;

import java.util.List;

public class GameSlotMoneyStatus {
	private int requestId;
	private int serverId;
	private int operationType;
	private List<Integer> uniqueIds;
	
	private int moneyCreditedStatus;
	private List<Integer> dbResultsIds;
	private long completedTime;
	
	public long getCompletedTime() {
		return completedTime;
	}
	public void setCompletedTime(long completedTime) {
		this.completedTime = completedTime;
	}
	
	public int getRequestId() {
		return requestId;
	}
	public void setRequestId(int requestId) {
		this.requestId = requestId;
	}
	public int getServerId() {
		return serverId;
	}
	public void setServerId(int serverId) {
		this.serverId = serverId;
	}
	public int getOperationType() {
		return operationType;
	}
	public void setOperationType(int operationType) {
		this.operationType = operationType;
	}
	public int getMoneyCreditedStatus() {
		return moneyCreditedStatus;
	}
	public void setMoneyCreditedStatus(int moneyCreditedStatus) {
		this.moneyCreditedStatus = moneyCreditedStatus;
	}
	public List<Integer> getUniqueIds() {
		return uniqueIds;
	}
	public void setUniqueIds(List<Integer> uniqueIds) {
		this.uniqueIds = uniqueIds;
	}
	public List<Integer> getDbResultsIds() {
		return dbResultsIds;
	}
	public void setDbResultsIds(List<Integer> dbResultsIds) {
		this.dbResultsIds = dbResultsIds;
	}
}
