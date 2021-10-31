/*
* Duncan Van Keulen
* 10/31/2021
* Advanced Computer Networking
* Homework 4: Reliable FTP Using UDP
*
* Sender client will send a file to the receiver
* Implements a reliable file transfer protocol using UDP
* 
* Usage: java Sender.java --destination <ip address/hostname> --port <port> --filename <file name>
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
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.Random;
import java.nio.ByteBuffer;
import java.io.RandomAccessFile;

public class Sender {
    public final static int PAYLOADSIZE = 1450;
    public final static int HEADERSIZE = 13;
    public final static int FULLPCKTSIZE = HEADERSIZE + PAYLOADSIZE;
    public final static int ACKSIZE = 8;
    private boolean verbose = false;
    private boolean progress = false;
    private String filename = "";
    private Integer port = 22222;
    private DatagramSocket Udp;
    private InetAddress destination;
    private long fileSize = 0;
    
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
            this.Udp = new DatagramSocket();
            this.Udp.setSoTimeout(1000);
        }
        catch (SocketException se) {
            PrintUtil.debugln("Error opening UDP socket", this.verbose);
            PrintUtil.exception(se, this.verbose);
            PrintUtil.fault();
        }
    }
        
    public static void displayUsageErrorMessage() {
        PrintUtil.fault(
            "Usage: java Sender.java --destination <ip address/hostname> --port <port> --filename <file name>\n" +
            "Also supported are -v (--verbose) and -prog (--progress) to show the progress bar"
        );
    }
    
    /* Splits up a file into packets and sends those packets as it goes
    * @param String file: The file data to send
    */
    public void sendFile() {
        try {
            
            // start time for progress bar estimation
            long startTime = System.currentTimeMillis();
            
            // send the file in chunks of size packetSize
            // file reading based on https://stackoverflow.com/questions/11110153/reading-file-chunk-by-chunk
            try {
                // get the file info
                File file = new File(this.filename);
                this.fileSize = file.length();
                
                // number of packets will be size of file / PACKETSIZE rounded up
                int totalNumPackets = (int)(((double)this.fileSize / (double)PAYLOADSIZE) + 0.5);
                PrintUtil.debugln("Excpected # of packets: " + totalNumPackets, this.verbose);
                PrintUtil.debugln("File size: " + file.length(), this.verbose);
                
                // init vars for reading in, sending, and receiving
                
                // ack
                RandomAccessFile fin = new RandomAccessFile(file, "r");
                byte[] ackBuffer = new byte[ACKSIZE];
                ByteBuffer ackWrapper = ByteBuffer.wrap(ackBuffer);
                DatagramPacket ackPacket;
                
                // payload/header
                byte[] chunk = new byte[PAYLOADSIZE];
                byte[] pckt = new byte[FULLPCKTSIZE];
                ByteBuffer packet = ByteBuffer.wrap(pckt);
                
                // tracking/header vars
                int chunkLen = 0;
                int writePosition = 0; // keeps track of where the receiver should write to
                int bytesAcked = 0;
                int ackPacketID = 0;
                int packetID = 0;
                int connectionId = new Random().nextInt(Integer.MAX_VALUE);
                int lastAckedPacket = 0;
                int gapCounter = 0;
                int gap = 0;
                byte toAck = 1;
                int packetsSentSinceReset = 0;
                boolean resend = false;
                boolean timedOut = false;
                int resendCount = 0;
                
                while (true) {
                    
                    if (resend)
                        toAck = 1;
                    else
                        toAck = (byte) (packetsSentSinceReset == gap ? 1 : 0);
                        
                    packet.clear();
                    
                    chunkLen = fin.read(chunk); // read in a chunk of the file
                    
                    if (chunkLen == -1)  break; // we've reached the end of the file
                    
                    // ack the last packet
                    if (packetID == totalNumPackets) {
                        toAck = 1;
                    }
                    
                    // put the header in the packet
                    packet.putInt(connectionId);
                    packet.putInt(writePosition);
                    packet.putInt(packetID);
                    packet.put(toAck);
                    // put chunk of file into packet
                    packet.put(Arrays.copyOfRange(chunk, 0, chunkLen));

                    // for (byte b : Arrays.copyOfRange(packet.array(), 0, chunkLen)) {
                    //     PrintUtil.debug("" + b + ' ', this.verbose);
                    // }
                    
                    // send over however much we read in
                    UdpSend(Arrays.copyOfRange(packet.array(), 0, chunkLen + HEADERSIZE));
                    PrintUtil.debugln(String.format("%d", writePosition), this.verbose);
                    
                    if (toAck == 1) ackPacketID = packetID;
                    packetsSentSinceReset++;
                                        
                    // update the progress bar
                    writePosition += chunkLen;
                    PrintUtil.printProgress(startTime, this.fileSize, writePosition, this.progress);
                    
                    PrintUtil.debugln(String.format("toAck: %d, gap: %d, gapCounter: %d, pcktsSentSinceReset: %d, pcktID: %d, bytesInPayload %d, writePos: %d", toAck, gap, gapCounter, packetsSentSinceReset, packetID, chunkLen, writePosition), this.verbose);
                    
                    int rcvdConnId = 0;
                    // wait for an ack packet if there's still  more to
                    if (toAck == 1) {
                        // make sure that we receive an ack for our connection order to go on
                        while (rcvdConnId != connectionId) {
                            ackWrapper.clear();
                            
                            // receive the ack
                            PrintUtil.debugln("Waiting for ack", this.verbose);
                            try {
                                ackPacket = new DatagramPacket(ackBuffer, ACKSIZE);
                                this.Udp.receive(ackPacket);
                            }
                            // we've timed out waiting for a packet!
                            catch (SocketTimeoutException sTO) {
                                
                                PrintUtil.debugln("Socket timeout!", this.verbose);

                                timedOut = true;
                                
                                // reset the gap
                                gap = gapCounter = packetsSentSinceReset = 0;
                                
                                // we only know that we have sent over lastAckedPacket packets successfully, same with bytes
                                packetID = lastAckedPacket;
                                writePosition = bytesAcked;
                                
                                // we need to seek backwards in the file however may bytes we sent out but didn't get acked
                                if (!resend) { // we only need to do that once
                                    fin.seek(bytesAcked);
                                }
                                
                                // if the this isn't the first time we're resending this packet, increase the count.
                                if (resend)
                                    resendCount++;

                                resend = true;
                                
                                // exit program if receiver just isn't responding...
                                if (resendCount > 5 && lastAckedPacket == totalNumPackets) {
                                    PrintUtil.fault("File transfer success unknown");
                                }
                                
                                break; // go back to the top of the sending function to try again
                            }
    
                            // print out ack (debug)
                            PrintUtil.debug("Ack: ", this.verbose);
                            for (byte b : ackWrapper.array()) {
                                PrintUtil.debug("" + b + " ", this.verbose);
                            }
                            PrintUtil.debugln(this.verbose);
                            
                            // check if the connection id is ok
                            if (!(ackWrapper.getInt() == connectionId)) { // this gets BufferUnderflow exception even though there is stuff in the buffer.
                                PrintUtil.debugln("Ack packet not from same connection", this.verbose);
                                continue;
                            }
                            
                            // ackWrapper wraps ackBuffer
                            lastAckedPacket = ackWrapper.getInt();
                            
                            // check to make sure the last acked packet is the one we were expecting to receive an ack for
                            if (lastAckedPacket != ackPacketID) {
                                PrintUtil.debugln("Last acked packet not in order!", this.verbose);
                                gap = gapCounter = 0;
                                continue;
                            }
                            
                            // we've successfully acked this packet
                            bytesAcked = writePosition - chunkLen;
                            resend = false;
                            timedOut = false;
                            
                            // printing ack messes up progbar
                            if (!this.progress) PrintUtil.println("ACK " + (lastAckedPacket));
                            
                            if (lastAckedPacket == totalNumPackets) {
                                PrintUtil.fault("Done sending file", this.verbose);
                            }
                            
                            gapCounter++;
                            gap += gapCounter;
                            
                            break;
                        }
                    }
                    if (!timedOut) packetID++;
                }
                
                fin.close();
            }
            catch (FileNotFoundException fnfE) {
                PrintUtil.exception(fnfE, this.verbose);
            } 
            catch (IOException ioE) {
                PrintUtil.exception(ioE, this.verbose);
            }
            catch (Exception e) {
                PrintUtil.exception(e, this.verbose);
                PrintUtil.debugln("There was an error sending the file", this.verbose);
            }
        }
        catch (Exception e) {
            PrintUtil.println("There was an error sending a packet");
            PrintUtil.exception(e, this.verbose);
        }
        if (this.progress) PrintUtil.pad();
        PrintUtil.debugln("Done sending file", this.verbose);
    }
    
    private void UdpSend(byte[] buffer) {
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
    
    public static void fault(String message, boolean debug) {
        if (debug) {
            System.err.println(message);
            System.err.flush();
        }
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