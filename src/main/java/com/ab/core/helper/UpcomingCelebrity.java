package com.ab.core.helper;

import java.util.List;

public class UpcomingCelebrity {
	private String hourVal;
	private String gameStartTime;
	private List<String> celebrityNames;
	
	public String getHourVal() {
		return hourVal;
	}
	public void setHourVal(String hourVal) {
		this.hourVal = hourVal;
	}
	public String getGameStartTime() {
		return gameStartTime;
	}
	public void setGameStartTime(String gameStartTime) {
		this.gameStartTime = gameStartTime;
	}
	public List<String> getCelebrityNames() {
		return celebrityNames;
	}
	public void setCelebrityNames(List<String> celebrityNames) {
		this.celebrityNames = celebrityNames;
	}
	
	@Override
	public String toString() {
		return "UpcomingCelebrity [hourVal=" + hourVal + ", gameStartTime=" + gameStartTime + ", celebrityNames="
				+ celebrityNames + "]";
	}
}

