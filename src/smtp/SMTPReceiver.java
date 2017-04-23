package smtp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.Properties;
import java.util.Scanner;

import javax.mail.*;
import javax.mail.internet.*;

public class SMTPReceiver extends Thread {

	private final static int PORT = 25444;
	private final static String OUR_DOMEN = "mmishak.ru";
	private final static String MAILRU_DOMEN = "mail.ru";

	
	public void run() {
		try {
			ServerSocket ss = new ServerSocket(PORT);
			
			while(true) {
				new HandleRequest(ss.accept()).start();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private class HandleRequest extends Thread {
		
		private Socket socket;
		private BufferedReader in;
		private PrintWriter out;

        private String subjectString = "Subject: ";
        private String fromString = "From: ";
        private String toString = "To: ";
        private String dateString = "Date: ";
		
		public HandleRequest(Socket socket) {
			this.socket = socket;
			try {
				in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
				out = new PrintWriter(socket.getOutputStream(), true);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		public void run() {
			
			String mailFrom = null;
			String mailTo = null;
			String body = "";
			String subject = null;
			String date = null;

			
			try {
				out.println(220 + " connect success");
				String temp = in.readLine();
				if(temp.startsWith("AUTH PLAIN")) {
					out.println("Accepted");
					in.readLine();
				}
				
				out.println(250);
				in.readLine();
				
				out.println(250);
				in.readLine();
				
				out.println(250);
				if (in.readLine().equalsIgnoreCase("data")) {
					out.println(354);
					String input;
					while(!(input = in.readLine()).equals(".")) {
						if (input.startsWith(fromString)) {
							input.replaceAll("<", "");
							input.replaceAll(">", "");
						}
						body += input + "\n";
					}
					out.println(250 + " receive data success");
				}
				
				if (in.readLine().equalsIgnoreCase("quit")) {
					out.println(221);
				}
				
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				try {
					out.close();
					in.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			
			Scanner sc = new Scanner(body);
			while(sc.hasNextLine()) {
				String input = sc.nextLine();
				if (input.startsWith(fromString)) {
					mailFrom = input;
				}
				if (input.startsWith(toString)) {
					mailTo = input;
				}
				if (input.startsWith(subjectString)) {
					subject = input;
				}
				if (input.startsWith(dateString)) {
					date = input;
				}
				if ((mailFrom != null) && (mailTo != null) && (subject != null) && (date != null)) {
					break;
				}
			}
			
			sc.close();

			subject = subject.substring(subjectString.length());
			date = date.substring(dateString.length());
			
			System.out.println("-----");
			System.out.println("new mailfrom = " + mailFrom);
			System.out.println("new mailto = " + mailTo);
			System.out.println("new subject = " + subject);
			System.out.println("new date = " + date);

			String toDomen;
			if (mailTo.contains("<"))
			    toDomen = mailTo.substring(mailTo.indexOf("@")+1, mailTo.indexOf(">"));
			else
			    toDomen = mailTo.substring(mailTo.indexOf("@")+1);
			
			if (toDomen.equals(OUR_DOMEN)) {
				Email email = new Email(mailFrom, mailTo, subject, date, body);
				email.save();
				System.out.println("Email saved");
			} else if (toDomen.equals(MAILRU_DOMEN)){
                try {
                    sendEmail(
                            mailFrom.substring(fromString.length()),
                            mailTo.substring(toString.length()),
                            subject,
                            body,
                            toDomen
                    );
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                }
            } else {
				System.out.println("=( " + toDomen);
			}
		}

		private void sendEmail(String from, String to, String subject, String content, String serverDomen) throws UnknownHostException {

//		    String host = "smtp." + serverDomen;
		    String host = "mxs." + serverDomen;

            Properties properties = System.getProperties();

            properties.put("mail.smtp.host", host);
			properties.put("mail.smtp.auth", "false");
//			properties.put("mail.debug", "true");

            Session session = Session.getDefaultInstance(properties);

            try {
                MimeMessage message = new MimeMessage(session);

                message.setFrom(new InternetAddress(from));

                message.addRecipient(Message.RecipientType.TO, new InternetAddress(to));

                message.setSubject(subject);

				message.setSentDate(new Date());

                message.setText(content);
//
//				Multipart multipart = new MimeMultipart();
//				MimeBodyPart contentPart = new MimeBodyPart();
//				contentPart.setContent(content, "text/htm; charset=utf-8");
//				multipart.addBodyPart(contentPart);
//				message.setContent(multipart);

				System.out.println("Sending start");
				Transport.send(message);
                System.out.println("Sent message successfully....");
            }catch (MessagingException mex) {
                mex.printStackTrace();
            }
        }
	}
}
