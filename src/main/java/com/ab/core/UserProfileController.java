package com.ab.core;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLConnection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.ab.core.common.LazyScheduler;
import com.ab.core.constants.QuizConstants;
import com.ab.core.db.MyTransactionDBHandler;
import com.ab.core.db.UserProfileDBHandler;
import com.ab.core.db.VerifyUserProfileDBHandler;
import com.ab.core.exceptions.InternalException;
import com.ab.core.exceptions.NotAllowedException;
import com.ab.core.handlers.UserProfileHandler;
import com.ab.core.helper.LoggedInUsersCountManager;
import com.ab.core.helper.Utils;
import com.ab.core.helper.WinMsgHandler;
import com.ab.core.pojo.LoginData;
import com.ab.core.pojo.Mail;
import com.ab.core.pojo.OTPDetails;
import com.ab.core.pojo.ReferalDetails;
import com.ab.core.pojo.TransactionsHolder;
import com.ab.core.pojo.UpdateTime;
import com.ab.core.pojo.UserNetwork;
import com.ab.core.pojo.UserProfile;
import com.ab.core.tasks.LoggedInUsersCountTask;
import com.ab.core.tasks.SendMailTask;
import com.ab.core.tasks.UpdateLastLoggedInTimeTask;
import com.ab.core.common.TAGS;

@RestController
public class UserProfileController extends BaseController {
	
	private static final Logger logger = LogManager.getLogger(UserProfileController.class);
	
	private static final String EXTERNAL_FILE_PATH = "D:" + File.separator + "Projects" + File.separator + "Games" +
			File.separator + "terms-and-conditions.pdf";
	
	
	// Completed ...
	@RequestMapping(value = "/loggedin/count/{serverIndex}", 
			method = RequestMethod.GET, produces = "application/json") 
	public @ResponseBody long getLoggedInUserCount(@PathVariable("serverIndex") int serverIndex) 
			throws InternalException {
		logger.debug("{} server index: {} getLoggedInUserCount", TAGS.LOGGED_IN_USER_COUNT, serverIndex);
		LoggedInUsersCountTask task = LoggedInUsersCountManager.getInstance().createIfDoesNotExist(serverIndex);
		long count = task.getUsersCount();
		logger.debug("{} server index: {} getLoggedInUserCount result: {}", 
				TAGS.LOGGED_IN_USER_COUNT, serverIndex, count);
		return count;
	}
	
	// Completed...
	@RequestMapping(value = "/user", method = RequestMethod.POST, produces = "application/json")
	public @ResponseBody UserProfile createUserProfile(@RequestBody UserProfile userProfile) 
			throws NotAllowedException, InternalException {
		
		String userMailId = userProfile.getEmailAddress().trim();
		logger.info("{} User Profile Creation called with {}", TAGS.CREATE_USER, userMailId);
		
		try {
			UserProfile dbUserProfile = UserProfileHandler.getInstance().createUserProfile(userProfile); 
			logger.info("{} createUserProfile returning with {} and {}", TAGS.CREATE_USER,
					dbUserProfile.getEmailAddress(), dbUserProfile.getId());
			ServerDetails serverDetails = getServerDetails(dbUserProfile.getId());
			
			String serverIp = null;
			if (serverDetails.getPort() != -1) {
				serverIp = "http://" + serverDetails.getIpAddress() + ":" + String.valueOf(serverDetails.getPort());
			} else {
				serverIp = serverDetails.getIpAddress();
			}
			dbUserProfile.setServerIpAddress(serverIp);
			dbUserProfile.setServerIndex(serverDetails.getServerIndex());
			
			return dbUserProfile;
		} catch (SQLException ex) {
			logger.error(QuizConstants.ERROR_PREFIX_START);
			logger.error("{} Exception in createUserProfile", TAGS.CREATE_USER);
			logger.error("Exception is: ", ex);
			logger.error(QuizConstants.ERROR_PREFIX_END);
			throw new InternalException("Server Error in createUserProfile");
		}
	}
	
