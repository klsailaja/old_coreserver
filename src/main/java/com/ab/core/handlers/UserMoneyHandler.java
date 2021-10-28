package com.ab.core.handlers;

import java.io.FileNotFoundException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.ab.core.common.LazyScheduler;
import com.ab.core.constants.TransactionType;
import com.ab.core.constants.UserMoneyAccountType;
import com.ab.core.constants.UserMoneyOperType;
import com.ab.core.db.ConnectionPool;
import com.ab.core.db.UserMoneyDBHandler;
import com.ab.core.exceptions.NotAllowedException;
import com.ab.core.helper.SingleThreadMoneyUpdater;
import com.ab.core.helper.Utils;
import com.ab.core.pojo.MoneyTransaction;
import com.ab.core.pojo.MyTransaction;
import com.ab.core.pojo.UserMoney;
import com.ab.core.pojo.UsersCompleteMoneyDetails;
import com.ab.core.pojo.WithdrawMoney;
import com.ab.core.tasks.AddTransactionsTask;
import com.ab.core.tasks.UserMoneyUpdateProcessorTask;

public class UserMoneyHandler {
	private static final Logger logger = LogManager.getLogger(UserMoneyHandler.class);
	private static UserMoneyHandler instance = null;
	
	private UserMoneyHandler() {
	}
	
	public static UserMoneyHandler getInstance() {
		if (instance == null) {
			logger.debug("In UserMoneyHandler getInstance() method instance created");
			instance = new UserMoneyHandler();
		}
		return instance;
	}
	
	public UserMoney getUserMoney(long id) throws SQLException, NotAllowedException {
		UserMoney userMoneyDb = UserMoneyDBHandler.getInstance().getUserMoneyById(id);
		return userMoneyDb;
	}
	
	public List<Integer> performUserMoneyOperation(UsersCompleteMoneyDetails usersMoneyDetails) 
			throws NotAllowedException, SQLException {
		
		String trackKey = usersMoneyDetails.getTrackStatusKey();
		logger.info("This is in performUserMoneyOperation with trackKey: {} , money transactions size {}",
				trackKey, usersMoneyDetails.getUsersMoneyTransactionList().size());
		
		boolean checkMoney = usersMoneyDetails.isCheckMoney();
		
		if (checkMoney) {
			MoneyTransaction moneyTransaction = usersMoneyDetails.getUsersMoneyTransactionList().get(0); 
			long userId = moneyTransaction.getUserProfileId();
			UserMoney userMoney = getUserMoney(userId);
			if (userMoney.getId() == 0) {
				throw new NotAllowedException("User Money details not found");
			}
			if (moneyTransaction.getAmount() > userMoney.getAmount()) {
				throw new NotAllowedException("No Enough Cash. Please add money");
			}
		}
		
		UserMoneyUpdateProcessorTask moneyProcessorTask = new UserMoneyUpdateProcessorTask(usersMoneyDetails);
		
		if (trackKey == null) {
			moneyProcessorTask.run();
			return moneyProcessorTask.getMoneyUpdateResults();
		}
		
		SingleThreadMoneyUpdater.getInstance().submit(moneyProcessorTask);
		return null;
	}
	
	public boolean performWitdrawOperation(WithdrawMoney wdMoney) throws SQLException {
		
		String wdSql = UserMoneyDBHandler.WITHDRAW_BALANCE_AMOUNT_BY_USER_ID;
		
		ConnectionPool cp = null;
		Connection dbConn = null;
		PreparedStatement ps = null;
		
		long userProfileId = wdMoney.getUid();
		
		UserMoneyAccountType accType = UserMoneyAccountType.LOADED_MONEY; 
		
		List<UserMoneyAccountType> accTypes = new ArrayList<>();
		accTypes.add(accType);
				
		List<TransactionType> transactionTypes = new ArrayList<>();
		transactionTypes.add(TransactionType.DEBITED);
				
		List<String> comments = new ArrayList<>();
		comments.add("Withdraw Request Placed");
				
		List<MyTransaction> wdRelatedTransactions = UserMoneyDBHandler.getInstance(). 
			getTransactionObjects(wdMoney.getUid(), wdMoney.getWdAmt(), 
						accTypes, transactionTypes, comments);
	
		int transferRes = 0;
		
		try {
			cp = ConnectionPool.getInstance();
			dbConn = cp.getDBConnection();
			ps = dbConn.prepareStatement(wdSql);
			
			int withdrawAmt = wdMoney.getWdAmt();
			
			ps.setLong(1, (withdrawAmt * -1));
			ps.setLong(2, withdrawAmt);
			ps.setLong(3, userProfileId);
			
			int createResult = ps.executeUpdate();
			if (createResult > 0) {
				transferRes = 1;
			}
			logger.debug("WithdrawMoney createResult {}", createResult);
			for (MyTransaction transaction : wdRelatedTransactions) {
				transaction.setOperResult(transferRes);
			}
			
			LazyScheduler.getInstance().submit(new AddTransactionsTask(wdRelatedTransactions));
		} catch(SQLException ex) {
			logger.error("Error while executing withdraw lock money statement", ex);
			throw ex;
		} finally {
			if (ps != null) {
				ps.close();
			}
			if (dbConn != null) {
				dbConn.close();
			}
		}
		return true;
	}
	
