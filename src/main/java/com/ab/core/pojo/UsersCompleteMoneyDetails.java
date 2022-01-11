package com.ab.core.pojo;

import java.util.List;

public class UsersCompleteMoneyDetails {
	private String trackStatusKey;
	private boolean checkMoney;
	private List<MoneyTransaction> usersMoneyTransactionList;
	private int kycDocsStatus;  // 1 means approved
	
	public boolean isCheckMoney() {
		return checkMoney;
	}
	public void setCheckMoney(boolean checkMoney) {
		this.checkMoney = checkMoney;
	}
	
	public List<MoneyTransaction> getUsersMoneyTransactionList() {
		return usersMoneyTransactionList;
	}
	public void setUsersMoneyTransactionList(List<MoneyTransaction> usersMoneyTransactionList) {
		this.usersMoneyTransactionList = usersMoneyTransactionList;
	}
	public int getkycDocsStatus() {
		return kycDocsStatus;
	}
	public void setkycDocsStatus(int kycDocsStatus) {
		this.kycDocsStatus = kycDocsStatus;
	}
	public String getTrackStatusKey() {
		return trackStatusKey;
	}
	public void setTrackStatusKey(String trackStatusKey) {
		this.trackStatusKey = trackStatusKey;
	}
}