	// Completed...
	@RequestMapping(value = "/user/sendcode", method = RequestMethod.POST, produces = "application/json")
	public @ResponseBody String verifyUserIdGenerateOTP(@RequestBody String eMail) throws NotAllowedException,
		InternalException {
		
		eMail = eMail.trim();
		eMail = eMail.replace("\"", "");
		logger.info("{} This is in verifyUserIdGenerateOTP {}", TAGS.CREATE_USER_SEND_OTP, eMail);
		
		try {
			UserProfile userProfile = UserProfileHandler.getInstance().getUserProfileByMailId(eMail);
			if (userProfile.getId() > 0) {
				throw new NotAllowedException(eMail + " already registered. Use forgot password if required");
			}
			
			 
			OTPDetails otpDBEntry = VerifyUserProfileDBHandler.getInstance().getOTPDetailsByMailId(eMail);
			boolean exists = ((otpDBEntry.getMailId() != null) && otpDBEntry.getMailId().equals(eMail)); 
			
			String passwd = UserProfileDBHandler.getInstance().getRandomPasswd(4);
			String passwdHash = UserProfileDBHandler.getPasswordHash(passwd);
			
			OTPDetails otpDetails = new OTPDetails();
			otpDetails.setMailId(eMail);
			otpDetails.setOtp_hash(passwdHash);
			
			boolean dbAddOrUpdate = false;
			if (!exists) {
				dbAddOrUpdate = VerifyUserProfileDBHandler.getInstance().createUserProfileForVerify(otpDetails);
			} else {
				int updateRowCount = VerifyUserProfileDBHandler.getInstance().updateRecordWithOTP(eMail, passwdHash);
				dbAddOrUpdate = (updateRowCount > 0);
			}
			if (dbAddOrUpdate) {
				
				Mail mail = new Mail();
		        
				mail.setMailFrom(QuizConstants.FROM_MAIL_ID);
				
				mail.setMailTo(eMail);
				mail.setMailSubject(QuizConstants.VERIFY_MAIL_ID_SUBJECT);
				
				String mailContents = String.format(QuizConstants.VERIFY_MAIL_CONTENTS, passwd); 
				mail.setMailContent(mailContents);
				
				LazyScheduler.getInstance().submit(new SendMailTask(mail));
			}
			
			return String.valueOf(true);
		} catch(SQLException ex) {
			logger.error(QuizConstants.ERROR_PREFIX_START);
			logger.error("{} Error in verifyUserIdGenerateOTP for {}", TAGS.CREATE_USER_SEND_OTP, eMail);
			logger.error("Exception is: ", ex);
			logger.error(QuizConstants.ERROR_PREFIX_END);
			throw new InternalException("Server Error in verifyUserIdGenerateOTP");
		}
	}
	
	// Completed...
	@RequestMapping(value = "/user/verify", method = RequestMethod.POST, produces = "application/json")
	public @ResponseBody String verifyOTP(@RequestBody OTPDetails otpDetails)
			throws NotAllowedException, InternalException {
		String eMail = otpDetails.getMailId().trim();
		String passwdHash = otpDetails.getOtp_hash().trim();
		logger.info("{} In verifyOTP for {}", TAGS.CREATE_USER_SEND_OTP, eMail);
		
		try {
			OTPDetails dbEntry = VerifyUserProfileDBHandler.getInstance().getOTPDetailsByMailId(eMail);
			if (dbEntry.getMailId() != null) {
				if (dbEntry.getMailId().equals(eMail)) {
					if (dbEntry.getOtp_hash().equals(passwdHash)) {
						return String.valueOf(true);
					}
				}
			}
			return String.valueOf(false);
		} catch (SQLException ex) {
			logger.error(QuizConstants.ERROR_PREFIX_START);
			logger.error("{} Error while OTP Verify process", TAGS.CREATE_USER_SEND_OTP);
			logger.error("Exception is: ", ex);
			logger.error(QuizConstants.ERROR_PREFIX_END);
			throw new InternalException("Server Error while OTP Verify process");
		}
	}
	
