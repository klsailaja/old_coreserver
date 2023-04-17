package com.ab.core;

public class ServerDetails {
	private String ipAddress;
	private int port;
	private long serverIndex;
	public long getServerIndex() {
		return serverIndex;
	}

	public void setServerIndex(long serverIndex) {
		this.serverIndex = serverIndex;
	}

	public String getIpAddress() {
		return ipAddress;
	}
	
	public void setIpAddress(String ipAddress) {
		this.ipAddress = ipAddress;
	}
	public int getPort() {
		return port;
	}
	public void setPort(int port) {
		this.port = port;
	}
	
	@Override
	public String toString() {
		return "ServerDetails [ipAddress=" + ipAddress + ", port=" + port + "]";
	}
}
