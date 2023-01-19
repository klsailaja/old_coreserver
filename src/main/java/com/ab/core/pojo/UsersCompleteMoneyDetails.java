package com.ab.core.pojo;

import java.util.List;

public class UsersCompleteMoneyDetails {
	private int requestId;
	private int serverId;
	private int operationType; // 0 - Add Money, 1 - Win Money, 2 - Cancel games refund
	private boolean checkMoney;
	private List<MoneyTransaction> usersMoneyTransactionList;
	private int kycDocsStatus;  // 1 means approved
	private String logTag;
	
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
	public String getLogTag() {
		return logTag;
	}
	public void setLogTag(String logTag) {
		this.logTag = logTag;
	}
	
	public int getOperationType() {
		return operationType;
	}
	public void setOperationType(int operationType) {
		this.operationType = operationType;
	}
}