	// Completed ...
	@RequestMapping(value = "/user/{id}", method = RequestMethod.GET, produces = "application/json") 
	public @ResponseBody UserProfile getUserProfileById(@PathVariable("id") long userId) 
			throws InternalException {
		try {
			return UserProfileHandler.getInstance().getUserProfileById(userId);
		} catch (SQLException ex) {
			logger.error(QuizConstants.ERROR_PREFIX_START);
			logger.error("Exception in getUserProfileById", ex);
			logger.error(QuizConstants.ERROR_PREFIX_END);
			throw new InternalException("Server Error in getUserProfileById");
		}
	}
	
	// Completed ...
	@RequestMapping(value = "/user/{email}", method = RequestMethod.GET, produces = "application/json")
	public @ResponseBody UserProfile getUserProfile(@PathVariable("email") String email) 
			throws InternalException {
		try {
			return UserProfileHandler.getInstance().getUserProfileByMailId(email);
		} catch (SQLException ex) {
			logger.error(QuizConstants.ERROR_PREFIX_START);
			logger.error("Exception in getUserProfileByMailId", ex);
			logger.error(QuizConstants.ERROR_PREFIX_END);
			throw new InternalException("Server Error in getUserProfileByMailId");
		}
	}
	
	// Completed ...
	@RequestMapping(value="/user/login", method = RequestMethod.POST, produces = "application/json")
	public @ResponseBody UserProfile login(@RequestBody LoginData loginData) 
			throws NotAllowedException,InternalException {
		logger.info("{} login called with {} ", TAGS.AUTH, loginData.getMailAddress());
		try {
			UserProfile loginResult = UserProfileHandler.getInstance().login(loginData); 
			logger.info("{} login returned with {} : {} : {}", TAGS.AUTH,  
					loginData.getMailAddress(), loginData.getPassword(), loginResult);
			ServerDetails serverDetails = getServerDetails(loginResult.getId());
			
			String serverIp = null;
			if (serverDetails.getPort() != -1) {
				serverIp = "http://" + serverDetails.getIpAddress() + ":" + String.valueOf(serverDetails.getPort());
			} else {
				serverIp = serverDetails.getIpAddress();
			}
			
			loginResult.setServerIpAddress(serverIp);
			loginResult.setServerIndex(serverDetails.getServerIndex());
			loginResult.setMoneyMode(QuizConstants.MONEY_MODE);
			return loginResult;
		} catch(SQLException ex) {
			logger.error(QuizConstants.ERROR_PREFIX_START);
			logger.error("{} Exception in login", TAGS.AUTH);
			logger.error("Exception is: ", ex);
			logger.error(QuizConstants.ERROR_PREFIX_END);
			throw new InternalException("Server Error in login");
		}
	}
	
	@RequestMapping(value="/user/logout/{id}", method = RequestMethod.GET, produces = "application/json")
	public @ResponseBody String logout(@PathVariable("id") long id) throws InternalException {
		try {
			boolean result = UserProfileDBHandler.getInstance().updateLoggedInState(id, 0);
			return String.valueOf(result);
		} catch(SQLException ex) {
			logger.error(QuizConstants.ERROR_PREFIX_START);
			logger.error("Exception in logout", ex);
			logger.error(QuizConstants.ERROR_PREFIX_END);
			throw new InternalException("Server Error in logout");
		}
	}
	
	// Completed.
	@RequestMapping(value = "/forgot", method = RequestMethod.POST, produces = "application/json") 
	public @ResponseBody UserProfile forgotPassword(@RequestBody LoginData loginData) 
			throws NotAllowedException, InternalException {
		
		logger.info("{} forgotPassword is called with {}", TAGS.FORGOT_PASSWD ,loginData.getMailAddress());
		try {
			UserProfile userProfile = new UserProfile();
			userProfile.setEmailAddress(loginData.getMailAddress());
			
			boolean updateResult = UserProfileHandler.getInstance().updateUserProfileDetails(userProfile, true);
			
			if (!updateResult) {
				String errMsg = "Could not reset passwd for : " + userProfile.getEmailAddress().trim() + " in forgot passwd";
				throw new InternalException(errMsg);
			}
			
			UserProfile dbProfile = UserProfileHandler.getInstance().getUserProfileByMailId(userProfile.getEmailAddress());
			logger.info("updateUserProfile returning with {} and {}", dbProfile.getEmailAddress(), dbProfile.getId());
			return dbProfile;
		} catch (SQLException ex) {
			logger.error(QuizConstants.ERROR_PREFIX_START);
			logger.error("{} Exception in forgotPassword", TAGS.FORGOT_PASSWD);
			logger.error("Exception is: ", ex);
			logger.error(QuizConstants.ERROR_PREFIX_END);
			throw new InternalException("Server Error in forgotPassword");
		}
	}
	
