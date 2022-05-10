package com.ab.core.db;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.ab.core.common.LazyScheduler;
import com.ab.core.constants.QuizConstants;
import com.ab.core.exceptions.NotAllowedException;
import com.ab.core.handlers.UserProfileHandler;
import com.ab.core.helper.Utils;
import com.ab.core.pojo.Mail;
import com.ab.core.pojo.ReferalDetails;
import com.ab.core.pojo.UserAccumulatedResults;
import com.ab.core.pojo.UserMoney;
import com.ab.core.pojo.UserProfile;
import com.ab.core.pojo.UserReferal;
import com.ab.core.tasks.SendMailTask;


/*
CREATE TABLE USERPROFILE(ID BIGINT UNSIGNED NOT NULL AUTO_INCREMENT, 
		NAME VARCHAR(20) NOT NULL,
		PASSWD VARCHAR(80) NOT NULL, 
		MAILID VARCHAR(320) NOT NULL, 
		MYREFERALID VARCHAR(20), 
		REFERREDID VARCHAR(20), 
		BOSSUSERID BIGINT,
        BOSSNAME VARCHAR(20),
        LOGGEDIN INT,
        FORGOTPASSWD INT,
        CREATEDDATE BIGINT,
        LASTLOGGEDDATE BIGINT, PRIMARY KEY (ID)) ENGINE = INNODB;
        
CREATE INDEX UserProfile_Inx ON USERPROFILE(MAILID);        
DROP INDEX UserProfile_Inx ON USERPROFILE;        
CREATE INDEX UserProfile_Inx ON USERPROFILE(MAILID);
CREATE INDEX UserProfile_Inx1 ON USERPROFILE(REFERREDID);        
DROP INDEX UserProfile_Inx1 ON USERPROFILE;        
CREATE INDEX UserProfile_Inx1 ON USERPROFILE(REFERREDID);
*/

public class UserProfileDBHandler {
	
	private static final Logger logger = LogManager.getLogger(UserProfileDBHandler.class);

	private static String TABLE_NAME = "USERPROFILE";
	private static String ID = "ID";
	private static String NAME = "NAME";
	private static String PASSWD = "PASSWD";
	private static String MAIL_ID = "MAILID";
	private static String MYREFERAL_ID = "MYREFERALID";
	private static String REFERED_ID = "REFERREDID";
	private static String BOSS_USER_ID = "BOSSUSERID";
	private static String BOSS_NAME = "BOSSNAME";
	private static String LOGGEDIN = "LOGGEDIN";
	private static String FORGOTPASSWD = "FORGOTPASSWD";
	private static String CREATEDDATE = "CREATEDDATE";
	private static String LASTLOGGEDDATE = "LASTLOGGEDDATE";
	
	private static UserProfileDBHandler instance = null;
	
	private static final String GET_USER_PROFILE_BY_MAIL_ID = "SELECT * FROM " + TABLE_NAME + " WHERE " 
			+ MAIL_ID + " = ?";
	private static final String GET_USER_PROFILE_BY_ID = "SELECT * FROM " + TABLE_NAME + " WHERE " 
			+ ID + " = ?";
	private static final String GET_USER_PROFILE_BY_REFERAL_CODE = "SELECT * FROM " + TABLE_NAME + " WHERE " 
			+ MYREFERAL_ID + " = ?";
	private static final String GET_MY_REFERALS = "SELECT * FROM " + TABLE_NAME + " WHERE " 
			+ REFERED_ID + " = ? ORDER BY " + ID + " DESC LIMIT ?, 10";
	private static final String GET_TOTAL_COUNT = "SELECT COUNT(*) FROM " + TABLE_NAME + " WHERE "
			+ REFERED_ID + " = ?";
	  
	
	private static final String CREATE_USER_PROFILE = "INSERT INTO " + TABLE_NAME  
			+ "(" + NAME + "," + MAIL_ID + "," + PASSWD + "," + MYREFERAL_ID + "," + REFERED_ID + ","
			+ BOSS_USER_ID + "," + BOSS_NAME + "," + LOGGEDIN + "," + FORGOTPASSWD + ","
			+ CREATEDDATE + "," + LASTLOGGEDDATE + ") VALUES"   
			+ "(?,?,?,?,?,?,?,?,?,?,?)";
	private static final String MAX_USER_PROFILE_ID = "SELECT MAX(ID) FROM " + TABLE_NAME;
	
