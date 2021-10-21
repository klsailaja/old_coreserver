package com.ab.core;

import java.sql.SQLException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.ab.core.exceptions.InternalException;
import com.ab.core.exceptions.NotAllowedException;
import com.ab.core.handlers.UserMoneyHandler;
import com.ab.core.pojo.UserMoney;
import com.ab.core.pojo.UsersCompleteMoneyDetails;


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
	
	@RequestMapping(value = "/money/update", method = RequestMethod.POST, produces = "application/json")
	public @ResponseBody boolean loadMoney(@RequestBody UsersCompleteMoneyDetails completeDetails)
			throws InternalException {
		
		logger.info("This is in loadMoney with size : {}", completeDetails.getUsersMoneyTransactionList().size());
		
		try {
			return UserMoneyHandler.getInstance().performUserMoneyOperation(completeDetails);
		} catch (SQLException ex) {
			logger.error("Exception in loadMoney", ex);
			throw new InternalException("Server Error in loadMoney");
		}
	}
	
	/*
	@RequestMapping(value = "/money/{userProfileId}/load/{amt}", method = RequestMethod.POST, produces = "application/json")
	public @ResponseBody boolean loadMoney(@PathVariable("userProfileId") long userProfileId, @PathVariable("amt") int amt,
			@RequestBody TransferRequest transferReq)
			throws InternalException {
		try {
			return UserMoneyHandler.getInstance().updateUserMoney(userProfileId, amt);
		} catch (SQLException ex) {
			logger.error("Exception in loadMoney", ex);
			throw new InternalException("Server Error in loadMoney");
		}
	} */
	
	
	/*
	@RequestMapping(value = "/money/{userProfileId}/transfer", method = RequestMethod.POST, produces = "application/json")
	public @ResponseBody boolean transferMoney(@PathVariable("userProfileId") long userProfileId, 
			@RequestBody TransferRequest transferReq) throws NotAllowedException, InternalException {
		try {
			return UserMoneyHandler.getInstance().transferMoney(userProfileId, transferReq);
		} catch (SQLException ex) {
			logger.error("Exception in transferMoney", ex);
			throw new InternalException("Server Error in transferMoney");
		}
	}*/
}