	@RequestMapping(value = "/updatetime", method = RequestMethod.POST, produces = "application/json")
	public @ResponseBody String updateLastLoggedInTime(@RequestBody UpdateTime updateLastLoggedInTime) {
		LazyScheduler.getInstance().submit(new UpdateLastLoggedInTimeTask(updateLastLoggedInTime.getUserIds()), 2, TimeUnit.MINUTES);
		return "true";
	}
	
	@RequestMapping(value = "/update", method = RequestMethod.POST, produces = "application/json")
	public @ResponseBody UserProfile updateUserProfile(@RequestBody UserProfile userProfile)
			throws NotAllowedException, InternalException {
		logger.info("{} updateUserProfile is called with {}", TAGS.UPDATE_USER,
				userProfile.getEmailAddress().trim());
		try {
			boolean updateResult = UserProfileHandler.getInstance().updateUserProfileDetails(userProfile, false);
			if (!updateResult) {
				throw new InternalException("Could not update profile contents for " + userProfile.getEmailAddress() + " during update");
			}
			
			UserProfile dbProfile = UserProfileHandler.getInstance().getUserProfileByMailId(userProfile.getEmailAddress());
			logger.info("{} updateUserProfile returning with {} and {}", TAGS.UPDATE_USER, 
					dbProfile.getEmailAddress(), dbProfile.getId());
			return dbProfile;
		} catch (SQLException ex) {
			logger.error(QuizConstants.ERROR_PREFIX_START);
			logger.error("{} Exception in updateUserProfile", TAGS.UPDATE_USER);
			logger.error("Exception is: ", ex);
			logger.error(QuizConstants.ERROR_PREFIX_END);
			throw new InternalException("Server Error in updateUserProfile");
		}
	}
	
	@RequestMapping(value = "/user/mreferal/{myreferalcode}/{pageNum}", method = RequestMethod.GET, produces = "application/json") 
	public @ResponseBody ReferalDetails getUserReferals(@PathVariable("myreferalcode") String referalCode,
			@PathVariable("pageNum") int pageNum) throws InternalException {
		logger.debug("getUserReferals is called with code {} : pageNo {}", referalCode, pageNum);
		try {
			UserProfileHandler profileHandler = UserProfileHandler.getInstance(); 
			ReferalDetails referalDetails = profileHandler.getUserReferals(referalCode, pageNum);
			logger.debug("Referals list size is {} for referal code {}", referalDetails.getReferalList().size(), referalCode);
			return referalDetails;
		} catch (SQLException ex) {
			logger.error(QuizConstants.ERROR_PREFIX_START);
			logger.error("Exception in getUserReferals", ex);
			logger.error(QuizConstants.ERROR_PREFIX_END);
			throw new InternalException("Server Error in getUserReferals");
		}
	}
	
	@RequestMapping(value = "/user/transaction/{userProfileId}/{pageNum}/{accType}", method = RequestMethod.GET,
			produces = "application/json") 
	public @ResponseBody TransactionsHolder getTransactions(@PathVariable("userProfileId") long userProfileId,
			@PathVariable("pageNum") int pageNum, @PathVariable("accType") int accType) throws InternalException, NotAllowedException {
		logger.debug("getTransactions is called with user id {} : pageNo {}", userProfileId, pageNum);
		try {
			UserProfileHandler profileHandler = UserProfileHandler.getInstance();
			TransactionsHolder transactionsDetails = profileHandler.getTransactionsList(userProfileId, pageNum, accType); 
			logger.debug("Transactions list size is {} for user profile id {}", transactionsDetails.getTransactionsList().size(), userProfileId);
			return transactionsDetails;
		} catch (SQLException ex) {
			logger.error(QuizConstants.ERROR_PREFIX_START);
			logger.error("Exception in getTransactionsList", ex);
			logger.error(QuizConstants.ERROR_PREFIX_END);
			throw new InternalException("Server Error in getTransactionsList");
		}
	}
	
