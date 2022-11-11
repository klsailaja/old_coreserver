package com.ab.core.pojo;

public class MyTransaction {
	
	private long id;
	private String transactionId;
	private long userId;
	private long date;
	private int amount;
	private int accountType;  
	private int transactionType; // Loaded, WithdrawInitiated, WithdrawCompleted, Debited, Credited, TransferedTo, Refunded, TransferedFrom, PaidToReferal
	private int operResult;
	private long openingBalance;
	private long closingBalance;
	private String comments;
	private String extraDetails;
	private int isWin;
	
	private int sNo;
	
	public String getTransactionId() {
		return transactionId;
	}
	public void setTransactionId(String transactionId) {
		this.transactionId = transactionId;
	}
	
	public int getIsWin() {
		return isWin;
	}
	public void setIsWin(int isWin) {
		this.isWin = isWin;
	}
	
	public int getOperResult() {
		return operResult;
	}
	
	public void setOperResult(int operResult) {
		this.operResult = operResult;
	}
	
	public String getComment() {
		return comments;
	}
	public void setComment(String comment) {
		this.comments = comment;
	}
	
	public String getExtraDetails() {
		return extraDetails;
	}
	public void setExtraDetails(String extraDetails) {
		this.extraDetails = extraDetails;
	}
	
	public long getId() {
		return id;
	}
	public void setId(long id) {
		this.id = id;
	}
	public long getUserId() {
		return userId;
	}
	public void setUserId(long userId) {
		this.userId = userId;
	}
	public long getDate() {
		return date;
	}
	public void setDate(long date) {
		this.date = date;
	}
	public int getAmount() {
		return amount;
	}
	public void setAmount(int amount) {
		this.amount = amount;
	}
	public int getAccountType() {
		return accountType;
	}
	public void setAccountType(int accountType) {
		this.accountType = accountType;
	}
	public int getTransactionType() {
		return transactionType;
	}
	public void setTransactionType(int transactionType) {
		this.transactionType = transactionType;
	}

	public long getOpeningBalance() {
		return openingBalance;
	}

	public void setOpeningBalance(long openingBalance) {
		this.openingBalance = openingBalance;
	}

	public long getClosingBalance() {
		return closingBalance;
	}

	public void setClosingBalance(long closingBalance) {
		this.closingBalance = closingBalance;
	}

	public int getsNo() {
		return sNo;
	}

	public void setsNo(int sNo) {
		this.sNo = sNo;
	}
}