	private static final String UPDATE_TIME_BY_ID = "UPDATE " + TABLE_NAME + " SET " + LASTLOGGEDDATE + "= ? WHERE " + ID + " = ?";
	private static final String UPDATE_PROFILE_BY_ID = "UPDATE " + TABLE_NAME + " SET " + NAME + "= ? , " 
			+ PASSWD + "= ? ," + FORGOTPASSWD + "= ? " 
			+ "WHERE " + ID + " = ?";
	
	private static final String UPDATE_LOGGED_STATE_ID = "UPDATE " + TABLE_NAME + " SET " + LOGGEDIN + "= ? WHERE " + ID + " = ?";
	
	private static final int LAST_LOGGED_IN_TIME_DIFF = 15;
	private static final long LAST_LOGGED_IN_TIME_DIFF_IN_MILLIS = LAST_LOGGED_IN_TIME_DIFF * 60 * 1000; 
	private static final String GET_LOGGED_IN_USERS_COUNT = "SELECT COUNT(*) FROM " + TABLE_NAME + " WHERE (?" 
			+ "- LASTLOGGEDDATE) <= " + LAST_LOGGED_IN_TIME_DIFF_IN_MILLIS + " AND (" + ID + " >= " + "?) AND (" + ID + " <= " + "?)"; 
	
	
	private static final String SOURCE = "0123456789"; //ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz 
			
	private static final SecureRandom secureRnd = new SecureRandom();
			
		
	private UserProfileDBHandler() {
	}
	
	public static UserProfileDBHandler getInstance() {
		if (instance == null) {
			logger.debug("In getInstance() method instance created");
			instance = new UserProfileDBHandler();
		}
		return instance;
	}
	
	public void testCreatedUserProfileList(List<UserProfile> userProfilesList, int batchSize) throws SQLException {
		
		System.out.println("testCreatedUserProfileList " + userProfilesList.size());
		
		ConnectionPool cp = null;
		Connection dbConn = null;
		PreparedStatement ps = null;
		
		int totalFailureCount = 0;
		int totalSuccessCount = 0;
		
		try {
			cp = ConnectionPool.getInstance();
			dbConn = cp.getDBConnection();
			dbConn.setAutoCommit(false);
			
			ps = dbConn.prepareStatement(CREATE_USER_PROFILE);
			int index = 0;
			
			for (UserProfile userProfile : userProfilesList) {
				ps.setString(1, userProfile.getName());
				ps.setString(2, userProfile.getEmailAddress());
				ps.setString(3, userProfile.getPasswordHash());
				ps.setString(4, userProfile.getMyReferalId());
				ps.setString(5, userProfile.getBossReferredId());
				ps.setLong(6, userProfile.getBossId());
				ps.setString(7, userProfile.getBossName());
				ps.setInt(8, userProfile.getLoggedIn());
				ps.setInt(9, userProfile.getForgotPasswdUsed());
				ps.setLong(10,  userProfile.getCreatedDate());
				ps.setLong(11, userProfile.getLastLoggedDate());
			
				ps.addBatch();
				index++;
				
				if (index % batchSize == 0) {
					int results[] = ps.executeBatch();
					dbConn.setAutoCommit(false);
					dbConn.commit();
					for (int result : results) {
						if (result == 1) {
							++totalSuccessCount;
						} else {
							++totalFailureCount;
						}
					}
				}
			}
			if (index > 0) {
				int results[] = ps.executeBatch();
				dbConn.setAutoCommit(false);
				dbConn.commit();
				for (int result : results) {
					if (result == 1) {
						++totalSuccessCount;
					} else {
						++totalFailureCount;
					}
				}
			}
			logger.info("End of testCreatedUserProfileList with success row count {} : failure row count {}", 
					totalSuccessCount, totalFailureCount);
		} catch(SQLException ex) {
			logger.error("******************************");
			logger.error("Error in creating user profiles list in bulk mode", ex);
			logger.error("******************************");
			throw ex;
		} finally {
			if (ps != null) {
				ps.close();
			}
			if (dbConn != null) {
				dbConn.close();
			}
		}
	}
	
