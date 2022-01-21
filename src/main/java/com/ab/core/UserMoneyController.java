package com.ab.core;

import java.sql.SQLException;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.ab.core.db.UserAccumulatedResultsDBHandler;
import com.ab.core.exceptions.InternalException;
import com.ab.core.exceptions.NotAllowedException;
import com.ab.core.handlers.UserMoneyHandler;
import com.ab.core.helper.WinnersMoneyUpdateStatus;
import com.ab.core.pojo.SlotGamesWinMoneyStatus;
import com.ab.core.pojo.UserMoney;
import com.ab.core.pojo.UsersCompleteMoneyDetails;
import com.ab.core.pojo.WithdrawMoney;

@RestController
public class UserMoneyController extends BaseController {
	private static final Logger logger = LogManager.getLogger(UserMoneyController.class);
	// get money
	// add money 
	// Transfer
	
	@RequestMapping(value = "/money/{userProfileId}", method = RequestMethod.GET, produces = "application/json")
	public @ResponseBody UserMoney getUserMoney(@PathVariable("userProfileId") long userProfileId) 
			throws InternalException, NotAllowedException {
		
		try {
			return UserMoneyHandler.getInstance().getUserMoney(userProfileId);
		} catch (SQLException ex) {
			logger.error("Exception in getUserMoney", ex);
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
			return userMoney;
		} catch (Exception ex) {
			logger.error("Exception in getFullMoneyTask", ex);
			throw new InternalException("Server Error in getFullMoneyTask");
		}
	}
	
	@RequestMapping(value = "/money/update", method = RequestMethod.POST, produces = "application/json")
	public @ResponseBody List<Integer> loadMoney(@RequestBody UsersCompleteMoneyDetails completeDetails)
			throws InternalException {
		
		logger.info("This is in loadMoney with size : {}", completeDetails.getUsersMoneyTransactionList().size());
		try {
			return UserMoneyHandler.getInstance().performUserMoneyOperation(completeDetails);
		} catch (SQLException ex) {
			logger.error("Exception in loadMoney", ex);
			throw new InternalException("Server Error in loadMoney");
		}
	}
	
	@RequestMapping(value = "/money/wd", method = RequestMethod.POST, produces = "application/json")
	public @ResponseBody boolean performWithdraw(@RequestBody WithdrawMoney wdMoney) throws InternalException {
		try {
			return UserMoneyHandler.getInstance().performWitdrawOperation(wdMoney);
		} catch (SQLException ex) {
			logger.error("Exception in performWithdraw", ex);
			throw new InternalException("Server Error in performWithdraw");
		}
	}
	
	@RequestMapping(value = "/money/update/{serverId}", method = RequestMethod.GET, produces = "application/json")
	public @ResponseBody List<SlotGamesWinMoneyStatus> getGamesSlotMoneyStatus(@PathVariable("serverId") String serverId) 
			throws InternalException {
		return WinnersMoneyUpdateStatus.getInstance().getServerIdStatus(serverId);
	}
}
