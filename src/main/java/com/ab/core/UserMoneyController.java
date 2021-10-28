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

import com.ab.core.exceptions.InternalException;
import com.ab.core.exceptions.NotAllowedException;
import com.ab.core.handlers.UserMoneyHandler;
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
	
	/*
	@RequestMapping(value = "/wd/messages/{userId}/{maxCount}", method = RequestMethod.GET, produces = "application/json")
	public @ResponseBody List<String> getRecentWinWDMessages(@PathVariable long userId, @PathVariable int maxCount) 
			throws NotAllowedException, InternalException {
		logger.info("In getRecentWinWDMessages with userId {} and {}", userId, maxCount);
		try {
			List<String> combinedMsgs = WinMsgHandler.getInstance().getCombinedMessages();
			if (userId == -1) {
				return combinedMsgs;
			}
			
			List<Long> closedGroupMembersIds = new ArrayList<>();
			List<String> closedGroupMembersNames = new ArrayList<>();
			
			Utils.getClosedCircleUserIds(userId, maxCount, closedGroupMembersIds, closedGroupMembersNames);
			
			logger.info(closedGroupMembersIds);
			logger.info(closedGroupMembersNames);
			
			if (closedGroupMembersIds.size() > 0) {
				
				List<List<String>> totalUsersWinMsgs = new ArrayList<>();
				List<List<String>> totalUsersWithDrawMsgs = new ArrayList<>();
				
				int winMsgsMaxSize = 0;
				int wdMsgsMaxSize = 0;
				
				for (int userIndex = 0; userIndex < closedGroupMembersIds.size(); userIndex++) {
					 
					long closedGrpUserId = closedGroupMembersIds.get(userIndex);
					String closedGrpUserName = closedGroupMembersNames.get(userIndex);
					
					List<String> gameWinMsgs = MyTransactionDBHandler.
						getInstance().getRecentWinRecords(closedGrpUserId, true, closedGrpUserName);
					List<String> withDrawMsgs = WithdrawDBHandler.
						getInstance().getRecentWinRecords(closedGrpUserId, true, closedGrpUserName);
					
					totalUsersWinMsgs.add(gameWinMsgs);
					totalUsersWithDrawMsgs.add(withDrawMsgs);
					
					if (winMsgsMaxSize < gameWinMsgs.size()) {
						winMsgsMaxSize = gameWinMsgs.size();
					}
					
					if (wdMsgsMaxSize < withDrawMsgs.size()) {
						wdMsgsMaxSize = withDrawMsgs.size();
					}
				}
				
				List<String> closedGrpUsersMsgs = new ArrayList<>();
				
				for (int winMsgIndex = 0; winMsgIndex < winMsgsMaxSize; winMsgIndex ++) {
					for (int totalIndex = 0; totalIndex < totalUsersWinMsgs.size(); totalIndex ++) {
						List<String> gameWinMsgs = totalUsersWinMsgs.get(totalIndex);
						if (winMsgIndex < gameWinMsgs.size()) {
							closedGrpUsersMsgs.add(gameWinMsgs.get(winMsgIndex));
						}
					}
				}
				
				for (int wdMsgIndex = 0; wdMsgIndex < wdMsgsMaxSize; wdMsgIndex ++) {
					for (int totalIndex = 0; totalIndex < totalUsersWithDrawMsgs.size(); totalIndex ++) {
						List<String> gameWdMsgs = totalUsersWithDrawMsgs.get(totalIndex);
						if (wdMsgIndex < gameWdMsgs.size()) {
							closedGrpUsersMsgs.add(gameWdMsgs.get(wdMsgIndex));
						}
					}
				}
				closedGrpUsersMsgs.addAll(closedGrpUsersMsgs);
				closedGrpUsersMsgs.addAll(closedGrpUsersMsgs);
				
				int totalClosedGrpMsgCount = 240 - closedGrpUsersMsgs.size();
				for (int totalIndex = 0; totalIndex < totalClosedGrpMsgCount; totalIndex ++) {
					if (totalIndex < combinedMsgs.size()) {
						closedGrpUsersMsgs.add(combinedMsgs.get(totalIndex));
					}
				}
				logger.info("closedGrpUsersMsgs size {}", closedGrpUsersMsgs.size());
				return closedGrpUsersMsgs;
			}
			
			logger.info("combinedMsgs {}", combinedMsgs.size());
			return combinedMsgs;
		} catch (SQLException ex) {
			logger.error("Exception in getRecentWinWDMessages", ex);
			throw new InternalException("Server Error in getRecentWinWDMessages");
		}
	}*/
}
