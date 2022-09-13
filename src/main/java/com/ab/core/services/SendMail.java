package com.ab.core.services;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.security.GeneralSecurityException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import org.json.JSONObject;

import com.ab.core.pojo.Mail;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.repackaged.org.apache.commons.codec.binary.Base64;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.Message;

public class SendMail {
	
	private static final String REFRESH_TOKEN = 
			"1//0gOYAGEW9dpRWCgYIARAAGBASNwF-L9Ir19xSDy4Gh4RNbtFDoRM-N2lifyyQj4E4UySL4QUyno1_KVUC3I2aYDsDydbrGv3xNl4";
	
	private static final String CLIENT_ID = 
			"588187770656-l8vkgeqfhvisbi9qar2jroc9prtjoihe.apps.googleusercontent.com";
	private static final String CLIENT_SECRET = 
			"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";
	private static final String ACCESS_TOKEN_HOST = "https://oauth2.googleapis.com/token";
	
	
	/*
	1.Get code : 
	https://accounts.google.com/o/oauth2/v2/auth?
 	scope=https://mail.google.com&
 	access_type=offline&
 	redirect_uri=http://localhost&
 	response_type=code&
 	client_id=[Client ID]
	2. Get access_token and refresh_token
 		curl \
		--request POST \
		--data "code=[Authentcation code from authorization link]&client_id=[Application Client Id]&client_secret=[Application Client Secret]&redirect_uri=http://localhost&grant_type=authorization_code" \
		https://accounts.google.com/o/oauth2/token
	3.Get new access_token using refresh_token
		curl \
		--request POST \
		--data "client_id=[your_client_id]&client_secret=[your_client_secret]&refresh_token=[refresh_token]&grant_type=refresh_token" \
		https://accounts.google.com/o/oauth2/token
	 */
	
	private static final String APPLICATION_NAME = "Gmail API Java Quickstart";
	private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
	private static final String user = "me";
	static Gmail service = null;
	private static File filePath = new File(System.getProperty("user.dir") + "/credentials.json");

	public static void main(String[] args) throws IOException, GeneralSecurityException, MessagingException {
		getGmailService();
		//getMailBody("Google");
		//sendEmail();
	}

	/*public static void getMailBody(String searchString) throws IOException {

		// Access Gmail inbox

		Gmail.Users.Messages.List request = service.users().messages().list(user).setQ(searchString);

		ListMessagesResponse messagesResponse = request.execute();
		request.setPageToken(messagesResponse.getNextPageToken());

		// Get ID of the email you are looking for
		String messageId = messagesResponse.getMessages().get(0).getId();

		Message message = service.users().messages().get(user, messageId).execute();

		// Print email body

		String emailBody = StringUtils
				.newStringUtf8(Base64.decodeBase64(message.getPayload().getParts().get(0).getBody().getData()));

		System.out.println("Email body : " + emailBody);

	}*/
	
	public static Message createMessageWithEmail(MimeMessage email) throws MessagingException, IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		email.writeTo(baos);
		String encodedEmail = Base64.encodeBase64URLSafeString(baos.toByteArray());
		Message message = new Message();
		message.setRaw(encodedEmail);
		return message;
	}

	public static MimeMessage createEmail(String to, String from, String subject, String bodyText) throws MessagingException, IOException {
		Properties props = new Properties();
		Session session = Session.getDefaultInstance(props, null);

		MimeMessage email = new MimeMessage(session);

		email.setFrom(new InternetAddress(from)); //me
		email.addRecipient(javax.mail.Message.RecipientType.TO, new InternetAddress(to)); //
		email.setSubject(subject); 

        email.setText(bodyText);
        
		return email;
	}
	
	
	public static MimeMessage createHTMLEmailBody(String to, String subject, String html) 
			throws AddressException, MessagingException {
		
		Properties props = new Properties();
        Session session = Session.getDefaultInstance(props, null);

        MimeMessage email = new MimeMessage(session);
        
        email.setFrom(new InternetAddress("me"));
         
        //For Multiple Email with comma separated ...
        
        String[] split = to.split(",");
        for(int i=0;i<split.length;i++) { 
        	email.addRecipient(javax.mail.Message.RecipientType.TO, new InternetAddress(split[i]));
        }
    
        email.setSubject(subject);

        Multipart multiPart = new MimeMultipart("mixed");
        
        //HTML Body 
        MimeBodyPart htmlPart = new MimeBodyPart();
        htmlPart.setContent(html, "text/html; charset=utf-8");  
        multiPart.addBodyPart(htmlPart,0);
       
        email.setContent(multiPart);
        return email;
    }
	
	public static void sendEmail(Mail mailObjs) throws IOException, GeneralSecurityException, MessagingException {
		
		Gmail service = getGmailService();
		/*MimeMessage Mimemessage = createEmail(mailObjs.getMailTo(), mailObjs.getMailFrom(),
				mailObjs.getMailSubject(), mailObjs.getMailContent());*/
		MimeMessage mimeMessage = createHTMLEmailBody(mailObjs.getMailTo(), mailObjs.getMailSubject(),
						mailObjs.getMailContent());
	
		Message message = createMessageWithEmail(mimeMessage);
		
		message = service.users().messages().send(user, message).execute();
		
		//System.out.println("Message id: " + message.getId());
		//System.out.println(message.toPrettyString());
	}

	public static Gmail getGmailService() throws IOException, GeneralSecurityException {

		InputStream in = new FileInputStream(filePath); // Read credentials.json
		GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

		// Credential builder
		Credential authorize = new GoogleCredential.Builder().setTransport(GoogleNetHttpTransport.newTrustedTransport())
				.setJsonFactory(JSON_FACTORY)
				.setClientSecrets(clientSecrets.getDetails().getClientId().toString(),
						clientSecrets.getDetails().getClientSecret().toString())
				.build().setAccessToken(getAccessToken()).setRefreshToken(
						REFRESH_TOKEN);
		
		// Create Gmail service
		final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
		service = new Gmail.Builder(HTTP_TRANSPORT, JSON_FACTORY, authorize)
				.setApplicationName(SendMail.APPLICATION_NAME).build();

		return service;
	}

	private static String getAccessToken() {
		
		try {
			Map<String, Object> params = new LinkedHashMap<>();
			params.put("grant_type", "refresh_token");
			params.put("client_id", CLIENT_ID); 
			params.put("client_secret", CLIENT_SECRET); 
			params.put("refresh_token",
					REFRESH_TOKEN); 

			StringBuilder postData = new StringBuilder();
			for (Map.Entry<String, Object> param : params.entrySet()) {
				if (postData.length() != 0) {
					postData.append('&');
				}
				postData.append(URLEncoder.encode(param.getKey(), "UTF-8"));
				postData.append('=');
				postData.append(URLEncoder.encode(String.valueOf(param.getValue()), "UTF-8"));
			}
			byte[] postDataBytes = postData.toString().getBytes("UTF-8");

			URL url = new URL(ACCESS_TOKEN_HOST);
			
			HttpURLConnection con = (HttpURLConnection) url.openConnection();
			con.setDoOutput(true);
			con.setUseCaches(false);
			con.setRequestMethod("POST");
			con.getOutputStream().write(postDataBytes);

			BufferedReader reader = new BufferedReader(new InputStreamReader(con.getInputStream()));
			StringBuffer buffer = new StringBuffer();
			for (String line = reader.readLine(); line != null; line = reader.readLine()) {
				buffer.append(line);
			}

			JSONObject json = new JSONObject(buffer.toString());
			String accessToken = json.getString("access_token");
			return accessToken;
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return null;
	}
}
