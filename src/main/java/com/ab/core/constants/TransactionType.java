package com.ab.core.constants;

public enum TransactionType {
	CREDITED (1), DEBITED(2), OPEN (3), CANCELLED(4), CLOSED(5); 
	// Loaded, WithdrawInitiated, WithdrawCompleted, Debited, Credited, TransferedTo, Refunded, TransferedFrom, PaidToReferal
	
	private final int id;
	
	private TransactionType(int id){
		this.id = id;
	}
	
	public static TransactionType findById(int id){
		for(TransactionType transactionType : TransactionType.values()){
			if(transactionType.id == id) return transactionType;
		}
		return null;
	}

	public int getId() {
		return id;
	}
}