	public boolean updateUserMoney (long userProfileId, int amt) throws SQLException {
		logger.info("This is in updateUserMoney {} : {}", userProfileId, amt);
		UserMoneyAccountType accountType = UserMoneyAccountType.LOADED_MONEY;
		UserMoneyOperType operType = UserMoneyOperType.ADD;
		long longAmt = amt;
		
		UserMoney userMoney = UserMoneyHandler.getInstance().getUserMoney(userProfileId);
		long userOB = userMoney.getAmount();
		long userCB = longAmt + userOB;
		long currentTime = System.currentTimeMillis();
		String comments = "Loaded Money";
		
		MyTransaction transaction = Utils.getTransactionPojo(userProfileId,currentTime, 
				amt, TransactionType.CREDITED.getId(), accountType.getId(), userOB, userCB,
				comments, null);
		return UserMoneyDBHandler.getInstance().updateUserMoney(accountType, operType, userProfileId, 
				longAmt, transaction);
	}

	/*public boolean transferMoney(long userProfileId, TransferRequest transferReq) 
			throws NotAllowedException, SQLException {
		
		UserMoneyAccountType accType = UserMoneyAccountType.findById(transferReq.getSourceAccType());
		if (accType == null) {
			throw new NotAllowedException("Specified account type not found");
		}
		UserMoney userMoney = getUserMoney(userProfileId);
		long accountMoney = userMoney.getLoadedAmount();
		if (accType == UserMoneyAccountType.WINNING_MONEY) {
			accountMoney = userMoney.getWinningAmount();
		}
		if (accType == UserMoneyAccountType.REFERAL_MONEY) {
			accountMoney = userMoney.getReferalAmount();
		}
		if (transferReq.getAmount() > accountMoney) {
			throw new NotAllowedException("Enough money not present in the account");
		}
		return UserMoneyDBHandler.getInstance().transferAmount(userProfileId, transferReq); 
				
	}*/
	
	// Withdraw support methods
	
