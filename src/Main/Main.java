package Main;

import com.sun.net.httpserver.HttpServer;

import Main.Java.ActiveMqSender;
import Main.Java.DatabaseManager;
import Main.Java.MessageRetrievalHandler;

import java.io.IOException;
import java.net.InetSocketAddress;

public class Main {
    public static void main(String[] args) throws IOException {
       
        DatabaseManager.initialize();

        // Create and start the HTTP server
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);

        server.createContext("/processQueue", new ActiveMqSender());
        server.createContext("/retrieveMessages", new MessageRetrievalHandler());
        server.setExecutor(null);
        System.out.println("HTTP Server is listening on port 8080");
        server.start();
}
}