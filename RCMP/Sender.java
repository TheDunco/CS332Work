/*
* Duncan Van Keulen
* 10/31/2021
* Advanced Computer Networking
* Homework 4: Reliable FTP Using UDP
*
* Sender client will receive a file from the sender
* 
* Usage: java Sender.java java Sender.java --hostname <ip address/hostname> --port <port> --filename <file name>
*/

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
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



public class Sender {
    public final static int PACKETSIZE = 1450;
    private boolean verbose = false;
    private String filename = "";
    private Integer port = 22222;
    private DatagramSocket Udp;
    private InetAddress destination;
    
    public static void main(String[] args) {
        Sender sender = new Sender();
        sender.parseArgs(args);
        sender.initUdp();
        sender.sendFile();
    }
    
    private void parseArgs(String[] args) {
        // need port, dest, file * 2 for flags
        if (args.length < 6) {
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
                        
                    case "--filename":
                    case "--file":
                    case "-fn":
                    case "-n":
                        this.filename = args[i+1];
                        break;
                        
                    case "--destination":
                    case "--dest":
                    case "-d":
                        this.destination = InetAddress.getByName(args[i+1]);
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
            "Usage: java Sender.java --hostname <ip address/hostname> --port <port> --filename <file name>\n" +
            "Also supported are -v (--verbose)"
        );
    }
    
    private void sendFile() {
        
        PrintUtil.debugln("Reading in file", this.verbose);
        
        String file = readInFile();
        
        splitAndSend(file);
        
        PrintUtil.debugln("Sending file...", this.verbose);
    }
    
    // adapted from...
    // https://www.w3schools.com/java/java_files_read.asp
    private String readInFile() {
        StringBuilder file = new StringBuilder();
        
        try {
            File localFile = new File(this.filename);
            Scanner myReader = new Scanner(localFile);
            
            // read in entire file into string
            while (myReader.hasNextLine()) {
                file.append(myReader.nextLine());
            }
            
            myReader.close();
        }
        catch (FileNotFoundException e) {
            PrintUtil.exception(e, this.verbose);
            PrintUtil.fault("File not found!");
        }
        catch (Exception e) {
            PrintUtil.exception(e, this.verbose);
            PrintUtil.fault("There was an unknown error reading in the file");
        }
        
        return file.toString();
    }
    
    public void splitAndSend(String file) {
        try {
            // allocate a buffer to use for each packet
            byte buffer[] = new byte[PACKETSIZE];
            byte[] sendData = file.getBytes("UTF-8");
            
            // for each character in the file
            for (int i = 0; i < PACKETSIZE; i++) {
                if (i == (PACKETSIZE - 1)) {
                    // make and send a DatagramPacket with our buffer
                    // reference: https://stackoverflow.com/questions/10556829/sending-and-receiving-udp-packets
                    this.Udp.send(
                        new DatagramPacket(
                            buffer, buffer.length, this.destination, this.port
                        )
                    );
                    
                    // TODO: clear buffer or the last packet will not have correct data in it
                    i = 0;
                    continue;
                }
                buffer[i] = sendData[i]; // TODO: This is getting an index error! at 50!
            }
        }
        catch (Exception e) {
            PrintUtil.println("There was an error sending a packet");
            PrintUtil.exception(e, this.verbose);  
        }
    }
}


// utility print functions to save my fingers and improve readability
class PrintUtil {
    
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
}
