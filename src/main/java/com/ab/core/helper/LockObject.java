package com.ab.core.helper;

public class LockObject {
	private static LockObject instance = null;
	private Object lockObj = new Object();
	
	private LockObject() {
	}
	
	public static LockObject getInstance() {
		if (instance == null) {
			instance = new LockObject();
		}
		return instance;
	}
	
	public Object getLockObject() {
		return lockObj;
	}
}
