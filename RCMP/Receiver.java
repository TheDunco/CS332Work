/*
* Duncan Van Keulen
* 10/31/2021
* Advanced Computer Networking
* Homework 4: Reliable FTP Using UDP
*
* Receiver client will receive a file from the sender
* 
* Usage: java Receiver.java --port <port> --output <filename>
*/

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;

// Prof. Norman's Imports
import java.io.File;
import java.net.DatagramSocket;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

public class Receiver {
    public final static int PACKETSIZE = 1450;
    private boolean verbose = false;
    private String filename = "";
    private Integer port = 22222;
    private DatagramSocket Udp;
    
    public static void main(String[] args) {
        Receiver receiver = new Receiver();
        receiver.parseArgs(args);
        receiver.initUdp();
        receiver.receiveFile();
    }
    
    private void parseArgs(String[] args) {
        // need port, outfile * 2 for flags
        if (args.length < 4) {
            displayUsageErrorMessage();
        }
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
                        
                    case "--output":
                    case "-out":
                    case "--filename":
                    case "--file":
                    case "-fn":
                    case "-n":
                    case "-f":
                        this.filename = args[i+1];
                        break;
                        
                    case "--help":
                        displayUsageErrorMessage();
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
    
    public void initUdp() {
        try {
            this.Udp = new DatagramSocket(this.port);
        }
        catch (SocketException se) {
            PrintUtil.fault("Error opening UDP socket");
        }
    }
        
    public static void displayUsageErrorMessage() {
        PrintUtil.fault(
            "Usage: java Receiver.java --port <port> --output <outputfile name>\n" +
            "Also supported are -v (--verbose)"
        );
    }
    
    private void receiveFile() {
        
        PrintUtil.debugln("Reading in file", this.verbose);
        try {
            byte[] receiveData = new byte[PACKETSIZE];
            byte[] response = new byte[16];
    
            while (true) {
                DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                Udp.receive(receivePacket);
                String rcvdData = new String(receivePacket.getData());
                PrintUtil.debugln("RECEIVED: " + rcvdData, this.verbose);
                
                // InetAddress IPAddress = receivePacket.getAddress();
                // String sendString = "A C K";
                // response = sendString.getBytes();
                // DatagramPacket sendPacket = new DatagramPacket(response, response.length, IPAddress, port);
                // Udp.send(sendPacket);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        PrintUtil.debugln("File sent", this.verbose);
    }
    
    private void UdpSend(byte[] buffer, InetAddress address) {
        // make and send a DatagramPacket with our buffer
        // reference: https://stackoverflow.com/questions/10556829/sending-and-receiving-udp-packets
        try {
            this.Udp.send(
                new DatagramPacket(
                    buffer, buffer.length, address, this.port
                )
            );
        } 
        catch (IOException ioe) {
            PrintUtil.exception(ioe, this.verbose);
        }
    }
}


// utility print functions to save my fingers and improve readability
class PrintUtil {
    
    public static void flush() {
        System.out.flush();
    }
    
    public static void pad() {
        System.out.println("\n");
    }
    
    public static void exception(Exception e, boolean debug) {
        if (debug)
            e.printStackTrace();
    }
    
    public static void debugln(String message, boolean debug) {
        if (debug)
            PrintUtil.println(message);
    }
    
    public static void debug(String message, boolean debug) {
        if (debug)
            PrintUtil.print(message);
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
    
    // Thanks a TON to Mike Shauneu on StackOverflow for providing this awesome code.
    // https://stackoverflow.com/questions/1001290/console-based-progress-in-java
    // https://stackoverflow.com/users/4503311/mike-shauneu
    public static void printProgress(long startTime, long total, long current, boolean debug) {
        if (debug) {
            long eta = current == 0 ? 0 : 
                (total - current) * (System.currentTimeMillis() - startTime) / current;
        
            String etaHms = current == 0 ? "N/A" : 
                    String.format("%02d:%02d:%02d", TimeUnit.MILLISECONDS.toHours(eta),
                            TimeUnit.MILLISECONDS.toMinutes(eta) % TimeUnit.HOURS.toMinutes(1),
                            TimeUnit.MILLISECONDS.toSeconds(eta) % TimeUnit.MINUTES.toSeconds(1));
        
            StringBuilder string = new StringBuilder(140);   
            int percent = (int) (current * 100 / total);
            string
                .append('\r')
                .append(String.join("", Collections.nCopies(percent == 0 ? 2 : 2 - (int) (Math.log10(percent)), " ")))
                .append(String.format(" %d%% [", percent))
                .append(String.join("", Collections.nCopies(percent, "=")))
                .append('>')
                .append(String.join("", Collections.nCopies(100 - percent, " ")))
                .append(']')
                .append(String.join("", Collections.nCopies(current == 0 ? (int) (Math.log10(total)) : (int) (Math.log10(total)) - (int) (Math.log10(current)), " ")))
                .append(String.format(" %d/%d, ETA: %s", current, total, etaHms));
        
            System.out.print(string);
        }
    }
}


