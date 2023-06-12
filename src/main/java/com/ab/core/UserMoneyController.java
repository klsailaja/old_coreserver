package com.ab.core;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.ab.core.constants.QuizConstants;
import com.ab.core.db.UserAccumulatedResultsDBHandler;
import com.ab.core.exceptions.InternalException;
import com.ab.core.exceptions.NotAllowedException;
import com.ab.core.handlers.UserMoneyHandler;
import com.ab.core.helper.CelebritySpecialHandler;
import com.ab.core.helper.Utils;
import com.ab.core.helper.WinnersMoneyUpdateStatus;
import com.ab.core.pojo.GameSlotMoneyStatus;
import com.ab.core.pojo.UserMoney;
import com.ab.core.pojo.UsersCompleteMoneyDetails;
import com.ab.core.pojo.WithdrawMoney;

@RestController
public class UserMoneyController extends BaseController {
	private static final Logger logger = LogManager.getLogger(UserMoneyController.class);
	// get money
	// add money 
	// Transfer
	
	@RequestMapping(value = "/coincost/{coincount}", method = RequestMethod.GET, produces = "application/json") 
	public @ResponseBody long getCoinsCost(@PathVariable("coincount") long coincount) 
			throws NotAllowedException {
		return Utils.convertCoinsToMoney(coincount);
	}
	
	@RequestMapping(value = "/money/{userProfileId}", method = RequestMethod.GET, produces = "application/json")
	public @ResponseBody UserMoney getUserMoney(@PathVariable("userProfileId") long userProfileId) 
			throws InternalException, NotAllowedException {
		
		try {
			return UserMoneyHandler.getInstance().getUserMoney(userProfileId);
		} catch (SQLException ex) {
			logger.error(QuizConstants.ERROR_PREFIX_START);
			logger.error("Exception in getUserMoney", ex);
			logger.error(QuizConstants.ERROR_PREFIX_END);
			throw new InternalException("Server Error in getUserMoney");
		}
	}
	
	@RequestMapping(value = "/fullmoney/{userProfileId}", method = RequestMethod.GET, produces = "application/json")
	public @ResponseBody UserMoney getFullUserMoney(@PathVariable("userProfileId") long userProfileId) 
			throws InternalException, NotAllowedException {
		try {
			UserMoney userMoney = UserMoneyHandler.getInstance().getUserMoney(userProfileId);
			long[] userAccumulatedResults = UserAccumulatedResultsDBHandler.getInstance().getAccumulatedResults(userProfileId);
			userMoney.setWinAmount(userAccumulatedResults[0]);
			userMoney.setReferAmount(userAccumulatedResults[1]);
			userMoney.setAddedAmount(userAccumulatedResults[2]);
			userMoney.setWithdrawnAmount(userAccumulatedResults[3]);
			return userMoney;
		} catch (Exception ex) {
			logger.error(QuizConstants.ERROR_PREFIX_START);
			logger.error("Exception in getFullMoneyTask", ex);
			logger.error(QuizConstants.ERROR_PREFIX_END);
			throw new InternalException("Server Error in getFullMoneyTask");
		}
	}
	
	@RequestMapping(value = "/money/update", method = RequestMethod.POST, produces = "application/json")
	public @ResponseBody List<Integer> updateGameMoney(@RequestBody UsersCompleteMoneyDetails completeDetails)
			throws InternalException {
		logger.info("{} {} This is in updatePlayedMoney with records size", completeDetails.getLogTag(),
				completeDetails.getUsersMoneyTransactionList().size());
		try {
			return UserMoneyHandler.getInstance().performUserMoneyOperation(completeDetails);
		} catch (SQLException ex) {
			logger.error(QuizConstants.ERROR_PREFIX_START);
			logger.error("{} Exception in updatePlayedMoney", completeDetails.getLogTag());
			logger.error("Exception is: ", ex);
			logger.error(QuizConstants.ERROR_PREFIX_END);
			throw new InternalException("Server Error in updatePlayedMoney");
		}
	}
	
	@RequestMapping(value = "/money/wd", method = RequestMethod.POST, produces = "application/json")
	public @ResponseBody boolean performWithdraw(@RequestBody WithdrawMoney wdMoney) throws InternalException {
		try {
			return UserMoneyHandler.getInstance().performWitdrawOperation(wdMoney);
		} catch (SQLException ex) {
			logger.error(QuizConstants.ERROR_PREFIX_START);
			logger.error("Exception in performWithdraw", ex);
			logger.error(QuizConstants.ERROR_PREFIX_END);
			throw new InternalException("Server Error in performWithdraw");
		}
	}
	
