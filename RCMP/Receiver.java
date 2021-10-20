/*
* Duncan Van Keulen
* 10/31/2021
* Advanced Computer Networking
* Homework 4: Reliable FTP Using UDP
*
* Receiver client will receive a file from the sender
* 
* Usage: java Receiver.java --port <port = 12345> --verbose <verbose = false>
*/

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.LinkedList;
import java.util.List;
// Prof. Norman's Imports
import java.io.File;
import java.net.DatagramSocket;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.io.FileInputStream;


public class Receiver {
    public final static int packetSize = 1450;
    private boolean verbose = false;
    private String filename = "";
    Integer port = 22222;
    
    public static void main(String[] args) {
        Receiver receiver = new Receiver();
        receiver.parseArgs(args);
        receiver.receiveFile();
    }   
     
    private void parseArgs(String[] args) {
        for (int i = 0; i < args.length; i++) {
            try {
                switch (args[i]) {
                    case "--verbose":
                    case "-v":
                        this.verbose = true;
                        break;
                    
                    case "--port":
                    case "-p":
                        this.port = Integer.parseInt(args[i+1]);
                        break;
                        
                    case "--filename":
                    case "-fn":
                    case "-n":
                        this.filename = args[i+1];
                        break;
                
                    default:
                        break;
                }
            }
            catch(Exception e) {
                displayUsageErrorMessage();
            }
        }
    }
        
    public static void displayUsageErrorMessage() {
        Util.fault(
            "Usage: java Receiver.java --port <port> --filename <file name>\n" +
            "Also supported are --verbose\n"
        );
    }
    
    private void receiveFile() {
        if (this.verbose) 
        PrintUtil.println("Waiting to receive file");
    }
}

// utility print functions to save my fingers and improve readability
class PrintUtil {
    
    public static void debugln(String message, boolean debug) {
        if (debug) {
            PrintUtil.println(message);
        }
    }
    public static void debug(String message, boolean debug) {
        if (debug) {
            PrintUtil.print(message);
        }
    }
    public static void println(String message) {
        System.out.println(message);
        System.out.flush();
    }

    public static void print(String message) {
        System.out.print(message);
        System.out.flush();
    }
    
    public static void fault(String message) {
        System.err.println(message);
        System.err.flush();
        System.exit(0);
    }
}