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
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.TimeUnit;
// Prof. Norman's Imports
import java.io.File;
import java.net.DatagramSocket;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.SocketException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.util.Random;
import java.nio.ByteBuffer;

public class Receiver {
    public final static int PAYLOADSIZE = 1450;
    public final static int HEADERSIZE = 12;
    public final static int FULLPCKTSIZE = HEADERSIZE + PAYLOADSIZE;
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
            // this.Udp.setReuseAddress(true); // this threw InvalidArgumentException
        }
        catch (SocketException se) {
            PrintUtil.debugln("Error opening UDP socket", this.verbose);
            PrintUtil.exception(se, this.verbose);
            PrintUtil.fault();
        }
    }
        
    public static void displayUsageErrorMessage() {
        PrintUtil.fault(
            "Usage: java Receiver.java --port <port> --output <outputfile name>\n" +
            "Also supported are -v (--verbose)"
        );
    }
    
    private void receiveFile() {
        
        PrintUtil.debugln("Receiving file...", this.verbose);
        try {
            // reference: https://stackoverflow.com/questions/10556829/sending-and-receiving-udp-packets
            byte[] receiveData = new byte[FULLPCKTSIZE];
            DatagramPacket receivePacket;
            byte[] ack = "ACK".getBytes();
            
            try (FileOutputStream fout = new FileOutputStream(this.filename)) {
                while (true) {
                    // receive data
                    receivePacket = new DatagramPacket(receiveData, FULLPCKTSIZE);
                    Udp.receive(receivePacket);
                    
                    PrintUtil.debugln("Got data, writing data to file", this.verbose);
                    // write out only the data we got in the payload to the file
                    byte[] payload = Arrays.copyOfRange(receivePacket.getData(), HEADERSIZE, receivePacket.getLength());
                    ByteBuffer header = ByteBuffer.wrap(Arrays.copyOfRange(receivePacket.getData(), 0, HEADERSIZE));
                    
                    int connectionId = header.getInt();
                    int bytesSent = header.getInt();
                    int packetNum = header.getInt();
                    
                    // print out the parts of the header
                    PrintUtil.debugln(
                        String.format("connectionId: %d, bytesSent: %d, packetNum: %d",
                                       connectionId,     bytesSent,     packetNum), 
                        this.verbose
                    );
                    
                    PrintUtil.pad();
                    
                    fout.write(payload);
                    fout.flush();
                    
                    // send ack
                    PrintUtil.debugln("Sending ack", this.verbose);
                    UdpSend(ack, receivePacket.getAddress(), receivePacket.getPort());
                    
                    // we've received the whole file if we get a packet that's smaller than packetsize
                    if (receivePacket.getLength() < FULLPCKTSIZE) {
                        break;
                    }
                }
                fout.close();
            }
    
            
        } catch (IOException e) {
            PrintUtil.exception(e, this.verbose);
        }
        
        PrintUtil.debugln("File received", this.verbose);
    }
    
    private void UdpSend(byte[] buffer, InetAddress address, int port) {
        // make and send a DatagramPacket with our buffer
        // reference: https://stackoverflow.com/questions/10556829/sending-and-receiving-udp-packets
        try {
            this.Udp.send(
                new DatagramPacket(
                    buffer, buffer.length, address, port
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
    
    public static void dbgpad(boolean debug) {
        if (debug)
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
        fault();
    }
    
    public static void fault() {
        System.exit(-1);
    }
    
    // Thanks a TON to Mike Shauneu on StackOverflow for providing this awesome code.
    // Note that the window size of the terminal needs to be big enough to fit the large progress bar.
    // I couldn't figure out how to make this smaller
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


