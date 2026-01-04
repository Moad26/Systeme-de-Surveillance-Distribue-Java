package com.monitor.server.handler;

import com.monitor.model.Alert;
import com.monitor.server.storage.DataManager;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * TCP server for receiving alerts from agents.
 * Handles multiple clients with a thread pool.
 */
public class TcpAlertHandler implements Runnable {
    
    private final int port;
    private final DataManager dataManager;
    private volatile boolean running = true;
    private ServerSocket serverSocket;
    private final ExecutorService clientPool;
    
    public TcpAlertHandler(int port) {
        this.port = port;
        this.dataManager = DataManager.getInstance();
        this.clientPool = Executors.newCachedThreadPool();
    }
    
    @Override
    public void run() {
        try {
            serverSocket = new ServerSocket(port);
            System.out.println("[TcpAlertHandler] Listening on port " + port);
            
            while (running) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    clientPool.submit(() -> handleClient(clientSocket));
                } catch (IOException e) {
                    if (running) {
                        System.err.println("[TcpAlertHandler] Error accepting connection: " + e.getMessage());
                    }
                }
            }
            
        } catch (IOException e) {
            System.err.println("[TcpAlertHandler] Failed to start: " + e.getMessage());
        } finally {
            stop();
        }
    }
    
    /**
     * Handle a single client connection.
     */
    private void handleClient(Socket socket) {
        try (ObjectInputStream ois = new ObjectInputStream(socket.getInputStream())) {
            Alert alert = (Alert) ois.readObject();
            
            if (alert != null) {
                dataManager.addAlert(alert);
                System.out.println("[TcpAlertHandler] Received alert: " + alert);
            }
            
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("[TcpAlertHandler] Error handling client: " + e.getMessage());
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                // Ignore
            }
        }
    }
    
    /**
     * Stop the handler.
     */
    public void stop() {
        running = false;
        clientPool.shutdown();
        
        if (serverSocket != null && !serverSocket.isClosed()) {
            try {
                serverSocket.close();
            } catch (IOException e) {
                // Ignore
            }
        }
        
        System.out.println("[TcpAlertHandler] Stopped");
    }
}
