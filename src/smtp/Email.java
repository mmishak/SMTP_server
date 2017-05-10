package smtp;

import org.json.simple.JSONObject;

import java.io.*;

public class Email {

    // Директория в которую будем сохранять письма для локального домена
    private static final String MAIL_DIR = "/home/mmishak/mail/";

    // Ключи для сохранения полей в JSON формат
    private static final String FROM_KEY = "from";
    private static final String TO_KEY = "to";
    private static final String SUBJECT_KEY = "subject";
    private static final String TIMESTAMP_KEY = "timestamp";
    private static final String CONTENT_KEY = "content";


    // Поля письма
    private String from;
    private String to;
    private String subject;
    private String timestamp;
    private String content;

    public Email(String from, String to, String subject, String timestamp, String content) {
        this.from = from;
        this.to = to;
        this.subject = subject;
        this.timestamp = timestamp;
        this.content = content;
    }

    public String getFrom() {
        return from;
    }

    public String getTo() {
        return to;
    }

    public String getSubject() {
        return subject;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public String getContent() {
        if (content != null) {
            return content;
        } else {
            return "";
        }
    }

    public void save() {

        // Упаковываем все поля в JSONObject
        JSONObject jsonObject = new JSONObject();
        jsonObject.put(FROM_KEY, from);
        jsonObject.put(TO_KEY, to);
        jsonObject.put(SUBJECT_KEY, subject);
        jsonObject.put(TIMESTAMP_KEY, timestamp);
        jsonObject.put(CONTENT_KEY, content);

        // Получаем адрес получателя
        String accaunt = getAccaunt();

        try {
            // создаем папку получателя, если она отсутствует
            File accauntDir = new File(new StringBuilder()
                    .append(MAIL_DIR)
                    .append(accaunt).append("/")
                    .toString());

            if (!accauntDir.exists())
                accauntDir.mkdir();


            // создаем новый файл, в который сохраним письмо
            File newEmail = new File(new StringBuilder()
                    .append(MAIL_DIR)
                    .append(accaunt).append("/")
                    .append(from).append(" ")
                    .append(timestamp)
                    .toString());

            if (!newEmail.exists())
                newEmail.createNewFile();

            // Записываем наш JSONObject в созданный файл
            PrintWriter pw = new PrintWriter(newEmail);

            pw.print(jsonObject.toString());
            pw.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String getAccaunt() {
        if (to.contains("<"))
            return to.substring(to.indexOf("<") + 1, to.indexOf(">"));
        else
            return to.substring("To: ".length());
    }
}
