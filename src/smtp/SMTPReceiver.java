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
import javax.naming.NamingException;

public class SMTPReceiver extends Thread {

	// Порт, на котором ждет наш сервер
	private final static int PORT = 25444;

	// Домен, для которого мы сохраняем почту локально
	private final static String OUR_DOMEN = "mmishak.ru";

	// запускается когда вызывают start()
	public void run() {
		try {
			ServerSocket ss = new ServerSocket(PORT); // Создали свервер-сокет для ожидания подключений клиентов

			// В бесконечном цикле ожидаем подключений
			while(true) {
				// при новом подключений ss.accept() возвращает сокет для определенного клиента
				// данный сокет передается в конструктор HandleRequest и потом метод start()
				// запускает метод run() у HandleRequest в отдельно потоке
				// таким образом для кажого нового подключения создается новый поток
				new HandleRequest(ss.accept()).start();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// Обработчик подключения клиента
	private class HandleRequest extends Thread {
		
		private Socket socket;		// Сокет клиента
		private BufferedReader in;	// Символьный поток чтения из сокета (получение команд от клиента)
		private PrintWriter out;	// Символьный поток записи в сокет (отправка клиенту ответов)

		// константы префиксов для парсинга заголовков письма
        private final String subjectString = "Subject: ";	// Тема письма
        private final String fromString = "From: ";			// Отправитель
        private final String toString = "To: ";				// Получатель
        private final String dateString = "Date: ";			// Дата отправки

		// Конструктор
		public HandleRequest(Socket socket) {

			// Сначала сохраняем переданный нам сокет
			this.socket = socket;

			// Затем получаем потоки чтения/записи сокета
			try {
				// здесь байтовый поток InputStream
				// при помощи адаптера InputStreamReader
				// преобразуется в символьный поток BufferedReader
				in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
				// аналогично OutputStream -> PrintWriter
				out = new PrintWriter(socket.getOutputStream(), true);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		// запускается, когда вызывают start()
		// здесь происходит получение письма по протоколу SMTP
		public void run() {
			
			String mailFrom = null;		// отправитель
			String mailTo = null;		// получатель
			String body = "";			// тело письма
			String subject = null;		// тема
			String date = null;			// дата

			
			try {
				// Сообщаем клиенту, что о успешно подключен
				out.println(220 + " connect success");

				// Читаем следующу команду клиента
				String temp = in.readLine();

				// Если ачинается с "AUTH PLAIN", то отправляем в ответ "Accepted"
				// и читаем новую команду
				if(temp.startsWith("AUTH PLAIN")) {
					out.println("Accepted");
					in.readLine();
				}

				// В прочитанной команде будет "EHLO ..."
				// отправляем код успешного получения команды
				out.println(250);
				in.readLine();	// читаем дальше

				// В прочитанной команде будет "MAIL FROM: ..."
				// отправляем код успешного получения команды
				out.println(250);
				in.readLine();	// читаем дальше

				// В прочитанной команде будет "RCPT TO: ..."
				// отправляем код успешного получения команды
				out.println(250);

				// Если следующая команда "DATA", то начинаем принимать тело письма
				if (in.readLine().equalsIgnoreCase("data")) {
					out.println(354);	// отправляем код успешного получения команды DATA

					String input;
					// пока прочитанная строка не ровна "." выполняем цикл
					while(!(input = in.readLine()).equals(".")) {
						body += input + "\n";
					}
					// отправляем ответ об успешном получении тела письма
					out.println(250 + " receive data success");
				}

				// Если команда ровна QUIT то отправляем код успешного получения
				if (in.readLine().equalsIgnoreCase("quit")) {
					out.println(221);
				}
				
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				// Здесь закрываем потоки чтения/записи
				try {
					out.close();
					in.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}


			// Далее парсим прочитанное тело письма
			Scanner sc = new Scanner(body);
			// Пока у нас есть следующая строка
			while(sc.hasNextLine()) {
				// Мы ее читаем
				String input = sc.nextLine();

				// Ис мотрим с чего она начинается:
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

				// Когда все поля (отправитель, получатель, тема, дата) заполнены, выходим из цикла
				if ((mailFrom != null) && (mailTo != null) && (subject != null) && (date != null)) {
					break;
				}
			}
			sc.close();


			// "обрезаем" префиксы для темы письма и даты
			subject = subject.substring(subjectString.length());
			date = date.substring(dateString.length());

			// Просто вывод в консоль
			System.out.println("-----");
			System.out.println("new mailfrom = " + mailFrom);
			System.out.println("new mailto = " + mailTo);
			System.out.println("new subject = " + subject);
			System.out.println("new date = " + date);


			// Теперь рассматриваем домен получателя
			String toDomen;
			// Сохраняем в в переменную toDomen ту часть адреса отправителя, которая идет после @
			if (mailTo.contains("<"))
			    toDomen = mailTo.substring(mailTo.indexOf("@")+1, mailTo.indexOf(">"));
			else
			    toDomen = mailTo.substring(mailTo.indexOf("@")+1);


			// Если toDomen соответствует нашему локальному домену, то сохраняем письмо
			if (toDomen.equals(OUR_DOMEN)) {
				Email email = new Email(mailFrom, mailTo, subject, date, body);
				email.save();
				System.out.println("Email saved");
			} else { // иначе отправляем письмо дальше
                try {
                	// Метод sendEmail описан ниже
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
            }
		}

		private void sendEmail(String from, String to, String subject, String content, String serverDomen) throws UnknownHostException {

			// Получаем адрес почтового сервера для домена (!!!)
			String host = serverDomen;
			try {
				host = MailHostsLookup.lookupMailHosts(serverDomen)[0];
				System.out.println(host);
			} catch (NamingException e) {
				e.printStackTrace();
			}


			// ========= Данная часть выполненая по туториалу JavaMail ===========

			Properties properties = System.getProperties();

			properties.put("mail.smtp.host", host);
			properties.put("mail.smtp.auth", "false");

            Session session = Session.getDefaultInstance(properties);

            try {
                MimeMessage message = new MimeMessage(session);

                message.setFrom(new InternetAddress(from));

                message.addRecipient(Message.RecipientType.TO, new InternetAddress(to));

                message.setSubject(subject);

				message.setSentDate(new Date());

                message.setText(content);

				System.out.println("Sending start");
				Transport.send(message);
                System.out.println("Sent message successfully....");
            }catch (MessagingException mex) {
                mex.printStackTrace();
            }

            // ===================================================================
        }
	}
}
