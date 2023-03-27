package com.ab.core.pojo;

public class CelebrityDetails {
	private String name;
	private int code;
	private int hour;
	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public int getCode() {
		return code;
	}
	public void setCode(int code) {
		this.code = code;
	}
	
	public int getHour() {
		return hour;
	}
	public void setHour(int hour) {
		this.hour = hour;
	}
	@Override
	public String toString() {
		return "CelebrityDetails [name=" + name + ", code=" + code + ", hour=" + hour + "]";
	}
}

