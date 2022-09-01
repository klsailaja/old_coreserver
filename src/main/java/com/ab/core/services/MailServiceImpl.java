package com.ab.core.services;

import java.io.IOException;
import java.security.GeneralSecurityException;

import javax.mail.MessagingException;

import com.ab.core.pojo.Mail;

public class MailServiceImpl implements MailService {
	
    /*public JavaMailSender getMailSender() {
    	
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
 
        mailSender.setHost("smtp.gmail.com");
        mailSender.setPort(587);
        mailSender.setUsername("ggraj.pec");
        mailSender.setPassword("Moonlight10");
 
        Properties javaMailProperties = new Properties();
        javaMailProperties.put("mail.smtp.starttls.enable", "true");
        javaMailProperties.put("mail.smtp.auth", "true");
        javaMailProperties.put("mail.transport.protocol", "smtp");
        javaMailProperties.put("mail.debug", "false");
        
        mailSender.setJavaMailProperties(javaMailProperties);
        return mailSender;
        
        JavaMailSender mailSender = getMailSender();
        MimeMessage mimeMessage = mailSender.createMimeMessage();
 
        try {
 
            MimeMessageHelper mimeMessageHelper = new MimeMessageHelper(mimeMessage, true);
 
            mimeMessageHelper.setSubject(mail.getMailSubject());
            mimeMessageHelper.setTo(mail.getMailTo());
            mimeMessageHelper.setText(mail.getMailContent());
            
            mimeMessageHelper.setFrom(new InternetAddress(mail.getMailFrom(), "gmail.com"));
            mailSender.send(mimeMessageHelper.getMimeMessage());
 
        } catch (MessagingException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

    }*/
    
    public void sendEmail(Mail mail) {
    	
    	try {
			SendMail.sendEmail(mail);
		} catch (IOException | GeneralSecurityException | MessagingException e) {
			e.printStackTrace();
		}
     }
}
