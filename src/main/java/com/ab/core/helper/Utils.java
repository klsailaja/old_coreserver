package com.ab.core.helper;

import java.security.SecureRandom;
import java.sql.SQLException;
import java.util.List;

import com.ab.core.db.UserProfileDBHandler;
import com.ab.core.pojo.MyTransaction;
import com.ab.core.pojo.UserProfile;

public class Utils {
	
	private static final String SOURCE = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ" 
			+ "abcdefghijklmnopqrstuvwxyz";
	private static final SecureRandom secureRnd = new SecureRandom();
	
	
	/*public static MyTransaction getTransactionPojo(long userId, long date,  
			int amount, int transactionType, int accountType, long ob, long cb, String comments) {
		
		MyTransaction transaction = new MyTransaction();
		transaction.setUserId(userId);
		transaction.setDate(date);
		transaction.setAmount(amount);
		transaction.setAccountType(accountType);
		transaction.setTransactionType(transactionType);
		transaction.setOpeningBalance(ob);
		transaction.setClosingBalance(cb);
		transaction.setComment(comments);
		transaction.setIsWin(0);
		return transaction;
	}*/
	
	public static MyTransaction getTransactionPojo(long userId, long date,  
			int amount, int transactionType, int accountType, long ob, long cb, String comments, String transactionId) {
		
		MyTransaction transaction = new MyTransaction();
		transaction.setUserId(userId);
		transaction.setDate(date);
		transaction.setAmount(amount);
		transaction.setAccountType(accountType);
		transaction.setTransactionType(transactionType);
		transaction.setOpeningBalance(ob);
		transaction.setClosingBalance(cb);
		transaction.setComment(comments);
		transaction.setIsWin(0);
		if (transactionId == null) {
			StringBuilder sb = new StringBuilder(5); 
			for (int i = 0; i < 5; i++) { 
				sb.append(SOURCE.charAt(secureRnd.nextInt(SOURCE.length())));
			}
			transactionId = sb.toString(); 
		}
		transaction.setTransactionId(transactionId);
		return transaction;
	}
	
	public static int getBossMoney(int profit) {
		return (profit/50);
	}
	
	public static void getClosedCircleUserIds(long userId, int maxUsers, List<Long> closedGrpUserIds,
			List<String> closedGrpUserNames, long uidStart, long uidEnd) throws SQLException {
		int index = 0;
		UserProfile userProfile = UserProfileDBHandler.getInstance().getProfileById(userId);
		long bossUserId = userProfile.getBossId();
		
		while ((index < maxUsers) && (bossUserId > 0)) {
			if ((bossUserId < uidStart) || (bossUserId > uidEnd)) {
				break;
			}
			closedGrpUserIds.add(bossUserId);
			userProfile = UserProfileDBHandler.getInstance().getProfileById(bossUserId);
			closedGrpUserNames.add(userProfile.getName());
			index ++;
			bossUserId = userProfile.getBossId();
		}
	}
}
