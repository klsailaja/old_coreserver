package com.ab.core.pojo;

public class WithdrawMoney {
	private long uid;
	private int wdAmt;
	private int wdType;
	private int wdCoinCount;
	private MyTransaction transaction;
	
	public int getWdCoinCount() {
		return wdCoinCount;
	}
	public void setWdCoinCount(int wdCoinCount) {
		this.wdCoinCount = wdCoinCount;
	}

	public long getUid() {
		return uid;
	}
	public void setUid(long uid) {
		this.uid = uid;
	}
	public int getWdAmt() {
		return wdAmt;
	}
	public void setWdAmt(int wdAmt) {
		this.wdAmt = wdAmt;
	}
	public int getWdType() {
		return wdType;
	}
	public void setWdType(int wdType) {
		this.wdType = wdType;
	}
	public MyTransaction getTransaction() {
		return transaction;
	}
	public void setTransaction(MyTransaction transaction) {
		this.transaction = transaction;
	} 
}
