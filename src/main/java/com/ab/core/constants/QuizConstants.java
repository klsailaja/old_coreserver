package com.ab.core.constants;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class QuizConstants {
	
	private static final String OS_ROOT = "D:";
	
	private static final String CORE_SERVER_PROP_FILE = OS_ROOT + File.separator + "QuizHome" 
			+ File.separator + "Conf" + File.separator + "CoreServer.prop";
	
	public static String ERROR_PREFIX_START;
	
	public static String ERROR_PREFIX_END;
	
	public static long MAX_USERS_COUNT;
	
	public static boolean MONEY_MODE;
	
	public static Integer MONEY_MODE_CONFIG = -1;
	
	public static int SPECIAL_CODE_MAX_COUNT;
	
	public static int CURRENT_SERVERS_COUNT;
	
	public static long MAX_USERS_PER_SERVER;
	
	public static int LOGGED_IN_USERS_COUNT_UPDATE_TIME_INTERVAL;
	
	public static long LOGGED_IN_USERS_COUNT_UPDATE_TIME_INTERVAL_IN_MILLIS;
	
	public static int MAX_BALANCE_ALLOWED;
	
	public static int TESTMODE;
	
	public static boolean MAINTENANCE_MODE;
	
	// Mail properties..
	
	public static String FROM_MAIL_ID;
	
	public static String VERIFY_MAIL_ID_SUBJECT;
	
	public static String VERIFY_MAIL_ID_SENDER_NAME;
	
	public static String VERIFY_MAIL_ID_BODY;
	
	public static String VERIFY_MAIL_CONTENTS;
	
	public static String FORGOT_MAIL_SUBJECT;
	
	public static String FORGOT_MAIL_CONTENTS;
	
	public static final int[] GAMES_RATES_IN_ONE_SLOT_SPECIAL = {0, 25, 50, 75, 100, 150, 25, 50, 75, 100, 150};
	
	public static void initializeProps() {
		try {
			FileInputStream reader = new FileInputStream(CORE_SERVER_PROP_FILE);
			Properties props = new Properties();
			props.load(reader);
			
			ERROR_PREFIX_START = "ERROR_START:***********************";
			ERROR_PREFIX_END = "ERROR_END:***********************";
			
			String value = props.getProperty("MAX_USERS_COUNT", "20000000");
			MAX_USERS_COUNT = Long.parseLong(value);
			
			value = props.getProperty("SPECIAL_CODE_MAX_COUNT", "5000");
			SPECIAL_CODE_MAX_COUNT = Integer.parseInt(value);
			
			value = props.getProperty("CURRENT_SERVERS_COUNT", "31");
			CURRENT_SERVERS_COUNT = Integer.parseInt(value);
			
			value = props.getProperty("MAX_USERS_PER_SERVER", "500000");
			MAX_USERS_PER_SERVER = Long.parseLong(value);
			
			value = props.getProperty("LOGGED_IN_USERS_COUNT_UPDATE_TIME_INTERVAL", "30");
			LOGGED_IN_USERS_COUNT_UPDATE_TIME_INTERVAL = Integer.parseInt(value);
			LOGGED_IN_USERS_COUNT_UPDATE_TIME_INTERVAL_IN_MILLIS = 
					LOGGED_IN_USERS_COUNT_UPDATE_TIME_INTERVAL * 60 * 1000;
			
			
			value = props.getProperty("MAX_BALANCE_ALLOWED", "200000");
			MAX_BALANCE_ALLOWED = Integer.parseInt(value);
			
			value = props.getProperty("TESTMODE", "1");
			TESTMODE = Integer.parseInt(value);
			
			value = props.getProperty("MAINTENANCE_MODE", "0");
			MAINTENANCE_MODE = value.equals("1")? true: false;
			
			value = props.getProperty("MONEY_MODE", "0");
			MONEY_MODE_CONFIG = Integer.parseInt(value); 
			MONEY_MODE = value.equals("1")? true: false; 
			
			FROM_MAIL_ID = props.getProperty("FROM_MAIL_ID", "satyahasini25@gmail.com");
			VERIFY_MAIL_ID_SUBJECT = props.getProperty("VERIFY_MAIL_ID_SUBJECT", "4-digit Verification Code");
			VERIFY_MAIL_ID_SENDER_NAME = props.getProperty("VERIFY_MAIL_ID_SENDER_NAME", "TeluguQuiz");
			VERIFY_MAIL_ID_BODY = props.getProperty("VERIFY_MAIL_ID_BODY", "Your 4-digit verification code : %s");
			VERIFY_MAIL_CONTENTS = "<html><p>Your 4-digit verification code : <b>%s</b>" 
					+ "<br><br>Thanks<br>"
					+ VERIFY_MAIL_ID_SENDER_NAME
					+ "</p></html>";
			
			FORGOT_MAIL_SUBJECT = props.getProperty("FORGOT_MAIL_SUBJECT", "Password Reset");
			FORGOT_MAIL_CONTENTS = "<html><p><b>Your password has been reset as requested</b>. New password is: <b>%s</b>."
					+ "If not reset by you, please change the password using ChangePassword Option in My Profile<br><br>Thanks<br>" 
					+ VERIFY_MAIL_ID_SENDER_NAME
					+ "</p></html>";
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