	public UserProfile createUserProfile(UserProfile userProfile) throws SQLException {
		
		logger.info("In createUserProfile for {}", userProfile.getEmailAddress());
		long maxUseId = SpecialDataDBHandler.getInstance().getCurrentMaxId();
		maxUseId = maxUseId + 1;
		int idStrLen = String.valueOf(maxUseId).length();
		int remainingLen = 10 - idStrLen;
		String userName = userProfile.getName().toUpperCase();
		if (userName.length() >= remainingLen) {
			userName = userName.substring(0, remainingLen);
		}
		String userIdStr = userName + Utils.getReferalCodeStrNotion(maxUseId); 
		userProfile.setMyReferalId(userIdStr);
		logger.debug("Max referal id formed is {}", userIdStr);
		
		ConnectionPool cp = null;
		Connection dbConn = null;
		PreparedStatement ps = null;
		ResultSet idRes = null;
		
		try {
			cp = ConnectionPool.getInstance();
			dbConn = cp.getDBConnection();
			ps = dbConn.prepareStatement(CREATE_USER_PROFILE, Statement.RETURN_GENERATED_KEYS);
		
			ps.setString(1, userProfile.getName());
			ps.setString(2, userProfile.getEmailAddress());
			ps.setString(3, userProfile.getPasswordHash());
			ps.setString(4, userProfile.getMyReferalId());
			ps.setString(5, userProfile.getBossReferredId());
			ps.setLong(6, userProfile.getBossId());
			ps.setString(7, userProfile.getBossName());
			ps.setInt(8, userProfile.getLoggedIn());
			ps.setInt(9, userProfile.getForgotPasswdUsed());
			ps.setLong(10,  userProfile.getCreatedDate());
			ps.setLong(11, userProfile.getLastLoggedDate());
		
			int createResult = ps.executeUpdate();
			logger.info("createUserProfile for {} is {}", userProfile.getEmailAddress(), (createResult > 0));
			if (createResult > 0) {
				idRes = ps.getGeneratedKeys();
				if (idRes.next()) {
					long userProfileId = idRes.getLong(1);
					userProfile.setId(userProfileId);
					long initialLoadedMoney = 0;
					if (QuizConstants.TESTMODE == 1) {
						initialLoadedMoney = 100000;
					}
					
					UserMoney userMoneyObject = new UserMoney(userProfileId, initialLoadedMoney, 0, 0, 0);
					UserMoneyDBHandler.getInstance().createUserMoney(userMoneyObject);
					
					UserAccumulatedResults obj = new UserAccumulatedResults();
					obj.setUid(userProfileId);
					obj.setWinAmount(0);
					obj.setReferAmount(0);
					UserAccumulatedResultsDBHandler.getInstance().createInitialEntries(obj, 3);

					
					VerifyUserProfileDBHandler.getInstance().deleteOTPRecord(userProfile.getEmailAddress());
					
					boolean maxCountUpdateResult = SpecialDataDBHandler.getInstance().updateMaxCount(userProfileId);
					logger.info("The result of update the max user id count is {} and {}", userProfileId, maxCountUpdateResult);
				}
			}
		} catch(SQLException ex) {
			logger.error(QuizConstants.ERROR_PREFIX_START);
			logger.error("Error creating user profile", ex);
			logger.error(QuizConstants.ERROR_PREFIX_END);
			throw ex;
		} finally {
			if (idRes != null) {
				idRes.close();
			}
			if (ps != null) {
				ps.close();
			}
			if (dbConn != null) {
				dbConn.close();
			}
		}
		return userProfile;
	}
	
	public long getMaxUserId() throws SQLException {
		
		logger.info("In getMaxUserId() method");
		
		long maxUserId = -1;
		
		ConnectionPool cp = null;
		Connection dbConn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		
		try {
			cp = ConnectionPool.getInstance();
			dbConn = cp.getDBConnection();
			ps = dbConn.prepareStatement(MAX_USER_PROFILE_ID);
			
			rs = ps.executeQuery();
			if (rs != null) {
				if (rs.next()) {
					maxUserId = rs.getLong("MAX(ID)");
				}
			}
		} catch (SQLException ex) {
			logger.error(QuizConstants.ERROR_PREFIX_START);
			logger.error("Exception while getting the getMaxUserId()");
			logger.error("SQL Exception in getMaxUserId()", ex);
			logger.error(QuizConstants.ERROR_PREFIX_END);
			throw ex;
		} finally {
			if (rs != null) {
				rs.close();
			}
			if (ps != null) {
				ps.close();
			}
			if (dbConn != null) {
				dbConn.close();
			}
		}
		logger.info("Returning from getMaxUserId() {}", maxUserId);
		return maxUserId;
	}
	
