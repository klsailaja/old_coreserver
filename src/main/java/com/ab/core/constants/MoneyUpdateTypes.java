package com.ab.core.constants;

public enum MoneyUpdateTypes {
	WIN_MONEY (1), REFUND_CANCEL_GAMES(2), ADD_MONEY(3), WITHDRAW_MONEY(4);
	
	private final int id;
	
	private MoneyUpdateTypes(int id){
		this.id = id;
	}
	
	public static MoneyUpdateTypes findById(int id){
		for(MoneyUpdateTypes operType : MoneyUpdateTypes.values()){
			if(operType.id == id) return operType;
		}
		return null;
	}

	public int getId() {
		return id;
	}
}
