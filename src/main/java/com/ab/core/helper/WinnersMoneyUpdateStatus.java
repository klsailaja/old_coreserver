package com.ab.core.helper;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.ab.core.constants.MoneyCreditStatus;
import com.ab.core.pojo.GameSlotMoneyStatus;
import com.ab.core.pojo.UsersCompleteMoneyDetails;

public class WinnersMoneyUpdateStatus {
	private static WinnersMoneyUpdateStatus instance = null;
	
	private static final Logger logger = LogManager.getLogger(WinnersMoneyUpdateStatus.class);
	
	private List<GameSlotMoneyStatus> gameSlotMoneyStatusList = new ArrayList<>();
	
	private WinnersMoneyUpdateStatus() {
		
	}
	
	public static WinnersMoneyUpdateStatus getInstance() {
		if (instance == null) {
			logger.info("This is in WinnersMoneyUpdateStatus getInstance");
			instance = new WinnersMoneyUpdateStatus(); 
		}
		return instance;
	}
	
	
	public synchronized void createEntry(UsersCompleteMoneyDetails usersCompleteDetailsObj) {
		
		GameSlotMoneyStatus slotStatus = new GameSlotMoneyStatus();
		
		slotStatus.setServerId(usersCompleteDetailsObj.getServerId());
		slotStatus.setRequestId(usersCompleteDetailsObj.getRequestId());
		slotStatus.setOperationType(usersCompleteDetailsObj.getOperationType());
		
		slotStatus.setMoneyCreditedStatus(MoneyCreditStatus.IN_PROGRESS.getId());
		
		gameSlotMoneyStatusList.add(slotStatus);
	}
	
	public synchronized void setStatusToComplete(int requestId, int serverId, List<Integer> uniqueIds,
			List<Integer> dbResultIds, int overallStatus) {
		for (GameSlotMoneyStatus obj : gameSlotMoneyStatusList) {
			if (obj.getServerId() == serverId) {
				if (obj.getRequestId() == requestId) {
					obj.setMoneyCreditedStatus(overallStatus);
					obj.setUniqueIds(uniqueIds);
					obj.setDbResultsIds(dbResultIds);
					obj.setCompletedTime(System.currentTimeMillis());
				}
			}
		}
	}
	
	public synchronized void cleanupOldEntries() {
		Iterator<GameSlotMoneyStatus> iterator = gameSlotMoneyStatusList.iterator();
		while (iterator.hasNext()) {
			GameSlotMoneyStatus object = iterator.next();
			if (object.getCompletedTime() != 0) {
				long currentTime = System.currentTimeMillis();
				if ((currentTime - object.getCompletedTime()) >= 10 * 60 * 1000) {
					iterator.remove();
				}
			}
		}
	}
	
	public List<GameSlotMoneyStatus> getServerIdStatus(int severId) {
		
		List<GameSlotMoneyStatus> serverSpecificMoneyStatusList = new ArrayList<>();
		
		for (GameSlotMoneyStatus obj : gameSlotMoneyStatusList) {
			if (obj.getServerId() == severId) {
				GameSlotMoneyStatus statusObj = new GameSlotMoneyStatus();
				
				statusObj.setRequestId(obj.getRequestId());
				statusObj.setServerId(obj.getServerId());
				statusObj.setOperationType(obj.getOperationType());
				statusObj.setMoneyCreditedStatus(obj.getMoneyCreditedStatus());
				statusObj.setUniqueIds(obj.getUniqueIds());
				statusObj.setDbResultsIds(obj.getDbResultsIds());
				statusObj.setCompletedTime(obj.getCompletedTime());
				
				serverSpecificMoneyStatusList.add(statusObj);
			}
		}
		
		return serverSpecificMoneyStatusList;
	}
}