	private UserProfile getProfile(String sql, String strVal, long longValue) throws SQLException {
		
		ConnectionPool cp = ConnectionPool.getInstance();
		Connection dbConn = cp.getDBConnection();
		PreparedStatement ps = dbConn.prepareStatement(sql);
		ResultSet rs = null;
		
		if (strVal != null) {
			ps.setString(1, strVal);
		}
		if (longValue != -1) {
			ps.setLong(1, longValue);
		}
		
		UserProfile userProfile = new UserProfile();
		
		try {
			rs = ps.executeQuery();
			if (rs != null) {
				if (rs.next()) {
					userProfile.setId(rs.getLong(ID));
					userProfile.setEmailAddress(rs.getString(MAIL_ID));
					userProfile.setPasswordHash(rs.getString(PASSWD));
					userProfile.setName(rs.getString(NAME));
					userProfile.setMyReferalId(rs.getString(MYREFERAL_ID));
					userProfile.setBossReferredId(rs.getString(REFERED_ID));
					userProfile.setBossId(rs.getLong(BOSS_USER_ID));
					userProfile.setBossName(rs.getString(BOSS_NAME));
					userProfile.setLoggedIn(rs.getInt(LOGGEDIN));
					userProfile.setForgotPasswdUsed(rs.getInt(FORGOTPASSWD));
					userProfile.setCreatedDate(rs.getLong(CREATEDDATE));
					userProfile.setLastLoggedDate(rs.getLong(LASTLOGGEDDATE));
				}
			}
		} catch (SQLException ex) {
			throw ex;
		} finally {
			if (rs != null) {
				rs.close();
			}
			if (ps != null) {
				ps.close();
			}
			if (dbConn != null) {
				dbConn.close();
			}
		}
		return userProfile;
	}
	
	public UserProfile getProfileByBossRefaralCode(String bossReferalCode) throws SQLException {
		
		logger.debug("In bossReferalCode {}", bossReferalCode);
		if (bossReferalCode != null) {
			bossReferalCode = bossReferalCode.trim();
		}
		String sql = GET_USER_PROFILE_BY_REFERAL_CODE;
		
		try {
			UserProfile userProfile = getProfile(sql, bossReferalCode, -1);
			return userProfile;
		} catch (SQLException ex) {
			logger.error("SQL Exception in getProfileByReralCode", ex);
			throw ex;
		}
	}
	
	public long getLoggedInUsersCount(long serverRangeStart, long serverRangeEnd) throws SQLException {
		
		String totalSql = GET_LOGGED_IN_USERS_COUNT;
		ConnectionPool cp = ConnectionPool.getInstance();
		Connection dbConn = cp.getDBConnection();
		
		PreparedStatement totalPs = dbConn.prepareStatement(totalSql);
		totalPs.setLong(1, System.currentTimeMillis());
		totalPs.setLong(2, serverRangeStart);
		totalPs.setLong(3, serverRangeEnd);
		logger.info("**************In getLoggedInUsersCount: {}", totalPs.toString());
		
		ResultSet totalRs = null;
		long total = 0;
		
		try {
			totalRs = totalPs.executeQuery();
			if (totalRs != null) {
				if (totalRs.next()) {
					total = totalRs.getInt("COUNT(*)");
				}
			}
			return total;
		} catch (SQLException ex) {
			throw ex;
		} finally {
			if (totalRs != null) {
				totalRs.close();
			}
			if (totalPs != null) {
				totalPs.close();
			}
			if (dbConn != null) {
				dbConn.close();
			}
		}
	}
	
