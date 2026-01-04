package com.monitor.server.handler;

import com.monitor.model.Metric;
import com.monitor.server.storage.DataManager;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

/**
 * UDP listener for receiving metrics from agents.
 * Runs in its own thread.
 */
public class UdpListener implements Runnable {
    
    private static final int BUFFER_SIZE = 8192;
    
    private final int port;
    private final DataManager dataManager;
    private volatile boolean running = true;
    private DatagramSocket socket;
    
    public UdpListener(int port) {
        this.port = port;
        this.dataManager = DataManager.getInstance();
    }
    
    @Override
    public void run() {
        try {
            socket = new DatagramSocket(port);
            System.out.println("[UdpListener] Listening on port " + port);
            
            byte[] buffer = new byte[BUFFER_SIZE];
            
            while (running) {
                try {
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    socket.receive(packet);
                    
                    // Deserialize the metric
                    Metric metric = deserialize(packet.getData(), packet.getLength());
                    
                    if (metric != null) {
                        dataManager.addMetric(metric);
                        System.out.println("[UdpListener] Received: " + metric);
                    }
                    
                } catch (IOException e) {
                    if (running) {
                        System.err.println("[UdpListener] Error receiving packet: " + e.getMessage());
                    }
                } catch (ClassNotFoundException e) {
                    System.err.println("[UdpListener] Error deserializing metric: " + e.getMessage());
                }
            }
            
        } catch (IOException e) {
            System.err.println("[UdpListener] Failed to start: " + e.getMessage());
        } finally {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        }
    }
    
    /**
     * Deserialize bytes to Metric object.
     */
    private Metric deserialize(byte[] data, int length) throws IOException, ClassNotFoundException {
        try (ByteArrayInputStream bais = new ByteArrayInputStream(data, 0, length);
             ObjectInputStream ois = new ObjectInputStream(bais)) {
            return (Metric) ois.readObject();
        }
    }
    
    /**
     * Stop the listener.
     */
    public void stop() {
        running = false;
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
        System.out.println("[UdpListener] Stopped");
    }
}
