package com.ab.core.pojo;

public class UserAccumulatedResults {
	private long id;
	private long uid;
	private int yearIndex;
	private long winAmount;
	private long referAmount;
	
	public long getId() {
		return id;
	}
	public void setId(long id) {
		this.id = id;
	}
	public long getUid() {
		return uid;
	}
	public void setUid(long uid) {
		this.uid = uid;
	}
	public int getYearIndex() {
		return yearIndex;
	}
	public void setYearIndex(int yearIndex) {
		this.yearIndex = yearIndex;
	}
	public long getWinAmount() {
		return winAmount;
	}
	public void setWinAmount(long winAmount) {
		this.winAmount = winAmount;
	}
	public long getReferAmount() {
		return referAmount;
	}
	public void setReferAmount(long referAmount) {
		this.referAmount = referAmount;
	}
}
