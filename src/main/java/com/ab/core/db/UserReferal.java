package com.ab.core.db;

public class UserReferal {
	private int sNo;
	private String userName;
	private long lastLoggedDate;
	
	public void setSno(int sNo) {
		this.sNo = sNo;
	}
	public int getSno() {
		return sNo;
	}
	
	public String getUserName() {
		return userName;
	}
	public void setUserName(String userName) {
		this.userName = userName;
	}
	public long getLastLoggedDate() {
		return lastLoggedDate;
	}
	public void setLastLoggedDate(long lastLoggedDate) {
		this.lastLoggedDate = lastLoggedDate;
	}
}
