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
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;
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
    private boolean progress = false;
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
                    case "-f":
                        this.filename = args[i+1];
                        break;
                        
                    case "--destination":
                    case "--dest":
                    case "-d":
                    case "--hostname":
                    case "--host":
                    case "-h":
                        try {
                            this.destination = InetAddress.getByName(args[i+1]);
                        }
                        catch (UnknownHostException e) {
                            PrintUtil.exception(e, this.verbose);
                        }
                        break;
                        
                    case "--help":
                        displayUsageErrorMessage();
                        break;
                        
                    case "--progress":
                    case "-prog":
                    case "-bar":
                        this.progress = true;
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
            "Also supported are -v (--verbose) and --progress (-p) to show the progress bar"
        );
    }
    
    private void sendFile() {
        
        PrintUtil.debugln("Reading in file", this.verbose);
        
        String file = readInFile();
        
        PrintUtil.debugln("Sending file...", this.verbose);
        
        splitAndSend(file);
        
        PrintUtil.debugln("File sent", this.verbose);
    }
    
    // adapted from...
    // https://www.w3schools.com/java/java_files_read.asp
    private String readInFile() {
        StringBuilder file = new StringBuilder();
        
        // derived from example at https://mkyong.com/java/how-to-read-file-in-java-fileinputstream/
        try (FileInputStream fis = new FileInputStream(new File(this.filename))) {
            PrintUtil.debugln(this.filename, this.verbose);
            
            int content;
            // reads a byte at a time, if it reached end of the file, returns -1
            while ((content = fis.read()) != -1) {
                file.append(content);
            }
        }
        catch (FileNotFoundException e) {
            PrintUtil.exception(e, this.verbose);
            PrintUtil.fault("File not found!");
        }
        catch (Exception e) {
            PrintUtil.exception(e, this.verbose);
            PrintUtil.fault("There was an unknown error reading in the file");
        }
        
        PrintUtil.debugln("Read file complete", this.verbose);
        return file.toString();
    }
    
    /* Splits up a file into packets and sends those packets as it goes
    * @param String file: The file data to send
    */
    public void splitAndSend(String file) {
        try {
            // allocate a buffer to use for each packet
            byte buffer[] = new byte[PACKETSIZE];
            byte[] sendData = file.getBytes("UTF-8");
            
            int fIndex = 0; // our place in the file
            int pIndex = 0; // our place in the packet
            
            long total = sendData.length;
            long startTime = System.currentTimeMillis();
            
            while (pIndex < PACKETSIZE) {
                try {
                    if (fIndex == sendData.length - 1) {
                        // we have reached the end of the file, send what we have left and stop
                        UdpSend(buffer);
                        break;
                    }
                    if (pIndex == (PACKETSIZE) - 1) {
                        UdpSend(buffer);
                        
                        // clear buffer or the last packet will not have correct data in it
                        Arrays.fill(buffer, (byte)0);
                        
                        // reset our place in the packet to 0
                        pIndex = 0; 
                        continue;
                    }
                    buffer[pIndex] = sendData[fIndex];
                    pIndex++;
                    fIndex++;
                    PrintUtil.printProgress(startTime, total, fIndex + 1, this.progress);
                }
                catch (Exception e) {
                    PrintUtil.exception(e, this.verbose);
                    PrintUtil.debugln("Done sending file?", this.verbose);
                    break;
                }
            }
        }
        catch (Exception e) {
            PrintUtil.println("There was an error sending a packet");
            PrintUtil.exception(e, this.verbose);
        }
        PrintUtil.pad();
    }
    
    private void UdpSend(byte[] buffer) {
        PrintUtil.debugln("Attempting to send over UDP...", this.verbose);
        // make and send a DatagramPacket with our buffer
        // reference: https://stackoverflow.com/questions/10556829/sending-and-receiving-udp-packets
        try {
            this.Udp.send(
                new DatagramPacket(
                    buffer, buffer.length, this.destination, this.port
                )
            );
        } 
        catch (IOException ioe) {
            PrintUtil.exception(ioe, this.verbose);
        }
        PrintUtil.debugln("UDP message sent", this.verbose);
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


