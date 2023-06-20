package com.ab.core.helper;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.ab.core.common.LazyScheduler;
import com.ab.core.common.TAGS;
import com.ab.core.constants.QuizConstants;
import com.ab.core.db.MyTransactionDBHandler;

public class WinMsgHandler implements Runnable {
	
	private static final Logger logger = LogManager.getLogger(WinMsgHandler.class);
	private static WinMsgHandler instance = null;
	
	// combinedMessages will have top WIN messages and top WD messages from the server instance 1
	private List<String> combinedMessages = new ArrayList<>();
	private List<String> gameWinMsgs = new ArrayList<>();
	// top WD messages from the server instance 1
	private List<String> gameWdMsgs = new ArrayList<>();
	private ReadWriteLock lock = new ReentrantReadWriteLock();
	
	private WinMsgHandler() {
	}
	
	public static WinMsgHandler getInstance() {
		if (instance == null) {
			logger.debug("{} In WinMsgHandler getInstance() method instance created", TAGS.WIN_WD_MSG_TASK);
			instance = new WinMsgHandler();
			instance.initialize();
		}
		return instance;
	}
	
	private void initialize() {
		LazyScheduler.getInstance().submitRepeatedTask(this, 0, 
				30, TimeUnit.MINUTES);
	}

	@Override
	public void run() {
		logger.info("{} In WinMsgHandler run()", TAGS.WIN_WD_MSG_TASK);
		lock.writeLock().lock();
		try {
			
			combinedMessages.clear();
			gameWinMsgs.clear();
			
			List<String> recentWinMsgs = MyTransactionDBHandler.getInstance().
					getRecentWinRecords(TAGS.WIN_WD_MSG_TASK, -1, false, null);
			gameWinMsgs.addAll(recentWinMsgs);
			
			List<String> remainingMsgs = gameWinMsgs;
			
			int size1 = gameWinMsgs.size();
			int size2 = gameWdMsgs.size();
			
			logger.info("In the run method {} : {}", size1, size2);
			
			int smallSize = size2;
			if (size1 < smallSize) {
				smallSize = size1;
				remainingMsgs = gameWdMsgs;
			}
			
			for (int index = 0; index < smallSize; index ++) {
				combinedMessages.add(gameWinMsgs.get(index));
				combinedMessages.add(gameWdMsgs.get(index));
			}
			combinedMessages.addAll(remainingMsgs.subList(smallSize, remainingMsgs.size()));
		} catch(Exception ex) {
			logger.error(QuizConstants.ERROR_PREFIX_START);
			logger.error("Exception seen in Core Server Win Msg Handler", ex);
			logger.error(QuizConstants.ERROR_PREFIX_END);
			
		} finally {
			lock.writeLock().unlock();
		}
	}
	
	public void setWithdrawMessages(List<String> wdMsgs) {
		gameWdMsgs.clear();
		gameWdMsgs.addAll(wdMsgs);
		logger.info("{} Successful withdraw messages received here with size {}", TAGS.WIN_WD_MSG_TASK, wdMsgs.size());
	}
	
	public List<String> getRecentWinMsgs() {
		return gameWinMsgs;
	}
	
	
	public List<String> getCombinedMessages() {
		lock.readLock().lock();
		List<String> retValue = combinedMessages;
		lock.readLock().unlock();
		return retValue;
	}
}
