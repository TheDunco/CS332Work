/*
* Duncan Van Keulen
* 9/20/2021
* Advanced Computer Networking
* Homework 1: TCP Chat Client
* 
* Derived from the examples listed and the CS232 Operating Systems Ceasar Cipher client/server
* 
* Usage: java TcpChatClient.java --server <server = localhost> --port <port = 12345> --name <display name>
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
    
    Boolean verbose = false;
    String displayName = "Anon";
    String host = "localhost";
    Integer port = 12345;
    
    public static void main (String[] args) {
        
        // we should have at least port number and host name
        // if (args.length >= 2) {
        parseArgsAndRunClient(args);
        // }
        // else {
        //     displayUsageMessageAndExit();
        // }
    }
    
    public static void displayUsageMessageAndExit() {
        Util.print(
            "Usage: java TcpChatClient --server <host> -- port <port>\n" +
            "Also supported are --verbose, --name <dispaly name>\n"
        );
        
        System.exit(0);
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
                displayUsageMessageAndExit();
            }
        }
        
        // create chat client and run it
        TcpChatClient client = new TcpChatClient(host, port, verbose, displayName);
        client.run();
    }

    public TcpChatClient(String host, int port, Boolean verbose, String displayName) {
        try {
            this.verbose = verbose;
            this.displayName = displayName;
            
            if(this.verbose) Util.println("Host: " + host + "\nPort: " + port);
            
            // Initialize socket
            this.socket = new Socket(host, port);
            if (this.verbose) Util.println("TCP connected");

            // Initialize in and out to read from and write to the socket
            this.socketWriter = new PrintStream( socket.getOutputStream() );

            // Initialize a scanner to grab input from the user
            this.userInput = new Scanner(System.in);
        } 
        catch(java.net.ConnectException ce) {
            Util.println("Could not connect to server!");
            System.exit(0);
        }
        catch(IOException ioe) {
            Util.println("Socket error!");
            System.exit(0);
        }
        catch(Exception e) {
            Util.println("Unknown error!");
            System.exit(0);
        }
    }

    public void run() {
        try {
            
            Util.println("Welcome, " + displayName + ", to the Chat Client!\n");
                
            this.listenToServer();
            
            this.readAndSendUntilQuit();
    
            this.closeAll();

            // exit normally
            System.exit(0);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void readAndSendUntilQuit() {
        
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
    
    private void listenToServer() {
        ServerListener myListener = new ServerListener(this, this.socket);
        myListener.start();
        // // needed to be cast to ServerListener instead of Thread
        // ((TcpChatClient.ServerListener) myListener).start();
    }
    
    // Close all open sockets/readers/writers to avoid leaks
    private void closeAll() {
        try {
            this.userInput.close();
            this.socketWriter.close();
            this.socket.close();
        }
        catch (Exception e) {
            System.out.println("Closing failed");
            e.printStackTrace();
        }
    }
}

class ServerListener extends Thread {
    private TcpChatClient client;
    public BufferedReader socketReader;
    
    public ServerListener(TcpChatClient client, Socket socket) {
        this.client = client;
        
        try {
            this.socketReader = new BufferedReader( new InputStreamReader( socket.getInputStream() ));
        }
        catch (IOException ioe) {
            if (client.verbose) Util.println("Error getting socket input stream");
        }
    }
    
    @Override
    public void run() {
        while (true) {
            try {
                // read message from server
                String serverMsg = this.readMessagesFromServer();
                if (this.client.verbose) Util.println("Message recieved...");
                
                Util.println(serverMsg);
            }
            catch (Exception e) {
                if(this.client.verbose) e.printStackTrace();
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
            // e.printStackTrace(); 
            // response = "error";
            // will get stackoverflow if truly broken
            if (this.client.verbose) Util.println("Error reading messages from server");
            readMessagesFromServer();
        }
        return "";
    }
    
    public void close() {
        try {
            this.socketReader.close();
        }
        catch (IOException ioe) {
            if (client.verbose) Util.println("Error closing socket");
        }
    }
}

// utility print functions to save my fingers and improve readability
class Util {
    static void println(String message) {
        System.out.println(message);
        System.out.flush();
    }

    static void print(String message) {
        System.out.print(message);
        System.out.flush();
    }    
}