	@RequestMapping (value = "/wd/messages", method = RequestMethod.POST, produces = "application/json")
	public @ResponseBody String postGenericWithdrawMessages(@RequestBody List<String> withDrawMsgs) {
		WinMsgHandler.getInstance().setWithdrawMessages(withDrawMsgs);
		return String.valueOf(true);
	}
	
	@RequestMapping(value = "/win/messages", method = RequestMethod.GET, produces = "application/json")
	public @ResponseBody List<String> getGenericWinMessages() {
		return WinMsgHandler.getInstance().getRecentWinMsgs();
	}
	
	/* This method fetches the generic win/wd messages */
	@RequestMapping(value = "/wd/messages/{userId}/{maxCount}", method = RequestMethod.GET, produces = "application/json")
	public @ResponseBody List<String> getRecentWinWDMessages(@PathVariable long userId, @PathVariable int maxCount) 
			throws NotAllowedException, InternalException {
		
		logger.debug("{} In getRecentWinWDMessages with userId {} and {}", TAGS.WIN_WD_MSG_FETCH, userId, maxCount);
		List<String> combinedMsgs = WinMsgHandler.getInstance().getCombinedMessages();
		return combinedMsgs;
	}
	
	@RequestMapping(value = "/user/network/{userId}/{maxCount}", method = RequestMethod.GET, produces = "application/json")
	public @ResponseBody UserNetwork getUserNetworkIds(@PathVariable long userId, @PathVariable int maxCount)
			throws InternalException {
		return getUserFrdDetails(userId, maxCount);
	}
	
	/* This call fetches the win and wd msgs. When userId is valid one, it fetches the closed grp msgs */
	@RequestMapping(value = "/user/win/{userId}/{maxCount}", method = RequestMethod.GET, produces = "application/json")
	public @ResponseBody List<String> getUserFrdsWinMsgs(@PathVariable long userId, @PathVariable int maxCount)
			throws InternalException {
		
		UserNetwork userNetwork = getUserFrdDetails(userId, maxCount);
		
		int frdSize = userNetwork.getClosedUserIdSet().size();
		
		List<String> frdsWinMsgs = new ArrayList<>();
		
		if (frdSize == 0) {
			return frdsWinMsgs;
		}
		
		for (int index = 0; index < frdSize; index++) {
			long frdUid = userNetwork.getClosedUserIdSet().get(index);
			String frdName = userNetwork.getClosedUserNameList().get(index);
			try {
				List<String> winMsgs = MyTransactionDBHandler.
						getInstance().getRecentWinRecords(TAGS.WIN_WD_MSG_FETCH ,frdUid, true, frdName);
				frdsWinMsgs.addAll(winMsgs);
				frdsWinMsgs.add("*");
			} catch(SQLException ex) {
				logger.error(QuizConstants.ERROR_PREFIX_START);
				logger.error("Exception in getUserFrdsWinMsgs", ex);
				logger.error(QuizConstants.ERROR_PREFIX_END);
				continue;
			}
		}
		return frdsWinMsgs;
	}