	public ReferalDetails getUserReferals(String myReferalCode, int startRowNumber) throws SQLException {
		
		String sql = GET_MY_REFERALS;
		String totalSql = GET_TOTAL_COUNT;
		
		ConnectionPool cp = ConnectionPool.getInstance();
		Connection dbConn = cp.getDBConnection();
		
		PreparedStatement totalPs = dbConn.prepareStatement(totalSql);
		PreparedStatement ps = dbConn.prepareStatement(sql);
		
		totalPs.setString(1, myReferalCode);
		
		ps.setString(1, myReferalCode);
		ps.setInt(2, startRowNumber);
		
		ReferalDetails referalDetails = new ReferalDetails();
		List<UserReferal> myReferals = new ArrayList<>();
		
		ResultSet totalRs = null;
		ResultSet rs = null;
		
		try {
			totalRs = totalPs.executeQuery();
			if (totalRs != null) {
				if (totalRs.next()) {
					
					int total = totalRs.getInt("COUNT(*)");
					
					referalDetails.setTotal(total);
					
					int lowerRange = startRowNumber + 1;
					int higherRange = startRowNumber + 10;
					
					if (higherRange < total) {
						referalDetails.setNextEnabled(true);
					} else {
						referalDetails.setNextEnabled(false);
					}
					if ((lowerRange - 10) > 0) {
						referalDetails.setPrevEnabled(true);
					} else {
						referalDetails.setPrevEnabled(false);
					}
					
				}
				totalRs.close();
			}
			
			rs = ps.executeQuery();
			if (rs != null) {
				while (rs.next()) {
					UserReferal userReferal = new UserReferal();
					
					userReferal.setSno(++startRowNumber); 
					userReferal.setUserName(rs.getString(NAME));
					userReferal.setLastLoggedDate(rs.getLong(LASTLOGGEDDATE));
					
					myReferals.add(userReferal);
				}
				rs.close();
			}
		} catch (SQLException ex) {
			throw ex;
		} finally {
			if (totalRs != null) {
				totalRs.close();
			}
			if (rs != null) {
				rs.close();
			}
			if (ps != null) {
				ps.close();
			}
			if (dbConn != null) {
				dbConn.close();
			}
		}
		referalDetails.setReferalList(myReferals);
		logger.debug("getUserReferals with total {}", referalDetails.getReferalList().size());
		return referalDetails;
	}
	
	public UserProfile getProfileById(long profileId) throws SQLException {
		
		String sql = GET_USER_PROFILE_BY_ID;
		
		try {
			UserProfile userProfile = getProfile(sql, null, profileId);
			return userProfile;
		} catch (SQLException ex) {
			logger.error(QuizConstants.ERROR_PREFIX_START);
			logger.error("SQL Exception in getProfileById", ex);
			logger.error(QuizConstants.ERROR_PREFIX_END);
			throw ex;
		}
	}
	
	public UserProfile getProfileByMailid(String mailId) throws SQLException {
		String sql = GET_USER_PROFILE_BY_MAIL_ID;
		
		try {
			UserProfile userProfile = getProfile(sql, mailId, -1);
			return userProfile;
		} catch (SQLException ex) {
			logger.error(QuizConstants.ERROR_PREFIX_START);
			logger.error("SQL Exception in getProfileByMailid", ex);
			logger.error(QuizConstants.ERROR_PREFIX_END);
			throw ex;
		}
	}
	
