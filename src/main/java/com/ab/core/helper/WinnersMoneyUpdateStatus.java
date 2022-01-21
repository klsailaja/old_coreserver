package com.ab.core.helper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.ab.core.pojo.GameSlotMoneyStatus;
import com.ab.core.pojo.SlotGamesWinMoneyStatus;

public class WinnersMoneyUpdateStatus {
	private static WinnersMoneyUpdateStatus instance = null;
	
	private static final Logger logger = LogManager.getLogger(WinnersMoneyUpdateStatus.class);
	
	private Map<String,GameSlotMoneyStatus> serverIdVsGameSlotCompleteStatus = new HashMap<>();
	
	private WinnersMoneyUpdateStatus() {
		
	}
	
	public static WinnersMoneyUpdateStatus getInstance() {
		if (instance == null) {
			logger.info("This is in WinnersMoneyUpdateStatus getInstance");
			instance = new WinnersMoneyUpdateStatus(); 
		}
		return instance;
	}
	
	
	public void createEntry(String trackKey) {
		
		if (trackKey == null) {
			return;
		}
		
		GameSlotMoneyStatus slotStatus = new GameSlotMoneyStatus();
		
		StringTokenizer strTokenizer = new StringTokenizer(trackKey, "-");
		String serverIdKey = strTokenizer.nextToken();
		long slotStartTime = Long.parseLong(strTokenizer.nextToken());
		
		slotStatus.setServerId(serverIdKey);
		slotStatus.setMoneyCreditedStatus(0); // 0 - means not completed
		slotStatus.setSlotGameStartTime(slotStartTime);
		
		serverIdVsGameSlotCompleteStatus.put(trackKey, slotStatus);
	}
	
	public synchronized void setStatusToComplete(String trackKey) {
		
		if (trackKey == null) {
			return;
		}
		
		GameSlotMoneyStatus slotStatus = serverIdVsGameSlotCompleteStatus.get(trackKey);
		slotStatus.setMoneyCreditedStatus(1);
		serverIdVsGameSlotCompleteStatus.put(trackKey, slotStatus);
	}
	
	public synchronized void cleanupOldEntries() {
		Set<Map.Entry<String,GameSlotMoneyStatus>> s = serverIdVsGameSlotCompleteStatus.entrySet();
		
		List<String> toDelKeys = new ArrayList<>();
        
        for (Map.Entry<String, GameSlotMoneyStatus> it: s)
        {
        	String mapKey = it.getKey();
        	GameSlotMoneyStatus status = it.getValue();
        	if (status.getMoneyCreditedStatus() == 1) {
        		long timeElapsed = System.currentTimeMillis() - status.getSlotGameStartTime();
        		if (timeElapsed >= (10 * 60 * 1000)) {
        			toDelKeys.add(mapKey);
        		}
        	}
        }
        for (String delKey : toDelKeys) {
        	serverIdVsGameSlotCompleteStatus.remove(delKey);
        }
	}
	
	public List<SlotGamesWinMoneyStatus> getServerIdStatus(String severId) {
		
		List<SlotGamesWinMoneyStatus> winCreditedStatus = new ArrayList<>();
		
		Set<Map.Entry<String,GameSlotMoneyStatus>> s = serverIdVsGameSlotCompleteStatus.entrySet();
		
		for (Map.Entry<String, GameSlotMoneyStatus> it: s)
        {
        	String mapKey = it.getKey();
        	GameSlotMoneyStatus status = it.getValue();
        	if (mapKey.startsWith(severId)) {
        		SlotGamesWinMoneyStatus statusObj = new SlotGamesWinMoneyStatus();
        		statusObj.setTrackKey(mapKey);
        		statusObj.setCreditedStatus(status.getMoneyCreditedStatus());
        		winCreditedStatus.add(statusObj);
        	}
        }
		return winCreditedStatus;
	}
}
