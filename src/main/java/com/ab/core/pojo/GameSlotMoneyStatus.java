package com.ab.core.pojo;

import java.util.List;

public class GameSlotMoneyStatus {
	private String trackKey;
	private String serverId;
	private long slotGameStartTime;
	
	private int operationType;
	private int overallStatus;
	private List<Integer> transactionsIdSet;
	private List<Integer> dbResultsIds;
	
	public String getServerId() {
		return serverId;
	}
	public void setServerId(String serverId) {
		this.serverId = serverId;
	}
	public int getOperationType() {
		return operationType;
	}
	public void setOperationType(int operationType) {
		this.operationType = operationType;
	}
	
	
	public String getTrackKey() {
		return trackKey;
	}
	public void setTrackKey(String trackKey) {
		this.trackKey = trackKey;
	}
	public long getSlotGameStartTime() {
		return slotGameStartTime;
	}
	public void setSlotGameStartTime(long slotGameStartTime) {
		this.slotGameStartTime = slotGameStartTime;
	}
	public int getOverallStatus() {
		return overallStatus;
	}
	public void setOverallStatus(int overallStatus) {
		this.overallStatus = overallStatus;
	}
	
	public void setUniqueIds(List<Integer> uniqueIds) {
		this.transactionsIdSet = uniqueIds;
	}
	public List<Integer> getUniqueIds() {
		return transactionsIdSet;
	}
	
	public void setDbResultsIds(List<Integer> dbResultsIds) {
		this.dbResultsIds = dbResultsIds;
	}
	public List<Integer> getDbResultsIds() {
		return dbResultsIds;
	}
}
