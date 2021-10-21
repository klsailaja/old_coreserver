package com.ab.core.pojo;

import java.util.List;

public class ReferalDetails {
	private int total;
	private boolean isPrevEnabled;
	private boolean isNextEnabled;
	private List<UserReferal> referalList;
	
	
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
	public List<UserReferal> getReferalList() {
		return referalList;
	}
	public void setReferalList(List<UserReferal> referalList) {
		this.referalList = referalList;
	}
}
