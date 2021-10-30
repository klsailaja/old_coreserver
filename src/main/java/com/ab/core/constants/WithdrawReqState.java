package com.ab.core.constants;

public enum WithdrawReqState {
	OPEN (1), CLOSED(2), CANCELLED(3);
	
	private final int id;
	
	private WithdrawReqState(int id){
		this.id = id;
	}
	
	public static WithdrawReqState findById(int id){
		for(WithdrawReqState state : WithdrawReqState.values()){
			if(state.id == id) return state;
		}
		return null;
	}

	public int getId() {
		return id;
	}
}
