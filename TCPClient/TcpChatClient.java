/*
Duncan Van Keulen
9/20/2021
Advanced Computer Networking
Homework 1: TCP Chat Client

Derived from the examples listed and the CS232 Operating Systems Ceasar Cipher client/server

Usage: java TcpChatClient.java --server <server> --port <port> --name <display name>
*/

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.Socket;
import java.util.Scanner;

/*
Initial Ceasar Cipher code based on these two examples...
https://www.infoworld.com/article/2853780/socket-programming-for-scalable-systems.html
https://docs.oracle.com/javase/tutorial/networking/sockets/readingWriting.html


*/

public class TcpChatClient {

    Socket socket;
    PrintStream socketWriter;
    public BufferedReader socketReader;
    Scanner userInput;
    
    Boolean verbose = false;
    String displayName = "";
    String host = "";
    Integer port = 0;
    
    public static void displayUsageMessageAndExit() {
        print(
            "Usage: java TcpChatClient --server <host> -- port <port>\n" +
            "Also supported are --verbose, --name <dispaly name>"
        );
        
        System.exit(0);
    }
    
    // utility print functions to save my fingers and improve readability
    public static void println(String message) {
        System.out.println(message);
    }
    
    public static void print(String message) {
        System.out.print(message);
    }
    
    public static void main (String[] args) {
        
        // we should have at least port number and host name
        if (args.length >= 2) {
            parseArgsAndRunClient(args);
        }
        else {
            displayUsageMessageAndExit();
        }
    }
    
    private static void parseArgsAndRunClient(String[] args) {
        // init local variables to be passed into TcpChatClient constructor
        Boolean verbose = false;
        String displayName = "";
        String host = "";
        Integer port = 0;
        
    
        // run through all args and parse out what we need to
        // there are libraries out there for this but I didn't want to have to use them.
        for (int i = 0; i < args.length; i++) {
            try {
                switch (args[i]) {
                    case "--verbose":
                    case "-v":
                        verbose = true;
                        break;
                        
                    case "--server":
                    case "-s":
                        host = args[i+1];
                        break;
                    
                    case "--port":
                    case "-p":
                        port = Integer.parseInt(args[i+1]);
                        
                    case "--name":
                    case "-n":
                        displayName = args[i+1];
                        break;
                
                    default:
                        break;
                }
            }
            catch(Exception e) {
                displayUsageMessageAndExit();
            }
        }
        
        // create chat client and run it
        TcpChatClient client = new TcpChatClient(host, port, verbose, displayName);
        client.run();
    }

    public TcpChatClient(String host, int port, Boolean verbose, String displayName) {
        try {

            if(verbose) println("Host: " + host + "\nPort: " + port);
            
            // Initialize socket
            this.socket = new Socket(host, port);
            if (verbose) println("TCP connected");

            // Initialize in and out to read from and write to the socket
            this.socketWriter = new PrintStream( socket.getOutputStream() );
            this.socketReader = new BufferedReader( new InputStreamReader( socket.getInputStream() ));

            // Initialize a scanner to grab input from the user
            this.userInput = new Scanner(System.in);
        } 
        catch(Exception e) {
            e.printStackTrace();
        }
    }

    public void run() {
        try {
            println("Welcome to the Chat Client!\n");
            
            if (this.displayName == "") 
                displayName = this.getDisplayName();
                
            this.listenToServer();
            
            this.loopUntilQuit();
    
            this.closeAll();

            // exit normally
            System.exit(0);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private String getDisplayName() {
        print("Please enter your username: ");
        return this.userInput.nextLine();
    }

    private void loopUntilQuit() {
        
        String message = "";

        while(true) {
            // get input from user
            print("Message: ");
            message = this.userInput.nextLine();

            if(message.equals(".")) { break; }

            // send message to server
            socketWriter.println(message);
        } 

        return;
    }
    
    private void listenToServer() {
        Thread myListener = new ServerListener(this);
        // needed to be cast to ServerListener instead of Thread
        ((TcpChatClient.ServerListener) myListener).listen();
    }
    
    // Close all open sockets/readers/writers to avoid leaks
    private void closeAll() {
        try {
            this.userInput.close();
            this.socketReader.close();
            this.socketWriter.close();
            this.socket.close();
        }
        catch (Exception e) {
            System.out.println("Closing failed");
            e.printStackTrace();
        }
    }
    
    private class ServerListener extends Thread {
        private TcpChatClient client;
        
        public ServerListener(TcpChatClient client) {
            this.client = client;
        }
        
        public void listen() {
            while (true) {
                try {
                    println(readMessagesFromServer());
                }
                catch (Exception e) {
                    if(client.verbose) e.printStackTrace();
                }
            }
        }
        
        private String readMessagesFromServer() {
            try {
                // read response from server
                String rsp = client.socketReader.readLine();
                return rsp;
            }
            catch (IOException e) { 
                // e.printStackTrace(); 
                // response = "error";
                // will get stackoverflow if truly broken
                readMessagesFromServer();
            }
            return "";
        }
        
    }
}
