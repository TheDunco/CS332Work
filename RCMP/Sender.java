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
import java.util.Arrays;
import java.util.Collections;
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
        }
        catch (SocketException se) {
            PrintUtil.debugln("Error opening UDP socket", this.verbose);
            PrintUtil.exception(se, this.verbose);
            PrintUtil.fault();
        }
    }
        
    public static void displayUsageErrorMessage() {
        PrintUtil.fault(
            "Usage: java Sender.java --hostname <ip address/hostname> --port <port> --filename <file name>\n" +
            "Also supported are -v (--verbose) and --progress (-p) to show the progress bar"
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
                File file = new File(this.filename);
                this.fileSize = file.length();
                
                // number of packets will be size of file / PACKETSIZE rounded up
                int numPackets = (int)(((double)this.fileSize / (double)PACKETSIZE) + 0.5);
                PrintUtil.debugln("Excpected # of packets: " + numPackets, this.verbose);
                
                FileInputStream fin = new FileInputStream(file);
                
                byte[] chunk = new byte[PACKETSIZE];
                int chunkLen = 0;
                int amountSent = 0;
                while (true) {
                    chunkLen = fin.read(chunk); // read in a chunk of the file
                    
                    if (chunkLen == -1)  break; // we've reached the end of the file
                    
                    // send over however much we read in
                    UdpSend(Arrays.copyOfRange(chunk, 0, chunkLen)); 
                    // PrintUtil.debugln("" + chunkLen, this.verbose);
                    
                    // update the progress bar
                    amountSent += chunkLen;
                    PrintUtil.printProgress(startTime, this.fileSize, amountSent, this.progress);
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
                PrintUtil.debugln("Done sending file?", this.verbose);
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