	/*private boolean isUserEligibleForWD(WithdrawRequestInput wdInputObject)
		throws NotAllowedException, SQLException {
		
		WDUserInput wdUserInput = wdInputObject.getWithdrawUserInput();
		
		long userProfileId = wdUserInput.getUserProfileId();
		
		boolean hasInMemRecords = InMemUserMoneyManager.getInstance().hasInMemRecords(wdUserInput.getUserProfileId());
		if (hasInMemRecords) {
			InMemUserMoneyManager.getInstance().commitNow();
		}
		
		UserMoney userMoneyDb = UserMoneyDBHandler.getInstance().getUserMoneyById(userProfileId);
		if (userMoneyDb.getId() == 0) {
			throw new NotAllowedException("Invalid user");
		}
		logger.info("The user money DB entry is {}", userMoneyDb);
		
		long accountMoney = userMoneyDb.getAmount();
		
		logger.info("Withdraw Amount and Account Money {} and {}", wdUserInput.getAmount(), accountMoney);
		
		if (wdUserInput.getAmount() > accountMoney) {
			throw new NotAllowedException("Withdraw Amount is more than available amount");
		}
		
		if (wdUserInput.getRequestType() == WithdrawReqType.BY_PHONE.getId()) {
			WithdrawReqByPhone byPhone = wdInputObject.getByPhoneDetails();
			if ((byPhone.getPhNumber() == null) || (byPhone.getPhNumber().length() == 0)) {
				throw new NotAllowedException("Phone number is empty");
			}
			PhonePaymentTypes phonePayType = PhonePaymentTypes.findById(byPhone.getPaymentMethod());
			if (phonePayType == null) {
				throw new NotAllowedException("Invalid phone pay method");
			}
			if ((byPhone.getAccountHolderName() == null) || (byPhone.getAccountHolderName().length() == 0)) {
				throw new NotAllowedException("User Name is empty");
			}
		} else if (wdUserInput.getRequestType() == WithdrawReqType.BY_BANK.getId()) {
			
			WithdrawReqByBank byBank = wdInputObject.getByBankDetails();
			
			if ((byBank.getAccountNumber() == null) || (byBank.getAccountNumber().length() == 0)) {
				throw new NotAllowedException("Bank Account Number is empty");
			}
			if ((byBank.getIfscCode() == null) || (byBank.getIfscCode().length() == 0)) {
				throw new NotAllowedException("Bank Ifsc Number is empty");
			}
			if ((byBank.getBankName() == null) || (byBank.getBankName().length() == 0)) {
				throw new NotAllowedException("Bank Name is empty");
			}
			if ((byBank.getUserName() == null) || (byBank.getUserName().length() == 0)) {
				throw new NotAllowedException("User Name is empty");
			}
		}
		return true;
	}*/
	
	/*
	public String getUserAccWithDrawSql(int accType) {
		String wdSqlQry = UserMoneyDBHandler.WITHDRAW_BALANCE_AMOUNT_BY_USER_ID; 
		return wdSqlQry;
	}*/
	
