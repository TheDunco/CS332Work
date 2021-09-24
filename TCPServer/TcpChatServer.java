/*
* Duncan Van Keulen
* 9/28/2021
* Advanced Computer Networking
* Homework 2: TCP Chat Server
* 
* Implements a well-known chat style server that sends messages between clients
*
* Derived from the documented examples and my CS232 Operating Systems Ceasar Cipher client/server from last year
* 
* Usage: java TcpChatServer.java --port <port = 12345> --verbose <verbose = false>
*/

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/*
Based heavily on this example...
https://www.infoworld.com/article/2853780/socket-programming-for-scalable-systems.html
*/

public class TcpChatServer extends Thread {
    private ServerSocket serverSocket;
    public int port = 12345;
    private boolean running = false;
    public boolean verbose = false;
    
    public List<ClientConnectionHandler> handlers;

    public TcpChatServer(String[] args) {
        // run through all args and parse out what we need to
        for (int i = 0; i < args.length; i++) {
            try {
                switch (args[i]) {
                    case "--verbose":
                    case "-v":
                        verbose = true;
                        break;
                    
                    case "--port":
                    case "-p":
                        this.port = Integer.parseInt(args[i+1]);
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

    public void startServer() {
        try {
            serverSocket = new ServerSocket(port);
            this.start();
        } 
        catch(java.net.ConnectException ce) {
            Util.fault("Socket could not connect");
        }
        catch(java.net.UnknownHostException ukh) {
            Util.fault("Unknown host!");
        }
        catch(IOException ioe) {
            Util.fault("Socket or IO error!");
        }
        catch(Exception e) {
            Util.fault("Unknown error!");
        }
    }

    public void stopServer() {
        running = false;
        this.interrupt();
    }
    
    public static void displayUsageErrorMessage() {
        Util.fault(
            "Usage: java TcpChatServer --port <port>\n" +
            "Also supported is --verbose\n"
        );
    }

    @Override
    public void run() {
        // Continually wait for new connections and spawn a request handler thread for every new connection
        this.running = true;
        while(this.running)
        {
            try
            {
                if (this.verbose) Util.println("Listening for a connection");

                // Call accept() to receive the next connection
                Socket socket = serverSocket.accept();
                
                if (this.verbose) Util.println("Got connection at port " + socket.getPort());
                
                // Pass the socket to the RequestHandler thread for processing
                ClientConnectionHandler requestHandler = new ClientConnectionHandler(socket, this);
                
                // add this new request handler to our list
                this.handlers.add(requestHandler);
                
                // start processing
                requestHandler.start();
            }
            catch (IOException e)
            {
                Util.fault("Error listening for or getting new connections");
            }
            catch (java.lang.NullPointerException npe) {
                Util.println("Error adding handler to list");
                npe.printStackTrace();
            }
            catch (Exception e) {
                Util.fault("Unknown error");
            }
        }
    }

    public static void main( String[] args )
    {
        TcpChatServer server;
        
        // We have 1-2 args...
        if (args.length < 1 * 2 || args.length > 2 * 2)
        {
            Util.fault( "Usage: SimpleSocketServer -port <port>" );
        }
        
        server = new TcpChatServer(args);
        
        if (server.verbose) Util.println("Starting server on port " + server.port);

        server.startServer();

        // Automatically shutdown in 8 hours
        try
        {
            Thread.sleep(28800000);
        }
        catch(Exception e)
        {
            server.stopServer();
            Util.fault("Error starting server");
        }

        server.stopServer();
        Util.fault("Server timeout");
    }
}

class ClientConnectionHandler extends Thread
{
    public Socket socket;
    private TcpChatServer server;
    public PrintWriter out;
    
    ClientConnectionHandler(Socket socket, TcpChatServer server)
    {
        this.socket = socket;
        this.server = server;
    }

    @Override
    public void run()
    {
        try
        {
            // Thanks to this tutorial for this date code...
            // https://www.edureka.co/blog/date-format-in-java/#:~:text=Creating%20A%20Simple%20Date%20Format,-A%20SimpleDateFormat%20is&text=String%20pattern%20%3D%20%22yyyy%2DMM,for%20formatting%20and%20parsing%20dates.
            // Date date = new Date();
            // SimpleDateFormat simpDate = new SimpleDateFormat("dd/MM hh:mm::ss");
            // String stringDate = simpDate.format(date);

            // if (server.verbose) Util.println("Received a connection " + stringDate);

            // Get input and output streams
            BufferedReader in = new BufferedReader(new InputStreamReader(this.socket.getInputStream()));
            out = new PrintWriter(this.socket.getOutputStream());

            try {
                
                out.println("Thank you for connecting!\n");
                out.flush();
                
                String inMsg = "";
                
                while (this.socket.isConnected()) {
                    try {
                        // read in a message from the client we're connected to
                        inMsg = in.readLine();
                        
                        if (inMsg == null) {
                            break;
                        }
                        
                        if (server.verbose) Util.println("--> Client on port " + socket.getPort() + " says: " + inMsg + "\nPassing msg to ");
                        
                        // broadcast message to all other connected clients
                        for (ClientConnectionHandler handler : server.handlers) {
                            if (handler != this) {
                                if (server.verbose) Util.println("--> " + handler.socket.getPort());
                                handler.out.println(inMsg);
                                handler.out.flush();
                            }
                        }
                    }
                    catch (Exception e) {
                        out.println("Whoops, server error! Sorry about that...");
                        out.flush();
                    }
                }
            }
            
            catch(NullPointerException e) { 
                
            }
            
            // remove ourself from the active handler list
            server.handlers.remove(this);
            
            if (server.verbose) Util.println("Server connection on port " + socket.getPort() + " has been closed");
            
            // Close the connection
            in.close();
            out.close();
            socket.close();
        }
        catch(Exception e)
        {
            Util.fault("Error processing messages");
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
