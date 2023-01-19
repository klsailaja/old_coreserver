package com.ab.core.constants;

public enum MoneyCreditStatus {
	IN_PROGRESS (0), ALL_SUCCESS(1), ALL_FAIL(2), PARTIAL_RESULTS(3);
	
	private final int id;
	
	private MoneyCreditStatus(int id){
		this.id = id;
	}
	
	public static MoneyCreditStatus findById(int id){
		for(MoneyCreditStatus type : MoneyCreditStatus.values()){
			if(type.id == id) return type;
		}
		return null;
	}

	public int getId() {
		return id;
	}
}