	public boolean updateUserProfileDetails(UserProfile userProfile, boolean fromForgotPasswd) 
			throws SQLException, NotAllowedException {

		String userMailId = userProfile.getEmailAddress().trim();
		logger.debug("This is in updateUserProfileDetails {}", userMailId);
		
		UserProfile dbObject = getProfileByMailid(userMailId);
		if (dbObject.getId() == 0) {
			throw new NotAllowedException(userMailId + " not registered. Please Check");
		}
		
		String userName;
		String passwd = getRandomPasswd(8);
		String passwdHash;
		int forgotPasswordUsed = 0;
		
		if (fromForgotPasswd) {
			userName = dbObject.getName().trim();
			passwdHash = getPasswordHash(passwd);
			forgotPasswordUsed = 1;
			
		} else {
			userName = userProfile.getName();
			passwdHash = userProfile.getPasswordHash();
		}
		
		ConnectionPool cp = ConnectionPool.getInstance();
		Connection dbConn = cp.getDBConnection();
		PreparedStatement ps = dbConn.prepareStatement(UPDATE_PROFILE_BY_ID);
		
		ps.setString(1, userName);
		ps.setString(2, passwdHash);
		ps.setInt(3, forgotPasswordUsed);
		ps.setLong(4, dbObject.getId());
		
		try {
			int resultCount = ps.executeUpdate();
			boolean result = (resultCount > 0);
			logger.info("updateUserProfileDetails result is {}", result);
			if (!result) {
				throw new NotAllowedException("Could not generate password. Please try later");
			}
		}
		catch(SQLException ex) {
			logger.error(QuizConstants.ERROR_PREFIX_START);
			logger.error("Error updating in updateUserProfileDetails", ex);
			logger.error(QuizConstants.ERROR_PREFIX_END);
			throw ex;
		} finally {
			if (ps != null) {
				ps.close();
			}
			if (dbConn != null) {
				dbConn.close();
			}
		}
		
		if (fromForgotPasswd) {
			
			Mail mail = new Mail();
        
			mail.setMailFrom(QuizConstants.FROM_MAIL_ID);
			mail.setMailTo(userProfile.getEmailAddress().trim());
			mail.setMailSubject("Password Reset");
        
			mail.setMailContent("Your password has been reset. Please login with " + passwd 
        		+ " If not reset by you, please change the password using ChangePassword Option in My Profile\n\nThanks\n" + QuizConstants.VERIFY_MAIL_ID_SENDER_NAME);
			
			LazyScheduler.getInstance().submit(new SendMailTask(mail));
		}
		
		return true;
	}
	
	public boolean updateLoggedInState(long id, int loggedInState) throws SQLException {
		
		ConnectionPool cp = ConnectionPool.getInstance();
		Connection dbConn = cp.getDBConnection();
		PreparedStatement ps = dbConn.prepareStatement(UPDATE_LOGGED_STATE_ID);
		
		ps.setLong(1, id);
		ps.setInt(2, loggedInState);
		
		try {
			int resultCount = ps.executeUpdate();
			boolean result = (resultCount > 0);
			logger.info("updateLoggedInState result is {}", result);
			if (!result) {
				logger.error("Could not update loggedInstate for id {} with state {}", id, loggedInState);
			}
			return result;
		}
		catch(SQLException ex) {
			logger.error(QuizConstants.ERROR_PREFIX_START);
			logger.error("Error updating in updateLoggedInState", ex);
			logger.error(QuizConstants.ERROR_PREFIX_END);
			throw ex;
		} finally {
			if (ps != null) {
				ps.close();
			}
			if (dbConn != null) {
				dbConn.close();
			}
		}
	}
	
	public void updateLastLoggedTimeInBulkMode(List<Long> playerIds, int batchSize) throws SQLException {
		
		logger.info("In updateLastLoggedTimeInBulkMode with size {}", playerIds.size());
		
		ConnectionPool cp = ConnectionPool.getInstance();
		Connection dbConn = cp.getDBConnection();
		PreparedStatement ps = null;
		
		int totalFailureCount = 0;
		int totalSuccessCount = 0;
		
		try {
			dbConn.setAutoCommit(false);
			
			ps = dbConn.prepareStatement(UPDATE_TIME_BY_ID);
			
			int index = 0;
			
			for (Long id : playerIds) {
			
				ps.setLong(1, Calendar.getInstance().getTime().getTime());
				ps.setLong(2, id);
				ps.addBatch();
				index++;
				
				if (index == batchSize) {
					index = 0;
					int[] results = ps.executeBatch();
					dbConn.setAutoCommit(false);
					dbConn.commit();
					for (int result : results) {
						if (result == 1) {
							++totalSuccessCount;
						} else {
							++totalFailureCount;
						}
					}
				}
			}
			if (index > 0) {
				int[] results = ps.executeBatch();
				dbConn.setAutoCommit(false);
				dbConn.commit();
				for (int result : results) {
					if (result == 1) {
						++totalSuccessCount;
					} else {
						++totalFailureCount;
					}
				}
			}
			logger.info("End of updateLastLoggedTimeInBulkMode with success row count {} : failure row count {}", 
					totalSuccessCount, totalFailureCount);
			
		} catch(SQLException ex) {
			logger.error(QuizConstants.ERROR_PREFIX_START);
			logger.error("Error processing updateLastLoggedTimeInBulkMode", ex);
			logger.error(QuizConstants.ERROR_PREFIX_END);
			throw ex;
		} finally {
			if (ps != null) {
				ps.close();
			}
			if (dbConn != null) {
				dbConn.close();
			}
		}
	}
	
