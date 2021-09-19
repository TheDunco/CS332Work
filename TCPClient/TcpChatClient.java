/*
* Duncan Van Keulen
* 9/20/2021
* Advanced Computer Networking
* Homework 1: TCP Chat Client
* 
* Implements a well-known chat client style that listens to a server and sends messages to it
*
* Derived from the documented examples and my CS232 Operating Systems Ceasar Cipher client/server from last year
* 
* Usage: java TcpChatClient.java --server <server = localhost> --port <port = 12345> --name <display name = Anon>
*/

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.Socket;
import java.util.Scanner;

/*
* Initial Ceasar Cipher code based on these two examples...
* https://www.infoworld.com/article/2853780/socket-programming-for-scalable-systems.html
* https://docs.oracle.com/javase/tutorial/networking/sockets/readingWriting.html
*/

public class TcpChatClient {

    Socket socket;
    PrintStream socketWriter;
    Scanner userInput;
    ServerListener myListener;
    
    Boolean verbose;
    String displayName;
    String host;
    Integer port;
    
    public static void main (String[] args) {
        parseArgsAndRunClient(args);
    }
    
    public static void displayUsageErrorMessage() {
        Util.fault(
            "Usage: java TcpChatClient --server <host> -- port <port>\n" +
            "Also supported are --verbose, --name <dispaly name>\n"
        );
    }
    
    
    private static void parseArgsAndRunClient(String[] args) {
        // init local variables to be passed into TcpChatClient constructor
        Boolean verbose = false;
        String displayName = "Anon";
        String host = "localhost";
        Integer port = 12345;
    
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
                        break;
                        
                    case "--name":
                    case "-n":
                        displayName = args[i+1];
                        break;
                
                    default:
                        break;
                }
            }
            catch(Exception e) {
                displayUsageErrorMessage();
            }
        }
        
        // create chat client and run it
        new TcpChatClient(host, port, verbose, displayName).run();
    }

    public TcpChatClient(String host, int port, Boolean verbose, String displayName) {
        try {
            this.verbose = verbose;
            this.displayName = displayName;
            
            if(this.verbose) Util.println("Host: " + host + "\nPort: " + port);
            
            // Initialize socket
            this.socket = new Socket(host, port);
            if (this.verbose) Util.println("TCP connected");

            // Initialize printstream to write to socket
            this.socketWriter = new PrintStream( socket.getOutputStream() );

            // Initialize a scanner to grab input from the user
            this.userInput = new Scanner(System.in);
        } 
        catch(java.net.ConnectException ce) {
            Util.fault("Could not connect to server!");
        }
        catch(IOException ioe) {
            Util.fault("Socket or IO error!");
        }
        catch(Exception e) {
            Util.fault("Unknown error!");
        }
    }

    // this is basically the "main" method of the chat client class
    public void run() {
        try {
            
            Util.println("Welcome, " + displayName + ", to the Chat Client!\n");
                
            this.startListeningToServer();
            
            this.readInAndSendUntilQuit();
    
            this.closeAll();

            // exit normally
            System.exit(0);
        }
        catch (Exception e) {
            Util.fault("Error running program");
        }
    }

    private void readInAndSendUntilQuit() {
        
        String message = "";

        while(true) {
            // get input from user
            message = this.userInput.nextLine();

            if(message.equals(".")) { break; }

            if(this.verbose) Util.println("Sending message...");
            // send message to server
            socketWriter.println("<" + this.displayName + "> says: " + message);
        } 

        return;
    }
    
    private void startListeningToServer() {
        this.myListener = new ServerListener(this, this.socket);
        this.myListener.start();
    }
    
    // Close all open sockets/readers/writers to avoid leaks
    private void closeAll() {
        try {
            this.userInput.close();
            this.socketWriter.close();
            this.socket.close();
            this.myListener.close();
        }
        catch (Exception e) {
            Util.fault("Closing failed");
        }
    }
}

// This example was useful in figuring out how to start a new thread and that I needed to in the first place
// https://www.codejava.net/java-se/networking/how-to-create-a-chat-console-application-in-java-using-socket
class ServerListener extends Thread {
    private TcpChatClient client;
    private BufferedReader socketReader;
    
    public ServerListener(TcpChatClient client, Socket socket) {
        this.client = client;
        
        try {
            this.socketReader = new BufferedReader( new InputStreamReader( socket.getInputStream() ));
        }
        catch (IOException ioe) {
            Util.fault("Error getting socket input stream");
        }
    }
    
    @Override
    public void run() {
        while (true) {
            try {
                // read message from server
                String serverMsg = this.readMessagesFromServer();
                if (this.client.verbose) Util.println("Message recieved...");
                
                // null message (repeatedly) means we got disconnected from server
                if (serverMsg == null) {
                    Util.fault("Server connection closed!");
                }
                
                Util.println(serverMsg);
            }
            catch (Exception e) {
                Util.fault("Error reading messages from server");
            }
        }
    }
    
    private String readMessagesFromServer() {
        try {
            // read response from server
            String rsp = this.socketReader.readLine();
            return rsp;
        }
        catch (IOException e) { 
            Util.fault("Error reading messages from server");
        }
        return ""; // to appease java
    }
    
    public void close() {
        try {
            this.socketReader.close();
            this.close();
        }
        catch (IOException ioe) {
            Util.fault("Error closing socket");
        }
    }
}

// utility print functions to save my fingers and improve readability
class Util {
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
