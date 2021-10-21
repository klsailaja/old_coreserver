package com.ab.core.tasks;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.ab.core.pojo.Mail;
import com.ab.core.services.MailService;
import com.ab.core.services.MailServiceImpl;

public class SendMailTask implements Runnable {
	private Mail mail;
	private static final Logger logger = LogManager.getLogger(SendMailTask.class);
	
	public SendMailTask(Mail mail) {
		this.mail = mail;
	}
	
	@Override
	public void run() {
		logger.info("This is in Mail Task {}", mail);
		MailService mailService = new MailServiceImpl();
		mailService.sendEmail(mail);
	}
}