	/*
	public boolean placeWithdrawMoneyRequest(WithdrawRequestInput wdInputObject)
		throws NotAllowedException, SQLException {
		
		boolean validOper = isUserEligibleForWD(wdInputObject);
		
		if (!validOper) {
			return false;
		}
		
		WDUserInput wdUserInput = wdInputObject.getWithdrawUserInput();
		WithdrawReqByPhone byPhoneReq = wdInputObject.getByPhoneDetails();
		WithdrawReqByBank byBankReq = wdInputObject.getByBankDetails();
		if (byPhoneReq != null) {
			byPhoneReq.setAccountHolderName(byPhoneReq.getAccountHolderName().trim());
			byPhoneReq.setPhNumber(byPhoneReq.getPhNumber().trim());
		}
		if (byBankReq != null) {
			byBankReq.setAccountNumber(byBankReq.getAccountNumber().trim());
			byBankReq.setIfscCode(byBankReq.getIfscCode().trim());
			byBankReq.setUserName(byBankReq.getUserName().trim());
			byBankReq.setBankName(byBankReq.getBankName().trim());
		}
		
		logger.info("WithdrawMoney is called with inputs {} and {} and {}", wdUserInput, byPhoneReq, byBankReq);
		
		WithdrawDBHandler wdDBHandler = WithdrawDBHandler.getInstance();
		boolean wdrecordsCreated = wdDBHandler.createWithDrawReq(wdUserInput, byPhoneReq, byBankReq);
		if (!wdrecordsCreated) {
			logger.debug("Withdraw DB Records not created in DB");
			throw new NotAllowedException("Could not insert beneficiary details to DB");
		}
		
		String wdSql = UserMoneyDBHandler.WITHDRAW_BALANCE_AMOUNT_BY_USER_ID;
		ConnectionPool cp = null;
		Connection dbConn = null;
		PreparedStatement ps = null;
		
		long userProfileId = wdUserInput.getUserProfileId();
		
		UserMoneyAccountType accType = UserMoneyAccountType.LOADED_MONEY; 
		
		List<UserMoneyAccountType> accTypes = new ArrayList<>();
		accTypes.add(accType);
				
		List<TransactionType> transactionTypes = new ArrayList<>();
		transactionTypes.add(TransactionType.DEBITED);
				
		List<String> comments = new ArrayList<>();
		comments.add("Withdraw Request Placed");
				
		List<MyTransaction> wdRelatedTransactions = UserMoneyDBHandler.getInstance(). 
			getTransactionObjects(wdUserInput.getUserProfileId(), wdUserInput.getAmount(), 
						accTypes, transactionTypes, comments);

		int transferRes = 0;
		
		try {
			cp = ConnectionPool.getInstance();
			dbConn = cp.getDBConnection();
			ps = dbConn.prepareStatement(wdSql);
			
			int withdrawAmt = wdUserInput.getAmount();
			
			ps.setLong(1, (withdrawAmt * -1));
			ps.setLong(2, withdrawAmt);
			ps.setLong(3, userProfileId);
			
			int createResult = ps.executeUpdate();
			if (createResult > 0) {
				transferRes = 1;
			}
			logger.debug("WithdrawMoney createResult {}", createResult);
			for (MyTransaction transaction : wdRelatedTransactions) {
				transaction.setOperResult(transferRes);
			}
			
			LazyScheduler.getInstance().submit(new AddTransactionsTask(wdRelatedTransactions));
		} catch(SQLException ex) {
			logger.error("Error while executing withdraw lock money statement", ex);
			throw ex;
		} finally {
			if (ps != null) {
				ps.close();
			}
			if (dbConn != null) {
				dbConn.close();
			}
		}
		return true;
	}
	
	public boolean cancelWithdrawRequest(long userProfileId, String withdrawRefId) 
			throws SQLException,NotAllowedException {
		
		WithdrawDBHandler withdrawDbHandler = WithdrawDBHandler.getInstance();
		return withdrawDbHandler.cancelWithdrawRequest(userProfileId, withdrawRefId);
	}
	
	public boolean closeWithDrawRequest(String receiptFileName, String withdrawRefId, String wdClosedCmts) 
			throws SQLException, NotAllowedException, FileNotFoundException {
		
		WithdrawDBHandler withdrawDbHandler = WithdrawDBHandler.getInstance();
		return withdrawDbHandler.closeWithDrawRequest(receiptFileName, withdrawRefId, wdClosedCmts);
	}
	
	public WithdrawRequestsHolder getWithdrawDataSet(long userProfileId, int startRowNumber, int state) 
			throws SQLException, NotAllowedException {
		
		WithdrawDBHandler withdrawDbHandler = WithdrawDBHandler.getInstance();
		return withdrawDbHandler.getWithdrawRequests(userProfileId, startRowNumber, state);
	}
	
	public byte[] getReceiptContents(long id) throws NotAllowedException, SQLException {
		WithdrawReceiptDBHandler wdReceiptDBHander = WithdrawReceiptDBHandler.getInstance();
		return wdReceiptDBHander.getReceiptContents(id);
	} */
	
	public static void main(String[] args) throws NotAllowedException, SQLException, FileNotFoundException {
		/*
		WDUserInput wdInput = new WDUserInput();
		
		wdInput.setUserProfileId(70);
		wdInput.setRequestType(WithdrawReqType.BY_PHONE.getId());
		wdInput.setAmount(100);
		wdInput.setOpenedTime(System.currentTimeMillis());
		
		WithdrawReqByPhone byPhone = new WithdrawReqByPhone();
		byPhone.setPhNumber("9566229372");
		byPhone.setPaymentMethod(1);
		byPhone.setAccountHolderName("Rajasekhar");
		
		WithdrawRequestInput wdInputs = new WithdrawRequestInput();
		wdInputs.setWithdrawUserInput(wdInput);
		wdInputs.setByPhoneDetails(byPhone);
		
		UserMoneyHandler handler = UserMoneyHandler.getInstance();*/
		/*boolean result = handler.placeWithdrawMoneyRequest(wdInputs);
		System.out.println("result is :" + result);*/
		
		/*boolean result = handler.cancelWithdrawRequest(70, "htheFhBQy1");
		System.out.println("The WD Cancellation status is " + result);*/
		
		/*String filePath = "D:" + File.separator + "Projects" + File.separator + "Receipt.png";
		
		boolean result = handler.closeWithDrawRequest(filePath, "hwhqjS6BE5", "Paid in Phone3");
		System.out.println("The WD Closure result is " + result);*/
		
		/*WithdrawRequestsHolder holder = handler.getWithdrawDataSet(70, 0, -1);
		System.out.println(holder);*/
	}
}
