package com.ab.core.constants;

public class QuizConstants {
	
	public static final String ERROR_PREFIX_START = "ERROR_START:***********************";
	
	public static final String ERROR_PREFIX_END = "ERROR_END:***********************";
	
	public static final long MAX_USERS_COUNT = 20000000;
	
	public static final int SPECIAL_CODE_MAX_COUNT = 5000;
	
	public static final int CURRENT_SERVERS_COUNT = 30;
	
	public static long MAX_USERS_PER_SERVER = 500000;
	
	public static int LOGGED_IN_USERS_COUNT_UPDATE_TIME_INTERVAL = 30;
	
	public static long LOGGED_IN_USERS_COUNT_UPDATE_TIME_INTERVAL_IN_MILLIS = LOGGED_IN_USERS_COUNT_UPDATE_TIME_INTERVAL * 60 * 1000;
	
	public static final int MAX_BALANCE_ALLOWED = 200000;
	
	public static int TESTMODE = 1;
	
	// Mail properties..
	
	public static final String FROM_MAIL_ID = "satyahasini25@gmail.com";
	
	public static final String VERIFY_MAIL_ID_SUBJECT = "4-digit Verification Code";
	
	public static final String VERIFY_MAIL_ID_SENDER_NAME = "TeluguQuiz";
	
	public static final String VERIFY_MAIL_ID_BODY = "Your 4-digit verification code : %s";
	
	public static final String VERIFY_MAIL_CONTENTS = "<html><p>Your 4-digit verification code : <b>%s</b>" 
				+ "<br><br>Thanks<br>"
				+ VERIFY_MAIL_ID_SENDER_NAME
				+ "</p></html>";
	
	public static final String FORGOT_MAIL_SUBJECT = "Password Reset";
	
	public static final String FORGOT_MAIL_CONTENTS = "<html><p><b>Your password has been reset as requested</b>. New password is: <b>%s</b>."
				+ "If not reset by you, please change the password using ChangePassword Option in My Profile<br><br>Thanks<br>" 
				+ VERIFY_MAIL_ID_SENDER_NAME
				+ "</p></html>";
}
