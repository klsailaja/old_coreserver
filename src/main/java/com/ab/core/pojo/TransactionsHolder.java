package com.ab.core.pojo;

import java.util.List;

public class TransactionsHolder {
	private int total;
	private boolean isPrevEnabled;
	private boolean isNextEnabled;
	private List<MyTransaction> transactionsList;
	
	public int getTotal() {
		return total;
	}
	public void setTotal(int total) {
		this.total = total;
	}
	public boolean isPrevEnabled() {
		return isPrevEnabled;
	}
	public void setPrevEnabled(boolean isPrevEnabled) {
		this.isPrevEnabled = isPrevEnabled;
	}
	public boolean isNextEnabled() {
		return isNextEnabled;
	}
	public void setNextEnabled(boolean isNextEnabled) {
		this.isNextEnabled = isNextEnabled;
	}
	public List<MyTransaction> getTransactionsList() {
		return transactionsList;
	}
	public void setTransactionsList(List<MyTransaction> transactionsList) {
		this.transactionsList = transactionsList;
	}
}
