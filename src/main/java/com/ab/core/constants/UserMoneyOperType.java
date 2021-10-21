package com.ab.core.constants;

public enum UserMoneyOperType {
	ADD (1), SUBTRACT(2);
	
	private final int id;
	
	private UserMoneyOperType(int id){
		this.id = id;
	}
	
	public static UserMoneyOperType findById(int id){
		for(UserMoneyOperType operType : UserMoneyOperType.values()){
			if(operType.id == id) return operType;
		}
		return null;
	}

	public int getId() {
		return id;
	}
}