	@RequestMapping(value = "/money/update/{serverId}", method = RequestMethod.GET, produces = "application/json")
	public @ResponseBody List<GameSlotMoneyStatus> getGamesSlotMoneyStatus(@PathVariable("serverId") Integer serverId) 
			throws InternalException {
		return WinnersMoneyUpdateStatus.getInstance().getServerIdStatus(serverId);
	}
	
	@RequestMapping(value = "/money/mode", method = RequestMethod.GET, produces = "application/json")
	public @ResponseBody Integer getMoneyMode() throws InternalException {
		return QuizConstants.MONEY_MODE_CONFIG;
	}
	
	
	@RequestMapping(value = "/upcoming", method = RequestMethod.GET, produces = "application/json") 
	public @ResponseBody List<String> getUpcomingCelebrityNames() {
		
        Calendar calendar = Calendar.getInstance();
        
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        
		int hour1 = hour;
		int hour2 = hour + 1;
		
		List<String> firstSet = CelebritySpecialHandler.getInstance().
				getUniqueCelebrityNames(hour1, QuizConstants.GAMES_RATES_IN_ONE_SLOT_SPECIAL.length);
		
		StringBuilder firstStrBuilder = new StringBuilder(); 
		for (String li : firstSet) {
			firstStrBuilder.append(li);
			firstStrBuilder.append(", ");
		}
		String firstStr = firstStrBuilder.toString(); 
		int lastPos = firstStr.lastIndexOf(",");
		if (lastPos > -1) {
			firstStr = firstStr.substring(0, lastPos).trim();
		}
		
		int startHour = calendar.get(Calendar.HOUR);
		if (startHour == 0) {
			startHour = 12;
		}
		int startTimeAMPM = calendar.get(Calendar.AM_PM);
		String startMeridianText = "AM";
		if (startTimeAMPM == Calendar.PM) {
			startMeridianText = "PM";
		}
		
		calendar.add(Calendar.HOUR_OF_DAY, 1);
		
		int endHour = calendar.get(Calendar.HOUR);
		if (endHour == 0) {
			endHour = 12;
		}
		int endTimeAMPM = calendar.get(Calendar.AM_PM);
		String endMeridianText = "AM";
		if (endTimeAMPM == Calendar.PM) {
			endMeridianText = "PM";
		}
		
		StringBuffer startTimeBuffer = new StringBuffer(firstStr);
		startTimeBuffer.append(" games coming at: ");
		startTimeBuffer.append(startHour);
		startTimeBuffer.append(" ");
		startTimeBuffer.append(startMeridianText);
		startTimeBuffer.append(" - ");
		startTimeBuffer.append(endHour);
		startTimeBuffer.append(" ");
		startTimeBuffer.append(endMeridianText);
		firstStr = startTimeBuffer.toString();
		
		List<String> secondSet = CelebritySpecialHandler.getInstance().
				getUniqueCelebrityNames(hour2, QuizConstants.GAMES_RATES_IN_ONE_SLOT_SPECIAL.length);
		StringBuilder secondStrBuilder = new StringBuilder(); 
		for (String li : secondSet) {
			secondStrBuilder.append(li);
			secondStrBuilder.append(", ");
		}
		String secondStr = secondStrBuilder.toString();
		
		lastPos = secondStr.lastIndexOf(",");
		if (lastPos > -1) {
			secondStr = secondStr.substring(0, lastPos).trim();
		}
		
		startHour = calendar.get(Calendar.HOUR);
		if (startHour == 0) {
			startHour = 12;
		}
		startTimeAMPM = calendar.get(Calendar.AM_PM);
		startMeridianText = "AM";
		if (startTimeAMPM == Calendar.PM) {
			startMeridianText = "PM";
		}
		
		calendar.add(Calendar.HOUR_OF_DAY, 1);
		
		endHour = calendar.get(Calendar.HOUR);
		if (endHour == 0) {
			endHour = 12;
		}
		endTimeAMPM = calendar.get(Calendar.AM_PM);
		endMeridianText = "AM";
		if (endTimeAMPM == Calendar.PM) {
			endMeridianText = "PM";
		}
		
		startTimeBuffer = new StringBuffer(secondStr);
		startTimeBuffer.append(" games coming at: ");
		startTimeBuffer.append(startHour);
		startTimeBuffer.append(" ");
		startTimeBuffer.append(startMeridianText);
		startTimeBuffer.append(" - ");
		startTimeBuffer.append(endHour);
		startTimeBuffer.append(" ");
		startTimeBuffer.append(endMeridianText);
		secondStr = startTimeBuffer.toString();
		
		List<String> results = new ArrayList<>(2);
		results.add(firstStr);
		results.add(secondStr);
		
		return results;
	}
}
