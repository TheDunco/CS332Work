/*
* Duncan Van Keulen
* 10/31/2021
* Advanced Computer Networking
* Homework 4: Reliable FTP Using UDP
*
* Receiver client will receive a file from the sender
* Implements a reliable file transfer protocol using UDP
* 
* Usage: java Receiver.java --port <port> --output <filename>
*/

import java.io.IOException;
import java.io.RandomAccessFile;
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
    public final static int HEADERSIZE = 13;
    public final static int FULLPCKTSIZE = HEADERSIZE + PAYLOADSIZE;
    public final static int ACKSIZE = 8;
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
        int ackPacketID = 0;
        try {
            // reference: https://stackoverflow.com/questions/10556829/sending-and-receiving-udp-packets
            byte[] receiveData = new byte[FULLPCKTSIZE];
            DatagramPacket receivePacket;
            ByteBuffer ack = ByteBuffer.wrap(new byte[ACKSIZE]);
            boolean oneTime = true;
            int lastPacket = 0;
            int bytesAcked = 0;
            
            try {
                // remove whatever old file there may have been
                File clearFile = new File(this.filename);
                clearFile.delete();
            }
            // allow creation of new file
            catch (Exception e) {}
            
            try (RandomAccessFile fout = new RandomAccessFile(this.filename, "rw")) {
                
                while (true) {
                    // receive data
                    receivePacket = new DatagramPacket(receiveData, FULLPCKTSIZE);
                    Udp.receive(receivePacket);
                    
                    PrintUtil.debugln("Got data", this.verbose);
                    // write out only the data we got in the payload to the file
                    byte[] payload = Arrays.copyOfRange(receivePacket.getData(), HEADERSIZE, receivePacket.getLength());
                    ByteBuffer header = ByteBuffer.wrap(Arrays.copyOfRange(receivePacket.getData(), 0, HEADERSIZE));
                    
                    int connectionId = header.getInt();
                    int bytesReceived = header.getInt();
                    int packetID = header.getInt();
                    int toAck = header.get();
                    
                    // // drop packet 95 (for testing)
                    // if (oneTime && packetID == 95) {
                    //     oneTime = false;
                    //     continue;
                    // }
                    
                    // this is the packet we were expecting
                    if (lastPacket == packetID) {
                        lastPacket++;
                        if (toAck == 1) {
                            ackPacketID = packetID;
                            bytesAcked = bytesReceived;
                            PrintUtil.debugln(String.format("bytesAcked: %d, bytesReceived: %d", bytesAcked, bytesReceived), this.verbose);
                        }
                        fout.seek(bytesReceived);
                    }
                    // reset if sender is still sending old packets
                    else if (packetID < lastPacket) {
                        lastPacket = packetID;
                        fout.seek(bytesReceived);
                        continue;
                    }
                    // receiver received wrong packet aka a packet was dropped
                    else {
                        PrintUtil.debugln("Dropping packet: not the expected packet", this.verbose);
                        // send ack but don't write out to the file
                        PrintUtil.debugln(
                            String.format("connectionId: %d, bytesReceived: %d, packetNum: %d, toAck: %d",
                                           connectionId,     bytesReceived,     packetID,     toAck), 
                            this.verbose
                        );
                        
                        lastPacket = ackPacketID;
                        fout.seek(bytesAcked);
                        
                        if (toAck == 1) {
                            SendAck(toAck, ack, connectionId, ackPacketID, receivePacket);
                        }
                        
                        continue;
                    }
                    
                    PrintUtil.debugln("\nGood packet, writing to file...", this.verbose);
                    
                    SendAck(toAck, ack, connectionId, ackPacketID, receivePacket);
                    
                    fout.write(payload);
                    
                    // print out the parts of the header
                    PrintUtil.debugln(
                        String.format("connectionId: %d, bytesReceived: %d, packetNum: %d, toAck: %d\n\n",
                        connectionId,     bytesReceived,     packetID,     toAck), 
                        this.verbose
                    );
                    
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
    
    private void SendAck(int toAck, ByteBuffer ack, int connectionId, int numPacketsReceived, DatagramPacket receivePacket) {
        if (toAck == 1) {
            ack.clear();
            ack.putInt(connectionId);
            ack.putInt(numPacketsReceived);
            
            for (byte b : ack.array()) {
                PrintUtil.debug("" + b + " ", this.verbose);
            }
            PrintUtil.debugln(this.verbose);
            
            // send ack
            PrintUtil.debugln("Sending ack", this.verbose);
            UdpSend(ack.array(), receivePacket.getAddress(), receivePacket.getPort());
        }
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
    
    public static void debugln(boolean debug) {
        if (debug) {
            System.out.println();
            System.out.flush();
        }
    }
    
    public static void debug(String message, boolean debug) {
        if (debug)
            PrintUtil.print(message);
    }
    
    public static void println(String message) {
        System.out.println(message);
        System.out.flush();
    }
    
    public static void println() {
        System.out.println();
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
