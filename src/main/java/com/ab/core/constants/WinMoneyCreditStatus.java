package com.ab.core.constants;

public enum WinMoneyCreditStatus {
	IN_PROGRESS (0), ALL_SUCCESS(1), ALL_FAIL(2), PARTIAL_RESULTS(3);
	
	private final int id;
	
	private WinMoneyCreditStatus(int id){
		this.id = id;
	}
	
	public static WinMoneyCreditStatus findById(int id){
		for(WinMoneyCreditStatus type : WinMoneyCreditStatus.values()){
			if(type.id == id) return type;
		}
		return null;
	}

	public int getId() {
		return id;
	}
}
