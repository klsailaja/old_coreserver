package com.ab.core.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.ab.core.constants.QuizConstants;
import com.ab.core.pojo.OTPDetails;

/*
 * CREATE TABLE VERIFYUSERPROFILE(MAILID VARCHAR(320) NOT NULL, 
 * OTPCODE VARCHAR(80) NULL, PRIMARY KEY (MAILID)) ENGINE = INNODB;
 */

public class VerifyUserProfile {
	
	private static final Logger logger = LogManager.getLogger(VerifyUserProfile.class);
	
	private static String TABLE_NAME = "VERIFYUSERPROFILE";
	private static String MAILID = "MAILID";
	private static String OTP = "OTPCODE";
	
	private static VerifyUserProfile instance = null;
	
	private static final String CREATE_OTP_PROFILE = "INSERT INTO " + TABLE_NAME   
			+ "(" + MAILID + "," + OTP + ") VALUES"   
			+ "(?,?)";
	private static final String REMOVE_OTP_PRIFILE = "DELETE FROM " + TABLE_NAME 
			+ " WHERE " + MAILID + " = ? ";
	
	private static final String SELECT_OTP_PRIFILE = "SELECT * FROM " + TABLE_NAME 
			+ " WHERE " + MAILID + " = ? ";
	
	private static final String UPDATE_OTP = "UPDATE " + TABLE_NAME + " SET " + OTP + " = ? WHERE " + MAILID + " = ? ";    
			
	private VerifyUserProfile() {
	}
	
	public static VerifyUserProfile getInstance() {
		if (instance == null) {
			logger.debug("In VerifyUserProfile getInstance() method instance created");
			instance = new VerifyUserProfile();
		}
		return instance;
	}
	
	public boolean createUserProfileForVerify(OTPDetails otpDetails) throws SQLException {
		
		logger.debug("In createUserProfileForVerify with {}", otpDetails.getMailId());
		
		ConnectionPool cp = null;
		Connection dbConn = null;
		PreparedStatement ps = null;
		
		try {
			cp = ConnectionPool.getInstance();
			dbConn = cp.getDBConnection();
			ps = dbConn.prepareStatement(CREATE_OTP_PROFILE);
			
			ps.setString(1, otpDetails.getMailId());
			ps.setString(2, otpDetails.getOtp_hash());
			
			int result = ps.executeUpdate();
			logger.info("createUserProfileForVerify with mailid {} result is {}", otpDetails.getMailId(), (result > 0));
			return (result > 0);
			
		} catch(SQLException ex) {
			logger.error(QuizConstants.ERROR_PREFIX_START);
			logger.error("Error in createUserProfileForVerify for mailid {} ", otpDetails.getMailId());
			logger.error("The Exception is", ex);
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
	
	public OTPDetails getOTPDetailsByMailId(String mailId) throws SQLException {
		
		mailId = mailId.trim();
		logger.info("In getOTPDetailsByMailId with {}", mailId);
		
		ConnectionPool cp = ConnectionPool.getInstance();
		Connection dbConn = cp.getDBConnection();
		
		PreparedStatement ps = dbConn.prepareStatement(SELECT_OTP_PRIFILE);
		ps.setString(1, mailId);
		
		ResultSet rs = null;
		
		OTPDetails otpDetails = new OTPDetails();
		
		try {
			rs = ps.executeQuery();
			if (rs != null) {
				if (rs.next()) {
					otpDetails.setMailId(mailId);
					otpDetails.setOtp_hash(rs.getString(OTP));
				}
			}
		} catch (SQLException ex) {
			logger.error(QuizConstants.ERROR_PREFIX_START);
			logger.error("Exception while getting the getOTPDetailsByMailId for mailId {}", mailId);
			logger.error("SQLException in getOTPDetailsByMailId()", ex);
			logger.error("******************************");
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
		return otpDetails;
	}
	
	public int deleteOTPRecord(String mailId) throws SQLException {
		
		logger.info("In deleteOTPRecord method: " + mailId);
		mailId = mailId.trim();
		
		ConnectionPool cp = null;
		Connection dbConn = null;
		PreparedStatement ps = null;
		
		try {
			cp = ConnectionPool.getInstance();
			dbConn = cp.getDBConnection();
			ps = dbConn.prepareStatement(REMOVE_OTP_PRIFILE);
			
			ps.setString(1, mailId);
			
			int result = ps.executeUpdate();
			logger.debug("In deleteOTPRecord operation result : {}", result);
			return result;
		} catch (SQLException ex) {
			logger.error(QuizConstants.ERROR_PREFIX_START);
			logger.error("Error in deleteOTPRecord ", ex);
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
	
	public int updateRecordWithOTP(String mailId, String otpHash) throws SQLException {
		
		mailId = mailId.trim();
		otpHash = otpHash.trim();
		logger.info("In updateRecordWithOTP method" + mailId);
		
		
		ConnectionPool cp = null;
		Connection dbConn = null;
		PreparedStatement ps = null;
		
		try {
			cp = ConnectionPool.getInstance();
			dbConn = cp.getDBConnection();
			ps = dbConn.prepareStatement(UPDATE_OTP);
			
			ps.setString(1, otpHash);
			ps.setString(2, mailId);
			
			int result = ps.executeUpdate();
			logger.debug("In updateRecordWithOTP operation result : {}", result);
			return result;
		} catch (SQLException ex) {
			logger.error(QuizConstants.ERROR_PREFIX_START);
			logger.error("Error in updateRecordWithOTP ", ex);
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
}
