/*
Duncan Van Keulen
9/20/2021
Advanced Computer Networking
Homework 1
TCP Chat Client

Derived from the examples listed and the CS232 Operating Systems Ceasar Cipher client/server
*/

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.Socket;
import java.util.Scanner;

/*
Based on these two examples...
https://www.infoworld.com/article/2853780/socket-programming-for-scalable-systems.html
https://docs.oracle.com/javase/tutorial/networking/sockets/readingWriting.html
*/

public class TcpChatClient {

    Socket socket;
    PrintStream socketWriter;
    BufferedReader socketReader;
    Scanner userInput;

    public static void main (String[] args) {
        if (args.length == 2) {
            TcpChatClient me = new TcpChatClient(args[0], Integer.parseInt(args[1]));
            me.run();
        }
        else {
            System.out.println("Usage: java TcpChatClient <host> <port>");
            System.exit(0);
        }
    }

    public TcpChatClient(String host, int port) {
        try {

            System.out.println("Host: " + host + "\nPort: " + port);
            // Initialize socket
            this.socket = new Socket(host, port);

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
            System.out.print("Welcome to the Chat Client! \nEnter your username: ");
    
            this.loopUntilQuit();
    
            this.closeAll();

            // exit normally
            System.exit(0);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loopUntilQuit() {
        
        String message = "";
        String response;

        // TODO: will have to update in order for it to display other people's updates periodically as well
        while(true) {
            // get input from user
            System.out.print("Message: ");
            message = this.userInput.nextLine();

            if(message.equals(".")) { break; }

            // send message to server
            socketWriter.println(message);

            response = readMessagesFromServer();
            
            // display server response
            // TODO: Assuming the server appends the username to the message?
            System.out.println(response);
        } 

        return;
    }
    
    private String readMessagesFromServer() {
        try {
            // read response from server
            String rsp = socketReader.readLine();
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
}