	@RequestMapping("/terms")
	public void downloadPDFResource(HttpServletRequest request, HttpServletResponse response) 
			throws InternalException {
		logger.info("File exists in downloadPDFResource");
		try {
		
			File file = new File(EXTERNAL_FILE_PATH);
			logger.info(file.getAbsolutePath());
			if (file.exists()) {
				//get the mimetype
				logger.info("File exists");
				String mimeType = URLConnection.guessContentTypeFromName(file.getName());
				if (mimeType == null) {
					//unknown mimetype so set the mimetype to application/octet-stream
					mimeType = "application/octet-stream";
				}
	
				logger.debug("File exists" + mimeType);
				response.setContentType(mimeType);
	
				/**
				 * In a regular HTTP response, the Content-Disposition response header is a
				 * header indicating if the content is expected to be displayed inline in the
				 * browser, that is, as a Web page or as part of a Web page, or as an
				 * attachment, that is downloaded and saved locally.
				 * 
				 */
	
				/**
				 * Here we have mentioned it to show inline
				 */
				response.setHeader("Content-Disposition", String.format("inline; filename=\"" + file.getName() + "\""));
	
				//Here we have mentioned it to show as attachment
				//response.setHeader("Content-Disposition", String.format("attachment; filename=\"" + file.getName() + "\""));
	
				response.setContentLength((int) file.length());
	
				InputStream inputStream = new BufferedInputStream(new FileInputStream(file));
	
				FileCopyUtils.copy(inputStream, response.getOutputStream());
			}
		} catch(IOException ex) {
			//logger.error(QuizConstants.ERROR_PREFIX_START);
			//logger.error("IOException at ", ex);
			//logger.error(QuizConstants.ERROR_PREFIX_END);
		}
	}
	
	private UserNetwork getUserFrdDetails(long userId, int maxCount) throws InternalException {
		try {
			List<Long> closedGroupMembersIds = new ArrayList<>();
			List<String> closedGroupMembersNames = new ArrayList<>();
			
			long index = (userId / QuizConstants.MAX_USERS_PER_SERVER);
			long serverRangeStart =  index * QuizConstants.MAX_USERS_PER_SERVER + 1;
			long serverRangeEnd = (index + 1) * QuizConstants.MAX_USERS_PER_SERVER;
			
			Utils.getClosedCircleUserIds(userId, maxCount, closedGroupMembersIds, closedGroupMembersNames, 
					serverRangeStart, serverRangeEnd);
			
			UserNetwork userNetwork = new UserNetwork();
			userNetwork.setClosedUserIdSet(closedGroupMembersIds);
			userNetwork.setClosedUserNameList(closedGroupMembersNames);
			
			return userNetwork;
		
		} catch (SQLException ex) {
			logger.error(QuizConstants.ERROR_PREFIX_START);
			logger.error("Exception while getting the closed users details ", ex);
			logger.error(QuizConstants.ERROR_PREFIX_END);
			throw new InternalException("Error while gettin the closed users details");
		}
	}
	
	private ServerDetails getServerDetails(long userId) {
		
		ServerDetails serverDetails = new ServerDetails();
		
		String ipAddr = "Backend Problem. Please try after some time";
		int serverPort = -1;
		
		long serverIndex = userId / QuizConstants.MAX_USERS_PER_SERVER;
		
		logger.info("userId is: " + userId + " and server index is :" + serverIndex);
		
		String testIpAddress = "192.168.1.6";
		
		if (serverIndex == 0) {
			ipAddr = testIpAddress;
			serverPort = 8081;
		} else if (serverIndex == 1) {
			ipAddr = testIpAddress;
			serverPort = 8082;
		} else if (serverIndex == 2) {
			ipAddr = testIpAddress;
			serverPort = 8083;
		}
	
		logger.info("ipAddr is: " + ipAddr + " and serverPort is :" + serverPort);
		if (serverPort == -1) {
			logger.error(QuizConstants.ERROR_PREFIX_START);
			logger.error("Please setup a server and assign the ip address");
			logger.error("Current userId is : {} ip is : {} and server port is : {}", userId, ipAddr, serverPort);
			logger.error(QuizConstants.ERROR_PREFIX_END);
			
		}
		
		if ((serverIndex + 1) >= QuizConstants.CURRENT_SERVERS_COUNT) {
			ipAddr = "Max Count Reached. No More Registrations allowed. Please try later";
			serverPort = -1;
		}
		
		if (QuizConstants.MAINTENANCE_MODE) {
			ipAddr = QuizConstants.MAINTENANCE_MODE_MSG;
			serverPort = -1;
		}
		
		serverDetails.setIpAddress(ipAddr);
		serverDetails.setPort(serverPort);
		serverDetails.setServerIndex(serverIndex);
		
		return serverDetails;
	}
}