	public static String getPasswordHash(String password) {
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e1) {
            e1.printStackTrace();
        }
        if (md == null) {
            return null;
        }
        md.update(password.getBytes());
        byte [] byteData = md.digest();
        StringBuilder sb = new StringBuilder();
        for (byte byteDatum : byteData) {
            sb.append(Integer.toString((byteDatum & 0xff) + 0x100, 16).substring(1));
        }
        return sb.toString();
    }
	
	public String getRandomPasswd(int maxLen) {
		StringBuilder sb = new StringBuilder(maxLen); 
		for (int i = 0; i < maxLen; i++) 
			sb.append(SOURCE.charAt(secureRnd.nextInt(SOURCE.length())));
		return sb.toString();
	}
	
	public static void main(String[] args) throws SQLException {
		
		UserProfileDBHandler dbHandler = UserProfileDBHandler.getInstance();
		
		UserMoneyDBHandler userMoneyDBHandler = UserMoneyDBHandler.getInstance();
		long total = 3000;
		boolean batchMode = true;
		long totalRecCount = 0;
		
		List<UserProfile> testProfiles = new ArrayList<>();
		// System Users from 1 - 10
		for (int index = 1; index <= 20; index ++) {
			UserProfile userProfile = new UserProfile();
			userProfile.setEmailAddress("systemuser" + index + "@gmail.com");
			userProfile.setName("Systemuser" + index);
			userProfile.setPasswordHash("25d19baaef913d50b6a8e302909507b8059c6410a1c8d7e1a844d6069732202e");
			userProfile.setBossReferredId("SPECIAL");
			userProfile.setForgotPasswdUsed(0);
			userProfile.setLoggedIn(0);
			userProfile.setCreatedDate(1609861020944L);
			userProfile.setLastLoggedDate(1609861020944L);
			
			int idStrLen = String.valueOf(index).length();
			int remainingLen = 10 - idStrLen;
			String userName = userProfile.getName().toUpperCase();
			if (userName.length() >= remainingLen) {
				userName = userName.substring(0, remainingLen);
			}
			String userIdStr = userName + Utils.getReferalCodeStrNotion(index); 
			userProfile.setMyReferalId(userIdStr);
			
			if (batchMode) {
				testProfiles.add(userProfile);
			} else {
				//dbHandler.createUserProfile(userProfile);
				UserProfileHandler.getInstance().createUserProfile(userProfile);
			}
			totalRecCount++;
		}
		
		UserProfile userProfile = new UserProfile();
		userProfile.setEmailAddress("ggraj.pec@gmail.com");
		userProfile.setName("Rajasekhar");
		userProfile.setPasswordHash("25d19baaef913d50b6a8e302909507b8059c6410a1c8d7e1a844d6069732202e");
		userProfile.setBossReferredId("SPECIAL");
		userProfile.setForgotPasswdUsed(0);
		userProfile.setLoggedIn(0);
		userProfile.setCreatedDate(1609861020944L);
		userProfile.setLastLoggedDate(1609861020944L);
		int idStrLen = String.valueOf(21).length();
		int remainingLen = 10 - idStrLen;
		String userName = userProfile.getName().toUpperCase();
		if (userName.length() >= remainingLen) {
			userName = userName.substring(0, remainingLen);
		}
		String userIdStr = userName + Utils.getReferalCodeStrNotion(21); 
		userProfile.setMyReferalId(userIdStr);
		
		if (batchMode) {
			testProfiles.add(userProfile);
		} else {
			//dbHandler.createUserProfile(userProfile);
			UserProfileHandler.getInstance().createUserProfile(userProfile);
		}
		totalRecCount++;
		
		for (int index = 22; index <= total; index ++) {
			userProfile = new UserProfile();
			userProfile.setEmailAddress("testuser" + index + "@gmail.com");
			userProfile.setName("testuser" + index);
			userProfile.setPasswordHash("25d19baaef913d50b6a8e302909507b8059c6410a1c8d7e1a844d6069732202e");
			
			/*String bossRefId = "SPECIAL";
			String bossName = "NA";
			long bossId = 0;
			
			if (index == 2065) {
				bossRefId = "RAJASEKHCB";
				bossName = "Rajasekhar";
				bossId = 21;
			}*/
			
			int bossIndex = (index - 1); 
			String bossUserName = "testuser" + bossIndex;
			userProfile.setBossName(bossUserName);
			int bossIdStrLen = String.valueOf(bossIndex).length();
			remainingLen = 10 - bossIdStrLen;
			bossUserName = userProfile.getName().toUpperCase();
			if (bossUserName.length() >= remainingLen) {
				bossUserName = bossUserName.substring(0, remainingLen);
			}
			String bossRefId = bossUserName + Utils.getReferalCodeStrNotion(bossIndex);
			
			userProfile.setBossReferredId(bossRefId);
			
			userProfile.setBossId(bossIndex);
			
			userProfile.setForgotPasswdUsed(0);
			userProfile.setLoggedIn(0);
			userProfile.setCreatedDate(1609861020944L);
			userProfile.setLastLoggedDate(1609861020944L);
			idStrLen = String.valueOf(index).length();
			remainingLen = 10 - idStrLen;
			userName = userProfile.getName().toUpperCase();
			if (userName.length() >= remainingLen) {
				userName = userName.substring(0, remainingLen);
			}
			userIdStr = userName + Utils.getReferalCodeStrNotion(index); 
			userProfile.setMyReferalId(userIdStr); 
			
			if (batchMode) {
				testProfiles.add(userProfile);
				if ((testProfiles.size() % 200) == 0) {
					dbHandler.testCreatedUserProfileList(testProfiles, 200);
					testProfiles.clear();
				}
			} else {
				//dbHandler.createUserProfile(userProfile);
				UserProfileHandler.getInstance().createUserProfile(userProfile);
			}
			totalRecCount++;
			if ((totalRecCount % 100) == 0) {
				System.out.println("User Profile Ct :" + totalRecCount);
			}
		}
		if (batchMode) {
			dbHandler.testCreatedUserProfileList(testProfiles, 200);
			SpecialDataDBHandler.getInstance().updateMaxCount(totalRecCount);
		}
		
		List<UserMoney> userMoneys = new ArrayList<>();
		totalRecCount = 0;
		for (int index = 1; index <= total; index ++) {
			UserMoney userMoney = new UserMoney();
			userMoney.setId(index);
			userMoney.setAmount(100000);
			userMoney.setAmtLocked(0);
			
			if (batchMode) {
				userMoneys.add(userMoney);
				if ((userMoneys.size() % 200) == 0) {
					userMoneyDBHandler.testCreateMoneyInBatch(userMoneys, 200);
					userMoneys.clear();
				}
				
			} else {
				//userMoneyDBHandler.createUserMoney(userMoney);
			}
			totalRecCount++;
			if ((totalRecCount % 100) == 0) {
				System.out.println("User Money Ct :" + totalRecCount);
			}
		}
		if (batchMode) {
			userMoneyDBHandler.testCreateMoneyInBatch(userMoneys, 200);
		}
		
		
		List<UserAccumulatedResults> userAccResults = new ArrayList<>();
		totalRecCount = 0;
		for (int index = 1; index <= total; index ++) {
			userAccResults.addAll(UserAccumulatedResultsDBHandler.getInstance().getAllUserEntries(index, 3));
			
			if (batchMode) {
				if ((userAccResults.size() % 200) == 0) {
					UserAccumulatedResultsDBHandler.getInstance().testCreateAccInBatch(userAccResults, 200);
					userAccResults.clear();
				}
			} else {
				//userMoneyDBHandler.createUserMoney(userMoney);
			}
			totalRecCount++;
			if ((totalRecCount % 100) == 0) {
				System.out.println("User Accum Ct :" + totalRecCount);
			}
		}
		if (batchMode) {
			UserAccumulatedResultsDBHandler.getInstance().testCreateAccInBatch(userAccResults, 200);
			userAccResults.clear();
		}
	}
}
