package server;

import smtp.SMTPReceiver;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Created by mmishak on 03/04/17.
 */
public class Server {
    public static void main(String[] args) throws UnknownHostException {
        new SMTPReceiver().start();
    }
}
