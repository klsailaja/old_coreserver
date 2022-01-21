package com.ab.core.pojo;

public class GameSlotMoneyStatus {
	private String serverId;
	private long slotGameStartTime;
	private int moneyCreditedStatus;
	
	public String getServerId() {
		return serverId;
	}
	public void setServerId(String serverId) {
		this.serverId = serverId;
	}
	public long getSlotGameStartTime() {
		return slotGameStartTime;
	}
	public void setSlotGameStartTime(long slotGameStartTime) {
		this.slotGameStartTime = slotGameStartTime;
	}
	public int getMoneyCreditedStatus() {
		return moneyCreditedStatus;
	}
	public void setMoneyCreditedStatus(int moneyCreditedStatus) {
		this.moneyCreditedStatus = moneyCreditedStatus;
	}
}
