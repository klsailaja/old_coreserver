package com.ab.core.pojo;

import java.util.List;

public class UserNetwork {
	private List<Long> closedUserIdSet;
	private List<String> closedUserNameList;
	
	public List<Long> getClosedUserIdSet() {
		return closedUserIdSet;
	}
	public void setClosedUserIdSet(List<Long> closedUserIdSet) {
		this.closedUserIdSet = closedUserIdSet;
	}
	public List<String> getClosedUserNameList() {
		return closedUserNameList;
	}
	public void setClosedUserNameList(List<String> closedUserNameList) {
		this.closedUserNameList = closedUserNameList;
	}
}
