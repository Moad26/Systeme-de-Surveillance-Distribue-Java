package com.monitor.agent.net;

import com.monitor.model.Alert;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.Socket;

/**
 * Sends alerts via TCP for guaranteed delivery.
 * Opens a new connection for each alert.
 */
public class TcpClient {
    
    private final String serverHost;
    private final int serverPort;
    
    public TcpClient(String serverHost, int serverPort) {
        this.serverHost = serverHost;
        this.serverPort = serverPort;
        System.out.println("[TcpClient] Initialized, will send alerts to " + serverHost + ":" + serverPort);
    }
    
    /**
     * Send an alert to the server via TCP.
     * @param alert The alert to send
     */
    public void sendAlert(Alert alert) {
        try (Socket socket = new Socket(serverHost, serverPort);
             ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream())) {
            
            oos.writeObject(alert);
            oos.flush();
            System.out.println("[TcpClient] Sent alert: " + alert);
            
        } catch (IOException e) {
            System.err.println("[TcpClient] Failed to send alert: " + e.getMessage());
        }
    }
}
