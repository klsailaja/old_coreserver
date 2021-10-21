package com.ab.core.pojo;

import com.ab.core.constants.UserMoneyAccountType;
import com.ab.core.constants.UserMoneyOperType;

public class MoneyTransaction {
	
	private UserMoneyAccountType accountType;
	private UserMoneyOperType operType;
	private long userProfileId;
	private long amount;
	private MyTransaction transaction;
	
	
	public MoneyTransaction() {
		
	}
	
	public MoneyTransaction(UserMoneyAccountType accountType, UserMoneyOperType operType, long userProfileId,
			long amount, MyTransaction transaction) {
		super();
		this.accountType = accountType;
		this.operType = operType;
		this.userProfileId = userProfileId;
		this.amount = amount;
		this.transaction = transaction;
	}
	
	public UserMoneyAccountType getAccountType() {
		return accountType;
	}
	public void setAccountType(UserMoneyAccountType accountType) {
		this.accountType = accountType;
	}
	public UserMoneyOperType getOperType() {
		return operType;
	}
	public void setOperType(UserMoneyOperType operType) {
		this.operType = operType;
	}
	public long getUserProfileId() {
		return userProfileId;
	}
	public void setUserProfileId(long userProfileId) {
		this.userProfileId = userProfileId;
	}
	public long getAmount() {
		return amount;
	}
	public void setAmount(long amount) {
		this.amount = amount;
	}
	public MyTransaction getTransaction() {
		return transaction;
	}
	public void setTransaction(MyTransaction transaction) {
		this.transaction = transaction;
	}
}
