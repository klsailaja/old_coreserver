package com.ab.core.constants;

public enum UserMoneyAccountType {
	LOADED_MONEY (1), WINNING_MONEY (2), REFERAL_MONEY (3);
	
	private final int id;
	
	private UserMoneyAccountType(int id){
		this.id = id;
	}
	
	public static UserMoneyAccountType findById(int id){
		for(UserMoneyAccountType accountType : UserMoneyAccountType.values()){
			if(accountType.id == id) return accountType;
		}
		return null;
	}

	public int getId() {
		return id;
	}
}









